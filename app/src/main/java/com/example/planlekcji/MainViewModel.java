package com.example.planlekcji;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.planlekcji.articles.ArticleDataDownloader;
import com.example.planlekcji.calendar.CalendarDataDownloader;
import com.example.planlekcji.ckziu_elektryk.client.CKZiUElektrykClient;
import com.example.planlekcji.ckziu_elektryk.client.article.Article;
import com.example.planlekcji.ckziu_elektryk.client.calendar.Calendar;
import com.example.planlekcji.ckziu_elektryk.client.replacements.Replacement;
import com.example.planlekcji.ckziu_elektryk.client.timetable.lesson.Lesson;
import com.example.planlekcji.listener.ArticlesDownloadCompleteListener;
import com.example.planlekcji.listener.CalendarDownloadCompleteListener;
import com.example.planlekcji.listener.ReplacementsDownloadCompleteListener;
import com.example.planlekcji.listener.TimetableDownloadCompleteListener;
import com.example.planlekcji.replacements.ReplacementDataDownloader;
import com.example.planlekcji.timetable.TimetableDataDownloader;
import com.example.planlekcji.timetable.model.DayOfWeek;
import com.example.planlekcji.utils.RetryHandler;
import com.example.planlekcji.utils.ToastUtils;

import java.util.List;
import java.util.Map;

public class MainViewModel extends ViewModel {
    private final CKZiUElektrykClient client;

    // Downloaded data
    private final MutableLiveData<List<List<Replacement>>> replacements = new MutableLiveData<>();
    private final MutableLiveData<Map<DayOfWeek, List<Lesson>>> timetable = new MutableLiveData<>();
    private final MutableLiveData<List<Article>> articles = new MutableLiveData<>();
    private final MutableLiveData<Calendar> calendar = new MutableLiveData<>();

    // ProgressBar state
    private final MutableLiveData<Boolean> isLoadingReplacements = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isLoadingTimetable = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isLoadingArticles = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isLoadingCalendar = new MutableLiveData<>(false);

    // Retry handlers
    private final RetryHandler replaceRetryHandler = new RetryHandler(this::startReplacementDownload);
    private final RetryHandler timetableRetryHandler = new RetryHandler(this::startTimetableDownload);
    private final RetryHandler articlesRetryHandler = new RetryHandler(this::startArticlesDownload);
    private final RetryHandler calendarRetryHandler = new RetryHandler(this::startCalendarDownload);

    public MainViewModel() {
        client = new CKZiUElektrykClient();
        client.setFailedApiConnectionCallback(e -> {
            Context context = MainActivity.getContext();
            if (context != null) {
                String errorMessage = context.getString(R.string.toastErrorMessage_failedApiConnection);
                ToastUtils.showToast(context, errorMessage, false);
            }
        });

        client.setFailedRouteRespondCallback(errorResponse -> {
            System.err.println("Error occurred: " + errorResponse.getMessage());
            Context context = MainActivity.getContext();
            if (context != null) {
                String errMess = context.getString(R.string.toast_errorMessage);
                ToastUtils.showToast(context, errMess, false);
            }
        });
    }

    public void fetchData() {
        startReplacementDownload();
        startTimetableDownload();
        startArticlesDownload();
        startCalendarDownload();
    }

    public void fetchReplacements() {
        startReplacementDownload();
    }

    public void fetchTimetable() {
        startTimetableDownload();
    }

    public void fetchArticles() {
        startArticlesDownload();
    }

    public void fetchCalendar() {
        startCalendarDownload();
    }

    private void startReplacementDownload() {
        isLoadingReplacements.postValue(true);
        ReplacementDataDownloader downloader = new ReplacementDataDownloader(client, new ReplacementsDownloadCompleteListener() {
            @Override
            public void onCacheLoaded(List<List<Replacement>> replacementList) {
                replacements.postValue(replacementList);
            }

            @Override
            public void onDownloadComplete(List<List<Replacement>> replacementList) {
                replacements.postValue(replacementList);
                isLoadingReplacements.postValue(false);
            }

            @Override
            public void onDownloadFailed() {
                isLoadingReplacements.postValue(false);
                replaceRetryHandler.handleRetry();
            }
        });
        new Thread(downloader).start();
    }

    private void startTimetableDownload() {
        isLoadingTimetable.postValue(true);
        TimetableDataDownloader downloader = new TimetableDataDownloader(client, new TimetableDownloadCompleteListener() {
            @Override
            public void onCacheLoaded(Map<DayOfWeek, List<Lesson>> timetableMap) {
                timetable.postValue(timetableMap);
            }

            @Override
            public void onDownloadComplete(Map<DayOfWeek, List<Lesson>> timetableMap) {
                timetable.postValue(timetableMap);
                isLoadingTimetable.postValue(false);
            }

            @Override
            public void onDownloadFailed() {
                isLoadingTimetable.postValue(false);
                timetableRetryHandler.handleRetry();
            }
        });
        new Thread(downloader).start();
    }

    private void startArticlesDownload() {
        isLoadingArticles.postValue(true);
        ArticleDataDownloader downloader = new ArticleDataDownloader(client, new ArticlesDownloadCompleteListener() {
            @Override
            public void onDownloadComplete(List<Article> articleList) {
                articles.postValue(articleList);
                isLoadingArticles.postValue(false);
            }

            @Override
            public void onDownloadFailed() {
                isLoadingArticles.postValue(false);
                articlesRetryHandler.handleRetry();
            }
        });
        new Thread(downloader).start();
    }

    private void startCalendarDownload() {
        isLoadingCalendar.postValue(true);
        CalendarDataDownloader downloader = new CalendarDataDownloader(client, new CalendarDownloadCompleteListener() {
            @Override
            public void onDownloadComplete(Calendar calendarData) {
                calendar.postValue(calendarData);
                isLoadingCalendar.postValue(false);
            }

            @Override
            public void onDownloadFailed() {
                isLoadingCalendar.postValue(false);
                calendarRetryHandler.handleRetry();
            }
        });
        new Thread(downloader).start();
    }

    // LiveData getters
    public LiveData<Map<DayOfWeek, List<Lesson>>> getTimetableLiveData() {
        return timetable;
    }

    public LiveData<List<List<Replacement>>> getReplacementsLiveData() {
        return replacements;
    }

    public LiveData<List<Article>> getArticlesLiveData() {
        return articles;
    }

    public LiveData<Calendar> getCalendarLiveData() {
        return calendar;
    }

    public LiveData<Boolean> getIsLoadingReplacements() {
        return isLoadingReplacements;
    }

    public LiveData<Boolean> getIsLoadingTimetable() {
        return isLoadingTimetable;
    }

    public LiveData<Boolean> getIsLoadingArticles() {
        return isLoadingArticles;
    }

    public LiveData<Boolean> getIsLoadingCalendar() {
        return isLoadingCalendar;
    }

    public CKZiUElektrykClient getClient() {
        return client;
    }

    private boolean settingsChanged = false;
    private boolean timetableNeedsRefresh = true;
    private boolean replacementsNeedsRefresh = true;
    private boolean articlesNeedsRefresh = true;
    private boolean calendarNeedsRefresh = true;

    public boolean isSettingsChanged() {
        return settingsChanged;
    }

    public void setSettingsChanged(boolean settingsChanged) {
        this.settingsChanged = settingsChanged;
    }

    public boolean isTimetableNeedsRefresh() {
        return timetableNeedsRefresh;
    }

    public void setTimetableNeedsRefresh(boolean timetableNeedsRefresh) {
        this.timetableNeedsRefresh = timetableNeedsRefresh;
    }

    public boolean isReplacementsNeedsRefresh() {
        return replacementsNeedsRefresh;
    }

    public void setReplacementsNeedsRefresh(boolean replacementsNeedsRefresh) {
        this.replacementsNeedsRefresh = replacementsNeedsRefresh;
    }

    public boolean isArticlesNeedsRefresh() {
        return articlesNeedsRefresh;
    }

    public void setArticlesNeedsRefresh(boolean articlesNeedsRefresh) {
        this.articlesNeedsRefresh = articlesNeedsRefresh;
    }

    public boolean isCalendarNeedsRefresh() {
        return calendarNeedsRefresh;
    }

    public void setCalendarNeedsRefresh(boolean calendarNeedsRefresh) {
        this.calendarNeedsRefresh = calendarNeedsRefresh;
    }

    public void markAllAsNeedsRefresh() {
        timetableNeedsRefresh = true;
        replacementsNeedsRefresh = true;
        articlesNeedsRefresh = true;
        calendarNeedsRefresh = true;
    }
}
