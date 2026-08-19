package com.example.planlekcji;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.example.planlekcji.ckziu_elektryk.client.timetable.SchoolEntryType;
import com.example.planlekcji.fragments.model.ViewPagerAdapter;
import com.example.planlekcji.utils.NetworkMonitor;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class MainActivity extends AppCompatActivity {
    private static Context appContext;
    private MainViewModel mainViewModel;
    private NetworkMonitor networkMonitor;

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize the application context for other functions.
        appContext = getApplicationContext();

        // Obtain the MainViewModel instance to update data on settings changes
        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);

        // Lock the orientation of the screen to portrait mode.
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Force night mode for the entire application.
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);

        // Set the content view for the main activity.
        setContentView(R.layout.activity_main);

        // Network monitoring and offline banner
        networkMonitor = new NetworkMonitor(this);
        View layoutOfflineBanner = findViewById(R.id.layout_offlineBanner);

        networkMonitor.getIsOnlineLiveData().observe(this, isOnline -> {
            if (layoutOfflineBanner != null) {
                layoutOfflineBanner.setVisibility(Boolean.TRUE.equals(isOnline) ? View.GONE : View.VISIBLE);
            }
            if (Boolean.TRUE.equals(isOnline)) {
                mainViewModel.fetchData();
            }
        });

        // Progress bar
        ProgressBar progressBar = findViewById(R.id.progressBar);

        androidx.lifecycle.Observer<Boolean> loadingObserver = unused -> {
            boolean isLoading = Boolean.TRUE.equals(mainViewModel.getIsLoadingReplacements().getValue())
                    || Boolean.TRUE.equals(mainViewModel.getIsLoadingTimetable().getValue())
                    || Boolean.TRUE.equals(mainViewModel.getIsLoadingArticles().getValue())
                    || Boolean.TRUE.equals(mainViewModel.getIsLoadingCalendar().getValue());
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        };

        mainViewModel.getIsLoadingReplacements().observe(this, loadingObserver);
        mainViewModel.getIsLoadingTimetable().observe(this, loadingObserver);
        mainViewModel.getIsLoadingArticles().observe(this, loadingObserver);
        mainViewModel.getIsLoadingCalendar().observe(this, loadingObserver);

        // Set adapter
        ViewPager2 viewPager2_appContent = findViewById(R.id.viewPager2_appContent);

        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        viewPager2_appContent.setAdapter(adapter);
        viewPager2_appContent.setOffscreenPageLimit(4);
        viewPager2_appContent.setUserInputEnabled(true);

        // Connect the TabLayout (navigation) with the ViewPager2 (app content)
        TabLayout tabLayout_navigate = findViewById(R.id.tabLayout_navigate);
        new TabLayoutMediator(tabLayout_navigate, viewPager2_appContent, (tab, position) -> {
            switch (position) {
                case ViewPagerAdapter.TIMETABLE_TAB_ID:
                    tab.setText(R.string.navigate_timetable);
                    tab.setIcon(R.drawable.timetable_icon);
                    break;
                case ViewPagerAdapter.REPLACEMENTS_TAB_ID:
                    tab.setText(R.string.navigate_replacements);
                    tab.setIcon(R.drawable.replacement_icon);
                    break;
                case ViewPagerAdapter.ARTICLES_TAB_ID:
                    tab.setText(R.string.navigate_articles);
                    tab.setIcon(R.drawable.articles_icon);
                    break;
                case ViewPagerAdapter.CALENDAR_TAB_ID:
                    tab.setText(R.string.navigate_calendar);
                    tab.setIcon(R.drawable.calendar_icon);
                    break;
                case ViewPagerAdapter.SETTINGS_TAB_ID:
                    tab.setText(R.string.navigate_settings);
                    tab.setIcon(R.drawable.settings_icon);
                    break;
            }
        }).attach();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (networkMonitor != null) {
            networkMonitor.unregisterCallback();
        }
    }

    public static Context getContext() {
        return appContext;
    }

    public NetworkMonitor getNetworkMonitor() {
        return networkMonitor;
    }

    public boolean isOnline() {
        if (networkMonitor != null) {
            return networkMonitor.isCurrentlyOnline();
        }
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        Network network = cm.getActiveNetwork();
        if (network == null) return false;

        NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
        return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    public static SchoolEntryType getTimetableType() {
        Context context = MainActivity.getContext();
        SharedPreferences sharedPreferences = context.getSharedPreferences("sharedPrefs", 0);

        // 0 - classes, 1 - teachers, 2 - classrooms
        int typeOfTimetable = sharedPreferences.getInt("selectedTypeOfTimetable", 0);

        return SchoolEntryType.values()[typeOfTimetable];
    }

    public static String getToken(SchoolEntryType timetableType) {
        Context context = MainActivity.getContext();
        SharedPreferences sharedPreferences = context.getSharedPreferences("sharedPrefs", 0);

        String tokenType;
        if (timetableType == SchoolEntryType.CLASSES) {
            tokenType = context.getString(R.string.classTokenKey);
        } else if(timetableType == SchoolEntryType.TEACHERS) {
            tokenType = context.getString(R.string.teacherTokenKey);
        } else {
            tokenType = context.getString(R.string.classroomTokenKey);
        }

        return sharedPreferences.getString(tokenType, "");
    }

}