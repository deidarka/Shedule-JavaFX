package com.example.demo12;

import org.junit.jupiter.api.*;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class DayScheduleTest {

    private DaySchedule daySchedule;

    @BeforeEach
    void setUp() {
        List<String> lessons = Arrays.asList("Математика", "Физика", "Программирование",
                "");
        daySchedule = new DaySchedule("ПОНЕДЕЛЬНИК", lessons);
    }

    @Test
    @DisplayName("Проверка создания расписания на день")
    void testDayScheduleCreation() {
        assertNotNull(daySchedule);
    }

    @Test
    @DisplayName("Проверка форматирования расписания с занятиями")
    void testGetFormattedScheduleWithLessons() {
        String formatted = daySchedule.getFormattedSchedule();

        assertTrue(formatted.contains("ПОНЕДЕЛЬНИК"));
        assertTrue(formatted.contains("1. пара: Математика"));
        assertTrue(formatted.contains("2. пара: Физика"));
        assertTrue(formatted.contains("3. пара: Программирование"));
        assertTrue(formatted.contains("4. пара:"));
    }

    @Test
    @DisplayName("Проверка форматирования пустого расписания")
    void testGetFormattedScheduleEmptyLessons() {
        List<String> emptyLessons = Arrays.asList("", "", "", "");
        DaySchedule emptyDay = new DaySchedule("ВТОРНИК", emptyLessons);
        String formatted = emptyDay.getFormattedSchedule();

        assertTrue(formatted.contains("ВТОРНИК"));
        assertTrue(formatted.contains("1. пара:"));
        assertTrue(formatted.contains("2. пара:"));
        assertTrue(formatted.contains("3. пара:"));
        assertTrue(formatted.contains("4. пара:"));
    }

    @Test
    @DisplayName("Проверка null значений в расписании")
    void testGetFormattedScheduleWithNullLessons() {
        List<String> lessonsWithNull = Arrays.asList("Математика", null, "Физика", "");
        DaySchedule dayWithNull = new DaySchedule("СРЕДА", lessonsWithNull);
        String formatted = dayWithNull.getFormattedSchedule();

        assertTrue(formatted.contains("СРЕДА"));
        assertTrue(formatted.contains("1. пара: Математика"));
        assertTrue(formatted.contains("3. пара: Физика"));
    }
}