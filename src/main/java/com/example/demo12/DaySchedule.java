package com.example.demo12;

import java.util.List;

public class DaySchedule {
    private String dayName;
    private List<String> lessons;

    public DaySchedule(String dayName, List<String> lessons) {
        this.dayName = dayName;
        this.lessons = lessons;
    }

    public String getFormattedSchedule() {
        StringBuilder sb = new StringBuilder();

        sb.append(dayName).append("\n");

        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

        for (int i = 0; i < lessons.size(); i++) {
            String lesson = lessons.get(i);
            if (lesson != null && !lesson.isEmpty()) {
                sb.append(i + 1).append(". пара: ").append(lesson).append("\n");
            } else {
                sb.append(i + 1).append(". пара: \n");
            }
        }

        return sb.toString();
    }
}
