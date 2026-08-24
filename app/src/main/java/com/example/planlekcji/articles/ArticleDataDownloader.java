package com.example.planlekcji.articles;

import android.content.Context;
import android.util.Log;

import com.example.planlekcji.MainActivity;
import com.example.planlekcji.ckziu_elektryk.client.CKZiUElektrykClient;
import com.example.planlekcji.ckziu_elektryk.client.article.Article;
import com.example.planlekcji.ckziu_elektryk.client.article.ArticleService;
import com.example.planlekcji.ckziu_elektryk.client.pagination.Page;
import com.example.planlekcji.database.DatabaseCacheManager;
import com.example.planlekcji.listener.ArticlesDownloadCompleteListener;
import com.example.planlekcji.utils.RefreshCooldownManager;
import com.example.planlekcji.utils.RefreshDataType;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

public class ArticleDataDownloader implements Runnable {
    public static final String CACHE_KEY = "articles_latest";
    private final CKZiUElektrykClient client;
    private final ArticlesDownloadCompleteListener listener;

    public ArticleDataDownloader(CKZiUElektrykClient client, ArticlesDownloadCompleteListener listener) {
        this.client = client;
        this.listener = listener;
    }

    @Override
    public void run() {
        Context context = MainActivity.getContext();
        Type listType = new TypeToken<List<Article>>(){}.getType();
        List<Article> cached = null;

        // Cache-First: immediately emit cached articles if available
        if (context != null) {
            try {
                cached = DatabaseCacheManager.getInstance(context).getObject(CACHE_KEY, listType);
                if (cached != null && !cached.isEmpty()) {
                    listener.onCacheLoaded(cached);
                }
            } catch (Exception e) {
                Log.e("ArticleDownloader", "Failed to load cached articles", e);
            }
        }

        // Complete immediately if cache is fresh within TTL
        RefreshCooldownManager cooldown = (context != null) ? RefreshCooldownManager.getInstance(context) : null;
        if (cooldown != null && cooldown.isFresh(RefreshDataType.ARTICLES, cached)) {
            listener.onDownloadComplete(cached);
            return;
        }

        try {
            ArticleService articleService = client.getArticleService();
            Page<Article> articlesPage = articleService.getArticles(1);

            if (articlesPage != null && articlesPage.data() != null) {
                List<Article> articles = articlesPage.data();
                if (context != null && !articles.isEmpty()) {
                    DatabaseCacheManager.getInstance(context).saveObject(CACHE_KEY, articles);
                    if (cooldown != null) {
                        cooldown.recordRefresh(RefreshDataType.ARTICLES);
                    }
                }
                listener.onDownloadComplete(articles);
            } else {
                listener.onDownloadFailed();
            }
        } catch (Exception e) {
            listener.onDownloadFailed();
        }
    }
}
