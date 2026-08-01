package com.example.planlekcji.fragments.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.planlekcji.MainViewModel;
import com.example.planlekcji.R;
import com.example.planlekcji.ckziu_elektryk.client.article.Article;
import com.example.planlekcji.ckziu_elektryk.client.article.PhotoSize;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

public class ArticlesFragment extends Fragment {
    private MainViewModel mainViewModel;
    private LinearLayout articlesContainer;
    private TextView textViewNoArticles;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_articles, container, false);

        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        articlesContainer = view.findViewById(R.id.linearLayout_articles);
        textViewNoArticles = view.findViewById(R.id.textView_noArticles);

        observeArticlesData();
        mainViewModel.fetchArticles();

        return view;
    }

    private void observeArticlesData() {
        mainViewModel.getArticlesLiveData().observe(getViewLifecycleOwner(), this::updateArticlesList);
    }

    private void updateArticlesList(List<Article> articles) {
        articlesContainer.removeAllViews();

        if (articles == null || articles.isEmpty()) {
            textViewNoArticles.setVisibility(View.VISIBLE);
            articlesContainer.addView(textViewNoArticles);
            return;
        }

        textViewNoArticles.setVisibility(View.GONE);
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        for (Article article : articles) {
            View cardView = inflater.inflate(R.layout.article_card, articlesContainer, false);

            ImageView imageViewHeader = cardView.findViewById(R.id.imageView_articleHeader);
            TextView textViewTitle = cardView.findViewById(R.id.textView_articleTitle);
            TextView textViewDate = cardView.findViewById(R.id.textView_articleDate);
            TextView textViewSnippet = cardView.findViewById(R.id.textView_articleSnippet);

            textViewTitle.setText(article.getTitle());

            if (article.getCreationDate() != null) {
                textViewDate.setText(dateFormat.format(article.getCreationDate()));
            } else {
                textViewDate.setVisibility(View.GONE);
            }

            if (article.getContent() != null) {
                String plainText = Html.fromHtml(article.getContent(), Html.FROM_HTML_MODE_LEGACY).toString().trim();
                textViewSnippet.setText(plainText);
            } else {
                textViewSnippet.setVisibility(View.GONE);
            }

            if (article.getHeaderImageUrl() != null) {
                imageViewHeader.setVisibility(View.VISIBLE);
                Glide.with(this)
                        .load(article.getHeaderImageUrl().toString())
                        .placeholder(R.drawable.round_corner)
                        .into(imageViewHeader);
            }

            cardView.setOnClickListener(v -> showArticleDetailsDialog(article));
            articlesContainer.addView(cardView);
        }
    }

    private void showArticleDetailsDialog(Article article) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_article_detail, (ViewGroup) requireView().getRootView(), false);

        ImageView imageViewHeader = dialogView.findViewById(R.id.imageView_detailHeader);
        TextView textViewTitle = dialogView.findViewById(R.id.textView_detailTitle);
        TextView textViewDate = dialogView.findViewById(R.id.textView_detailDate);
        TextView textViewContent = dialogView.findViewById(R.id.textView_detailContent);
        HorizontalScrollView galleryScrollView = dialogView.findViewById(R.id.scrollView_detailGallery);
        LinearLayout galleryLayout = dialogView.findViewById(R.id.layout_detailGallery);

        textViewTitle.setText(article.getTitle());

        if (article.getCreationDate() != null) {
            textViewDate.setText(dateFormat.format(article.getCreationDate()));
        } else {
            textViewDate.setVisibility(View.GONE);
        }

        if (article.getContent() != null) {
            textViewContent.setText(Html.fromHtml(article.getContent(), Html.FROM_HTML_MODE_LEGACY));
        }

        List<String> allPhotoUrls = new ArrayList<>();

        if (article.getHeaderImageUrl() != null) {
            String headerUrlStr = article.getHeaderImageUrl().toString();
            allPhotoUrls.add(headerUrlStr);

            imageViewHeader.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load(headerUrlStr)
                    .into(imageViewHeader);
            imageViewHeader.setOnClickListener(v -> showFullGalleryViewer(allPhotoUrls, 0));
        }

        // Fetch full article details asynchronously to load photo gallery
        new Thread(() -> {
            try {
                Optional<Article> fullArticleOpt = mainViewModel.getClient()
                        .getArticleService()
                        .getArticle(article.getId(), PhotoSize.SIZE_FULL);

                if (fullArticleOpt.isPresent() && isAdded()) {
                    Article fullArticle = fullArticleOpt.get();
                    List<URL> photos = fullArticle.getPhotosURLs();

                    List<String> galleryUrls = (photos != null) ? photos.stream()
                            .map(URL::toString)
                            .collect(Collectors.toList()) : new ArrayList<>();

                    List<String> combinedUrls = new ArrayList<>(allPhotoUrls);
                    for (String url : galleryUrls) {
                        if (!combinedUrls.contains(url)) {
                            combinedUrls.add(url);
                        }
                    }

                    requireActivity().runOnUiThread(() -> {
                        if (!isAdded()) return;

                        if (fullArticle.getContent() != null) {
                            textViewContent.setText(Html.fromHtml(fullArticle.getContent(), Html.FROM_HTML_MODE_LEGACY));
                        }

                        if (!combinedUrls.isEmpty()) {
                            populateGalleryThumbnails(galleryScrollView, galleryLayout, combinedUrls);
                            if (article.getHeaderImageUrl() != null) {
                                imageViewHeader.setOnClickListener(v -> showFullGalleryViewer(combinedUrls, 0));
                            }
                        }
                    });
                }
            } catch (Exception ignored) {}
        }).start();

        dialog.setContentView(dialogView);
        dialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        dialog.show();
    }

    private void populateGalleryThumbnails(HorizontalScrollView scrollView, LinearLayout galleryLayout, List<String> photoUrls) {
        if (!isAdded() || getContext() == null || photoUrls.isEmpty()) return;

        galleryLayout.removeAllViews();
        scrollView.setVisibility(View.VISIBLE);

        float density = requireContext().getResources().getDisplayMetrics().density;
        int sizePx = (int) (100 * density);
        int marginPx = (int) (6 * density);
        int radiusPx = (int) (8 * density);

        for (int i = 0; i < photoUrls.size(); i++) {
            final int index = i;
            String urlStr = photoUrls.get(i);

            MaterialCardView card = new MaterialCardView(requireContext());
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(sizePx, sizePx);
            cardParams.setMargins(marginPx, marginPx, marginPx, marginPx);
            card.setLayoutParams(cardParams);
            card.setRadius(radiusPx);
            card.setCardElevation(2 * density);
            card.setStrokeWidth(0);

            ImageView thumbnail = new ImageView(requireContext());
            thumbnail.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
            thumbnail.setContentDescription(getString(R.string.article_image_description));

            Glide.with(this)
                    .load(urlStr)
                    .placeholder(R.drawable.round_corner)
                    .into(thumbnail);

            card.addView(thumbnail);
            card.setOnClickListener(v -> showFullGalleryViewer(photoUrls, index));

            galleryLayout.addView(card);
        }
    }

    private void showFullGalleryViewer(List<String> photoUrls, int initialPosition) {
        if (photoUrls == null || photoUrls.isEmpty() || !isAdded()) return;

        Dialog dialog = new Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_full_gallery_viewer, (ViewGroup) requireView().getRootView(), false);

        ViewPager2 viewPager = view.findViewById(R.id.viewPager_fullGallery);
        TextView textCounter = view.findViewById(R.id.textView_photoCounter);
        ImageButton btnPrev = view.findViewById(R.id.button_prevPhoto);
        ImageButton btnNext = view.findViewById(R.id.button_nextPhoto);
        ImageButton btnClose = view.findViewById(R.id.button_closeGallery);

        viewPager.setAdapter(new GalleryPagerAdapter(photoUrls));
        viewPager.setCurrentItem(initialPosition, false);

        Runnable updateUI = () -> {
            int current = viewPager.getCurrentItem();
            textCounter.setText(getString(R.string.photo_counter_format, current + 1, photoUrls.size()));
            btnPrev.setVisibility(current > 0 ? View.VISIBLE : View.INVISIBLE);
            btnNext.setVisibility(current < photoUrls.size() - 1 ? View.VISIBLE : View.INVISIBLE);
        };

        updateUI.run();

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateUI.run();
            }
        });

        btnPrev.setOnClickListener(v -> {
            int curr = viewPager.getCurrentItem();
            if (curr > 0) viewPager.setCurrentItem(curr - 1, true);
        });

        btnNext.setOnClickListener(v -> {
            int curr = viewPager.getCurrentItem();
            if (curr < photoUrls.size() - 1) viewPager.setCurrentItem(curr + 1, true);
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.setContentView(view);
        dialog.show();
    }

    private class GalleryPagerAdapter extends RecyclerView.Adapter<GalleryPagerAdapter.PhotoViewHolder> {
        private final List<String> urls;

        GalleryPagerAdapter(List<String> urls) {
            this.urls = urls;
        }

        @NonNull
        @Override
        public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_full_photo, parent, false);
            return new PhotoViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
            Glide.with(ArticlesFragment.this)
                    .load(urls.get(position))
                    .into(holder.imageView);
        }

        @Override
        public int getItemCount() {
            return urls.size();
        }

        static class PhotoViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;

            PhotoViewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.imageView_fullPhoto);
            }
        }
    }
}
