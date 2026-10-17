package com.example.planlekcji.fragments.ui;

import android.os.Bundle;
import android.text.Html;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.planlekcji.MainActivity;
import com.example.planlekcji.MainViewModel;
import com.example.planlekcji.R;
import com.example.planlekcji.utils.EmptyStateHelper;
import com.example.planlekcji.utils.EmptyStateType;
import com.example.planlekcji.ckziu_elektryk.client.replacements.Replacement;
import com.example.planlekcji.ckziu_elektryk.client.replacements.ReplacementChange;
import com.example.planlekcji.ckziu_elektryk.client.timetable.SchoolEntryType;
import com.example.planlekcji.replacements.DayReplacements;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReplacementsFragment extends Fragment {
    private List<DayReplacements> replacements;
    private MainViewModel mainViewModel;
    private LayoutInflater inflater;
    private LinearLayout layout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_replacements, container, false);
        this.mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        this.inflater = inflater;
        this.layout = view.findViewById(R.id.linearLayout_replacements);

        observeAndHandleReplacementsLiveData();
        layout.addView(EmptyStateHelper.create(inflater, layout, EmptyStateType.REPLACEMENTS));

        return view;
    }

    private void observeAndHandleReplacementsLiveData() {
        mainViewModel.getReplacementsLiveData().observe(getViewLifecycleOwner(), newReplacements -> {
            replacements = newReplacements;

            updateReplacements();
        });
    }

    private void updateReplacements() {
        layout.removeAllViews();

        if (replacements == null || replacements.isEmpty() || areReplacementsEmpty()) {
            layout.addView(EmptyStateHelper.create(inflater, layout, EmptyStateType.REPLACEMENTS));

            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault());

        for (DayReplacements day : replacements) {
            if (day == null) continue;
            List<Replacement> dayReplacements = day.replacements();
            if (dayReplacements == null || dayReplacements.isEmpty()) continue;

            CardView dayCard = (CardView) inflater.inflate(R.layout.replacement_day_card, layout, false);
            LinearLayout dayCardLayout = dayCard.findViewById(R.id.replacementDay_layout);

            TextView dayTitle = dayCard.findViewById(R.id.textView_dayTitle);
            String formattedDate = day.date() != null ? sdf.format(day.date()) : "";
            if (!formattedDate.isEmpty()) {
                formattedDate = Character.toUpperCase(formattedDate.charAt(0)) + formattedDate.substring(1);
            }
            dayTitle.setText(formattedDate);

            boolean isClassType = (MainActivity.getTimetableType() == SchoolEntryType.CLASSES);

            Map<String, List<CharSequence>> groupedDetails = new LinkedHashMap<>();

            for (Replacement replacement : dayReplacements) {
                if (replacement.changes() != null && !replacement.changes().isEmpty()) {
                    for (ReplacementChange change : replacement.changes()) {
                        String period = change.period();
                        String lessonText;
                        if (period == null) {
                            lessonText = "";
                        } else if (isAllDay(period)) {
                            lessonText = period;
                        } else {
                            lessonText = getString(R.string.lesson_label, period);
                        }

                        String title;
                        CharSequence detailText;

                        if (isClassType) {
                            String info = change.info();
                            String subjectName = info;
                            String statusOrTeacher = "";

                            if (info != null && info.contains(" - ")) {
                                String[] parts = info.split(" - ", 2);
                                subjectName = parts[0].trim();
                                statusOrTeacher = parts[1].trim();
                            }

                            subjectName = capitalizeSubject(subjectName);
                            title = subjectName;
                            detailText = formatItemDetailsHtml(lessonText, statusOrTeacher);
                        } else {
                            title = replacement.name();
                            detailText = formatItemDetailsHtml(lessonText, formatReplacementInfo(change.info()));
                        }

                        if (title == null) title = "";

                        groupedDetails.computeIfAbsent(title, k -> new ArrayList<>()).add(detailText);
                    }
                }
            }

            for (Map.Entry<String, List<CharSequence>> entry : groupedDetails.entrySet()) {
                String title = entry.getKey();
                List<CharSequence> detailsList = entry.getValue();

                CardView replacementCard = (CardView) inflater.inflate(R.layout.replacement_card, dayCardLayout, false);
                TextView replacementTitle = replacementCard.findViewById(R.id.textView_replacementTitle);
                LinearLayout itemsLayout = replacementCard.findViewById(R.id.layout_replacementItems);

                replacementTitle.setText(title);

                for (int i = 0; i < detailsList.size(); i++) {
                    View row = inflater.inflate(R.layout.replacement_item_row, itemsLayout, false);
                    View divider = row.findViewById(R.id.view_itemDivider);
                    TextView itemText = row.findViewById(R.id.textView_itemDetails);

                    if (i == 0) {
                        divider.setVisibility(View.GONE);
                    } else {
                        divider.setVisibility(View.VISIBLE);
                    }

                    itemText.setText(detailsList.get(i));
                    itemsLayout.addView(row);
                }

                dayCardLayout.addView(replacementCard);
            }

            layout.addView(dayCard);
        }
    }

    private CharSequence formatItemDetailsHtml(String lesson, String info) {
        String lessonStr = (lesson != null) ? lesson : "";
        String infoStr = (info != null) ? info : "";

        StringBuilder sb = new StringBuilder();
        if (!lessonStr.isEmpty()) {
            sb.append("<b>").append(lessonStr).append("</b>");
        }

        if (!infoStr.isEmpty()) {
            if (!lessonStr.isEmpty()) {
                sb.append(" — ");
            }
            sb.append(infoStr);
        }

        return Html.fromHtml(sb.toString(), Html.FROM_HTML_MODE_LEGACY);
    }

    private boolean areReplacementsEmpty() {
        for (DayReplacements day : replacements) {
            if (day != null && day.replacements() != null && !day.replacements().isEmpty()) {
                return false;
            }
        }

        return true;
    }

    private boolean isAllDay(String period) {
        if (period == null) return false;
        String normalized = period.trim().toLowerCase(Locale.ROOT);
        return normalized.contains("cały dzień") || normalized.contains("caly dzien");
    }

    private String formatReplacementInfo(String info) {
        if (info == null) {
            return "";
        }
        if (info.contains(" - ")) {
            String[] parts = info.split(" - ", 2);
            return capitalizeSubject(parts[0].trim()) + " - " + parts[1].trim();
        }
        return capitalizeSubject(info);
    }

    private String capitalizeSubject(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        char[] chars = text.toCharArray();
        boolean capitalizeNext = true;

        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (c == ':') {
                capitalizeNext = true;
            } else if (capitalizeNext && Character.isLetter(c)) {
                chars[i] = Character.toUpperCase(c);
                capitalizeNext = false;
            }
        }

        return new String(chars);
    }
}