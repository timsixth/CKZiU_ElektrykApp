package com.example.planlekcji.timetable.ui;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import android.graphics.Typeface;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.card.MaterialCardView;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.planlekcji.MainActivity;
import com.example.planlekcji.R;
import com.example.planlekcji.utils.EmptyStateHelper;
import com.example.planlekcji.utils.EmptyStateType;
import com.example.planlekcji.ckziu_elektryk.client.timetable.SchoolEntryType;
import com.example.planlekcji.ckziu_elektryk.client.timetable.lesson.GroupLesson;
import com.example.planlekcji.ckziu_elektryk.client.timetable.lesson.Lesson;
import com.example.planlekcji.ckziu_elektryk.client.timetable.lesson.LessonDetails;
import com.example.planlekcji.ckziu_elektryk.client.timetable.lesson.SchoolClass;
import com.example.planlekcji.ckziu_elektryk.client.timetable.lesson.SingleLesson;
import com.example.planlekcji.timetable.model.DayOfWeek;

import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LessonFragment extends Fragment {
    public static final String TITLE = "title";
    private Map<DayOfWeek, List<Lesson>> timetableMap;

    public LessonFragment() {
    }

    public LessonFragment(Map<DayOfWeek, List<Lesson>> timetableMap) {
        this.timetableMap = timetableMap;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Context context = requireContext();
        LinearLayout layout = view.findViewById(R.id.linearLayoutCards);
        LayoutInflater inflater = LayoutInflater.from(context);

        assert getArguments() != null;
        String argument = getArguments().getString(TITLE);
        assert argument != null;
        int tabNumber = Character.getNumericValue(argument.charAt(3));

        DayOfWeek thisDayNumber = DayOfWeek.getDayOfWeek(tabNumber);

        if (timetableMap == null || timetableMap.get(DayOfWeek.MONDAY) == null) {
            return;
        }

        List<Lesson> lessonList = timetableMap.get(thisDayNumber);
        int currentLessonIndex = getCurrentLessonIndex(tabNumber, lessonList);

        Map<Integer, List<LessonDetails>> lessonDataMap = new HashMap<>();
        Map<Integer, String> lessonTimeMap = new HashMap<>();

        if (lessonList == null) {
            Log.e("TimetableLessonFragment", "lessonList is empty");
            return;
        }

        for (Lesson lesson : lessonList) {
            for (int number : lesson.getLessonsNumbers()) {
                if (lesson.getDuration() != null && lesson.getDuration().startTime() != null && lesson.getDuration().endTime() != null) {
                    String timeRange = lesson.getDuration().startTime().toString() + " - " + lesson.getDuration().endTime().toString();
                    lessonTimeMap.put(number, timeRange);
                }

                if (lesson instanceof SingleLesson) {
                    LessonDetails lessonDetails = ((SingleLesson) lesson).getDetails();
                    lessonDataMap.put(number, Collections.singletonList(lessonDetails));
                } else if (lesson instanceof GroupLesson) {
                    List<LessonDetails> lessonDetailsObject = ((GroupLesson) lesson).getLessonsDetails();
                    lessonDataMap.put(number, lessonDetailsObject);
                }
            }
        }

        // No lessons scheduled for this day
        if (lessonDataMap.isEmpty()) {
            layout.addView(EmptyStateHelper.create(inflater, layout, EmptyStateType.DAY_OFF));
            return;
        }

        int minKey = Collections.min(lessonDataMap.keySet());
        int maxKey = Collections.max(lessonDataMap.keySet());

        // Add empty data entries for gaps between lessons
        for (int i = minKey; i <= maxKey; i++) {
            if (!lessonDataMap.containsKey(i)) {
                lessonDataMap.put(i, Collections.emptyList());
            }
        }

        for (Integer i : lessonDataMap.keySet()) {
            MaterialCardView cardView = (MaterialCardView) inflater.inflate(R.layout.lesson_card, layout, false);

            TextView lessonHoursText = cardView.findViewById(R.id.textViewLessonHours);
            TextView lessonNumber = cardView.findViewById(R.id.textViewLessonNumber);
            View divider = cardView.findViewById(R.id.lessonCard_divider);
            LinearLayout lessonsContainer = cardView.findViewById(R.id.layout_lessonsContainer);
            TextView viewLessonData = cardView.findViewById(R.id.textViewLessonData);

            String timeRangeString = lessonTimeMap.getOrDefault(i, "");

            lessonHoursText.setText(timeRangeString);
            lessonNumber.setText(String.valueOf(i));

            List<LessonDetails> detailsList = lessonDataMap.get(i);
            boolean isCurrentLesson = (currentLessonIndex == i);

            if (detailsList == null || detailsList.isEmpty()) {
                viewLessonData.setVisibility(View.VISIBLE);
                viewLessonData.setText("");
            } else {
                viewLessonData.setVisibility(View.GONE);

                for (int idx = 0; idx < detailsList.size(); idx++) {
                    LessonDetails details = detailsList.get(idx);

                    // Add subtle divider between multiple lessons in the same time slot
                    if (idx > 0) {
                        View itemDivider = new View(context);
                        LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                (int) (1 * getResources().getDisplayMetrics().density)
                        );
                        int marginV = (int) (3 * getResources().getDisplayMetrics().density);
                        divParams.setMargins(0, marginV, 0, marginV);
                        itemDivider.setLayoutParams(divParams);
                        itemDivider.setBackgroundColor(isCurrentLesson ? Color.parseColor("#44FFC107") : Color.parseColor("#1FFFFFFF"));
                        lessonsContainer.addView(itemDivider);
                    }

                    View rowView = inflater.inflate(R.layout.item_lesson_entry, lessonsContainer, false);
                    TextView textSubject = rowView.findViewById(R.id.textView_lessonSubject);
                    TextView textMeta = rowView.findViewById(R.id.textView_lessonMeta);

                    textSubject.setText(getSubjectText(details));
                    textMeta.setText(getMetaText(details));

                    if (isCurrentLesson) {
                        textSubject.setTextColor(Color.WHITE);
                        textSubject.setTypeface(null, Typeface.BOLD);
                        textMeta.setTextColor(Color.parseColor("#FFD54F"));
                    }

                    lessonsContainer.addView(rowView);
                }
            }

            // Highlight current lesson
            if (isCurrentLesson) {
                int goldColor = ContextCompat.getColor(requireContext(), R.color.primaryDark);
                int darkGoldBg = Color.parseColor("#2E250A");
                int strokeColor = Color.parseColor("#99FFC107");
                int strokeWidthPx = (int) (1.5f * getResources().getDisplayMetrics().density);

                cardView.setCardBackgroundColor(darkGoldBg);
                cardView.setStrokeColor(strokeColor);
                cardView.setStrokeWidth(strokeWidthPx);

                lessonNumber.setTextColor(goldColor);
                lessonNumber.setTypeface(null, Typeface.BOLD);

                lessonHoursText.setTextColor(Color.parseColor("#FFD54F"));
                lessonHoursText.setAlpha(1.0f);

                if (divider != null) {
                    divider.setBackgroundColor(strokeColor);
                }
            }

            layout.addView(cardView);
        }
    }

    private String getSubjectText(LessonDetails lessonDetails) {
        return lessonDetails.getSubject().name();
    }

    private String getMetaText(LessonDetails lessonDetails) {
        SchoolEntryType timetableType = MainActivity.getTimetableType();
        String classroomName = lessonDetails.getClassroom();
        String teacherName = lessonDetails.getTeacher();
        String schoolClassName = "";
        if (lessonDetails.getSchoolClasses() != null && !lessonDetails.getSchoolClasses().isEmpty()) {
            schoolClassName = lessonDetails.getSchoolClasses().stream()
                    .map(SchoolClass::shortcut)
                    .collect(Collectors.joining(", "));
        } else if (lessonDetails.getSchoolClass().isPresent()) {
            schoolClassName = lessonDetails.getSchoolClass().get().shortcut();
        }

        if (timetableType == SchoolEntryType.CLASSES) {
            StringBuilder sb = new StringBuilder();
            if (!classroomName.isEmpty()) sb.append(classroomName);
            if (!teacherName.isEmpty()) {
                if (sb.length() > 0) sb.append(" · ");
                sb.append(teacherName);
            }
            return sb.toString();
        } else if (timetableType == SchoolEntryType.TEACHERS) {
            StringBuilder sb = new StringBuilder();
            if (!schoolClassName.isEmpty()) sb.append(schoolClassName);
            if (!classroomName.isEmpty()) {
                if (sb.length() > 0) sb.append(" · ");
                sb.append(classroomName);
            }
            return sb.toString();
        } else {
            StringBuilder sb = new StringBuilder();
            if (!schoolClassName.isEmpty()) sb.append(schoolClassName);
            if (!teacherName.isEmpty()) {
                if (sb.length() > 0) sb.append(" · ");
                sb.append(teacherName);
            }
            return sb.toString();
        }
    }

    /**
     * Determines the index of the current active lesson based on current time and API lesson durations.
     *
     * @return The index of the current lesson or 0 if there is no active lesson.
     */
    private int getCurrentLessonIndex(int tabNumber, List<Lesson> lessonList) {
        Calendar calendar = Calendar.getInstance();
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1; // Monday == 1, Tuesday == 2, etc

        if (dayOfWeek != tabNumber || lessonList == null || lessonList.isEmpty()) return 0;

        int currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);

        for (Lesson lesson : lessonList) {
            if (lesson.getDuration() == null || lesson.getDuration().startTime() == null || lesson.getDuration().endTime() == null) {
                continue;
            }

            int startMinutes = lesson.getDuration().startTime().getHours() * 60 + lesson.getDuration().startTime().getMinutes();
            int endMinutes = lesson.getDuration().endTime().getHours() * 60 + lesson.getDuration().endTime().getMinutes();

            if (currentMinutes >= startMinutes && currentMinutes <= endMinutes) {
                if (lesson.getLessonsNumbers() != null && !lesson.getLessonsNumbers().isEmpty()) {
                    return lesson.getLessonsNumbers().get(0);
                }
            }
        }

        return 0;
    }

}