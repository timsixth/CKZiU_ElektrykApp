package com.example.planlekcji.articles;

import com.example.planlekcji.ckziu_elektryk.client.CKZiUElektrykClient;
import com.example.planlekcji.ckziu_elektryk.client.article.Article;
import com.example.planlekcji.ckziu_elektryk.client.article.ArticleService;
import com.example.planlekcji.ckziu_elektryk.client.pagination.Page;
import com.example.planlekcji.listener.ArticlesDownloadCompleteListener;

import java.util.Collections;

public class ArticleDataDownloader implements Runnable {
    private final CKZiUElektrykClient client;
    private final ArticlesDownloadCompleteListener listener;

    public ArticleDataDownloader(CKZiUElektrykClient client, ArticlesDownloadCompleteListener listener) {
        this.client = client;
        this.listener = listener;
    }

    @Override
    public void run() {
        try {
            ArticleService articleService = client.getArticleService();
            Page<Article> articlesPage = articleService.getArticles(1);

            if (articlesPage != null && articlesPage.data() != null) {
                listener.onDownloadComplete(articlesPage.data());
            } else {
                listener.onDownloadComplete(Collections.emptyList());
            }
        } catch (Exception e) {
            listener.onDownloadFailed();
        }
    }
}
