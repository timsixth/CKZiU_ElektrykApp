package com.example.planlekcji.listener;

import com.example.planlekcji.ckziu_elektryk.client.article.Article;

import java.util.List;

public interface ArticlesDownloadCompleteListener {
    void onDownloadComplete(List<Article> articles);
    void onDownloadFailed();
}
