package com.example.planlekcji.listener;

import com.example.planlekcji.ckziu_elektryk.client.replacements.Replacement;

import java.util.List;

public interface ReplacementsDownloadCompleteListener {

    default void onCacheLoaded(List<List<Replacement>> rawReplacements) {}

    void onDownloadComplete(List<List<Replacement>> rawReplacements);

    void onDownloadFailed();
}
