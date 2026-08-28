package com.example.planlekcji.fragments.ui;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.planlekcji.MainViewModel;
import com.example.planlekcji.R;
import com.example.planlekcji.utils.EmptyStateHelper;
import com.example.planlekcji.utils.EmptyStateType;
import com.example.planlekcji.ckziu_elektryk.client.calendar.Calendar;
import com.example.planlekcji.ckziu_elektryk.client.calendar.CalendarEvent;
import com.example.planlekcji.ckziu_elektryk.client.calendar.CalendarSection;

import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CalendarFragment extends Fragment {

    private MainViewModel mainViewModel;
    private LinearLayout calendarContainer;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);

        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        calendarContainer = view.findViewById(R.id.linearLayout_calendar);

        calendarContainer.addView(EmptyStateHelper.create(LayoutInflater.from(requireContext()), calendarContainer, EmptyStateType.CALENDAR));

        observeCalendarData();

        return view;
    }

    private void observeCalendarData() {
        mainViewModel.getCalendarLiveData().observe(getViewLifecycleOwner(), this::updateCalendarView);
    }

    private void updateCalendarView(Calendar calendar) {
        if (!isAdded()) return;

        calendarContainer.removeAllViews();

        if (calendar == null || calendar.sections() == null || calendar.sections().isEmpty()) {
            calendarContainer.addView(EmptyStateHelper.create(LayoutInflater.from(requireContext()), calendarContainer, EmptyStateType.CALENDAR));
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(requireContext());

        List<CalendarSection> sections = calendar.sections();
        for (CalendarSection section : sections) {
            View sectionCard = inflater.inflate(R.layout.calendar_section_card, calendarContainer, false);
            TextView textViewSectionName = sectionCard.findViewById(R.id.textView_sectionName);
            TextView textViewSectionExtraInfo = sectionCard.findViewById(R.id.textView_sectionExtraInfo);
            LinearLayout layoutSectionEvents = sectionCard.findViewById(R.id.layout_sectionEvents);

            textViewSectionName.setText(section.name());

            List<CalendarEvent> events = section.events();
            StringBuilder extraInfoBuilder = new StringBuilder();
            List<String> addedExtraInfos = new java.util.ArrayList<>();

            if (events != null && !events.isEmpty()) {
                for (CalendarEvent event : events) {
                    View eventItem = inflater.inflate(R.layout.calendar_event_item, layoutSectionEvents, false);
                    TextView textViewDate = eventItem.findViewById(R.id.textView_eventDate);
                    TextView textViewDescription = eventItem.findViewById(R.id.textView_eventDescription);

                    textViewDate.setText(event.dateRaw());
                    textViewDescription.setText(event.description());

                    boolean isPast = isPastEvent(event.dateRaw());
                    if (isPast) {
                        textViewDate.setTextColor(Color.parseColor("#777777"));
                        textViewDate.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#222222")));
                        textViewDescription.setTextColor(Color.parseColor("#666666"));
                        eventItem.setAlpha(0.55f);
                    } else {
                        textViewDate.setTextColor(Color.parseColor("#FFD54F"));
                        textViewDate.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2C2C2E")));
                        textViewDescription.setTextColor(Color.parseColor("#E0E0E0"));
                        eventItem.setAlpha(1.0f);
                    }

                    layoutSectionEvents.addView(eventItem);

                    if (event.extraInfo() != null && !event.extraInfo().trim().isEmpty()) {
                        String extra = event.extraInfo().trim();
                        if (!addedExtraInfos.contains(extra)) {
                            addedExtraInfos.add(extra);
                            if (extraInfoBuilder.length() > 0) {
                                extraInfoBuilder.append("\n");
                            }
                            extraInfoBuilder.append(extra);
                        }
                    }
                }
            }

            if (extraInfoBuilder.length() > 0) {
                textViewSectionExtraInfo.setText(extraInfoBuilder.toString());
                textViewSectionExtraInfo.setVisibility(View.VISIBLE);
            } else {
                textViewSectionExtraInfo.setVisibility(View.GONE);
            }

            calendarContainer.addView(sectionCard);
        }
    }

    private boolean isPastEvent(String dateRaw) {
        if (dateRaw == null || dateRaw.trim().isEmpty()) return false;

        Date referenceDate = new Date();

        String text = dateRaw.trim();
        String datePart = text;
        if (text.contains("-")) {
            String[] parts = text.split("-");
            datePart = parts[parts.length - 1].trim();
        }

        // Try parsing full date (dd.MM.yyyy) first
        Matcher fullDateMatcher = Pattern.compile("(\\d{1,2})\\.(\\d{1,2})\\.(\\d{4})").matcher(datePart);
        if (fullDateMatcher.find()) {
            String g1 = fullDateMatcher.group(1);
            String g2 = fullDateMatcher.group(2);
            String g3 = fullDateMatcher.group(3);
            if (g1 != null && g2 != null && g3 != null) {
                try {
                    int day = Integer.parseInt(g1);
                    int month = Integer.parseInt(g2) - 1;
                    int year = Integer.parseInt(g3);

                    java.util.Calendar eventCal = java.util.Calendar.getInstance();
                    eventCal.set(java.util.Calendar.YEAR, year);
                    eventCal.set(java.util.Calendar.MONTH, month);
                    eventCal.set(java.util.Calendar.DAY_OF_MONTH, day);
                    eventCal.set(java.util.Calendar.HOUR_OF_DAY, 23);
                    eventCal.set(java.util.Calendar.MINUTE, 59);

                    return eventCal.getTime().before(referenceDate);
                } catch (Exception ignored) {}
            }
        }

        // Fallback for dd.MM with year in text or system default
        Matcher dayMonthMatcher = Pattern.compile("(\\d{1,2})\\.(\\d{1,2})").matcher(datePart);
        if (dayMonthMatcher.find()) {
            String g1 = dayMonthMatcher.group(1);
            String g2 = dayMonthMatcher.group(2);
            if (g1 != null && g2 != null) {
                try {
                    int day = Integer.parseInt(g1);
                    int month = Integer.parseInt(g2) - 1;

                    int year = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
                    Matcher yearMatcher = Pattern.compile("20\\d{2}").matcher(text);
                    if (yearMatcher.find()) {
                        String yGroup = yearMatcher.group();
                        year = Integer.parseInt(yGroup);
                    }

                    java.util.Calendar eventCal = java.util.Calendar.getInstance();
                    eventCal.set(java.util.Calendar.YEAR, year);
                    eventCal.set(java.util.Calendar.MONTH, month);
                    eventCal.set(java.util.Calendar.DAY_OF_MONTH, day);
                    eventCal.set(java.util.Calendar.HOUR_OF_DAY, 23);
                    eventCal.set(java.util.Calendar.MINUTE, 59);

                    return eventCal.getTime().before(referenceDate);
                } catch (Exception ignored) {}
            }
        }

        return false;
    }
}
