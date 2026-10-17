package com.example.planlekcji.timetable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.planlekcji.ckziu_elektryk.client.replacements.Replacement;
import com.example.planlekcji.ckziu_elektryk.client.replacements.ReplacementChange;
import com.example.planlekcji.replacements.DayReplacements;
import com.example.planlekcji.timetable.utils.TimetableReplacementMatcher;
import com.example.planlekcji.utils.DayOfWeek;

import org.junit.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class TimetableReplacementMatcherTest {

    @Test
    public void testGetTargetDateForWeekday() {
        // Mock Wednesday 2026-10-14
        Calendar wednesdayCal = Calendar.getInstance();
        wednesdayCal.set(2026, Calendar.OCTOBER, 14, 10, 30, 0);

        Date mondayDate = TimetableReplacementMatcher.getTargetDateForDayOfWeek(DayOfWeek.MONDAY, wednesdayCal);
        Calendar resultCal = Calendar.getInstance();
        resultCal.setTime(mondayDate);

        assertEquals(2026, resultCal.get(Calendar.YEAR));
        assertEquals(Calendar.OCTOBER, resultCal.get(Calendar.MONTH));
        assertEquals(12, resultCal.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void testGetTargetDateForWeekendSwitchesToNextWeek() {
        // Mock Saturday 2026-10-17
        Calendar saturdayCal = Calendar.getInstance();
        saturdayCal.set(2026, Calendar.OCTOBER, 17, 14, 0, 0);

        Date mondayDate = TimetableReplacementMatcher.getTargetDateForDayOfWeek(DayOfWeek.MONDAY, saturdayCal);
        Calendar resultCal = Calendar.getInstance();
        resultCal.setTime(mondayDate);

        // Next week Monday = 2026-10-19
        assertEquals(2026, resultCal.get(Calendar.YEAR));
        assertEquals(Calendar.OCTOBER, resultCal.get(Calendar.MONTH));
        assertEquals(19, resultCal.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void testHasReplacementForLessonMatching() {
        Calendar wednesdayCal = Calendar.getInstance();
        wednesdayCal.set(2026, Calendar.OCTOBER, 14, 10, 0, 0);

        Date mondayDate = TimetableReplacementMatcher.getTargetDateForDayOfWeek(DayOfWeek.MONDAY, wednesdayCal);

        DayReplacements mondayReplacements = new DayReplacements(mondayDate, List.of(
                new Replacement("4G2T", List.of(
                        new ReplacementChange("2", "Matematyka - Anulowano")
                ))
        ));

        List<DayReplacements> allReplacements = List.of(mondayReplacements);

        boolean hasReplacementLesson2 = TimetableReplacementMatcher.hasReplacementForLesson(
                allReplacements, DayOfWeek.MONDAY, 2, "4G2T", wednesdayCal
        );
        assertTrue(hasReplacementLesson2);

        boolean hasReplacementLesson3 = TimetableReplacementMatcher.hasReplacementForLesson(
                allReplacements, DayOfWeek.MONDAY, 3, "4G2T", wednesdayCal
        );
        assertFalse(hasReplacementLesson3);

        boolean hasReplacementOtherClass = TimetableReplacementMatcher.hasReplacementForLesson(
                allReplacements, DayOfWeek.MONDAY, 2, "4ET", wednesdayCal
        );
        assertFalse(hasReplacementOtherClass);
    }
}
