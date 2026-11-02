package com.example.demo12;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.io.IOException;
import java.sql.*;
import java.util.*;

public class TeacherScheduleController {
    @FXML private Label teacherNameLabel, subjectsLabel, scheduleLabel;
    @FXML private Label teacherNotificationLabel;
    @FXML private Button mondayButton, tuesdayButton, wednesdayButton, thursdayButton, fridayButton, backButton, toolsButton;

    private String currentTeacherUsername;
    private int currentTeacherId = -1;
    private String currentTeacherFullName;


    private Set<String> teacherSubjects = new HashSet<>();

    private static final String URL =
            "jdbc:postgresql://localhost:5432/raspisanie";
    private static final String USER = "postgres";
    private static final String PASSWORD = "3785";

    @FXML public void initialize() {
        if (mondayButton != null) mondayButton.setOnAction(e -> showScheduleForDay("понедельник"));
        if (tuesdayButton != null) tuesdayButton.setOnAction(e -> showScheduleForDay("вторник"));
        if (wednesdayButton != null) wednesdayButton.setOnAction(e -> showScheduleForDay("среда"));
        if (thursdayButton != null) thursdayButton.setOnAction(e -> showScheduleForDay("четверг"));
        if (fridayButton != null) fridayButton.setOnAction(e -> showScheduleForDay("пятница"));
    }

    public void setTeacherData(String username) {
        currentTeacherUsername = username;

        loadTeacherInfo();

        if (currentTeacherId != -1) {
            loadTeacherNotifications();
        }

        showScheduleForDay("понедельник");
    }

    private void loadTeacherInfo() {
        String teacherQuery =
                "SELECT id, full_name FROM teachers WHERE username = ?";

        String subjectsQuery =
                "SELECT DISTINCT s.name " +
                        "FROM teacher_subjects ts " +
                        "JOIN subjects s ON s.id = ts.subject_id " +
                        "WHERE ts.teacher_id = ? " +
                        "ORDER BY s.name";

        try (Connection connection =
                     DriverManager.getConnection(URL, USER, PASSWORD)) {

            // Получаем ID и ФИО преподавателя
            try (PreparedStatement stmt =
                         connection.prepareStatement(teacherQuery)) {

                stmt.setString(1, currentTeacherUsername);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        currentTeacherId = rs.getInt("id");
                        currentTeacherFullName = rs.getString("full_name");

                        teacherNameLabel.setText(
                                currentTeacherFullName == null
                                        ? "ФИО не указано"
                                        : currentTeacherFullName
                        );
                    } else {
                        currentTeacherId = -1;
                        currentTeacherFullName = null;
                        teacherSubjects.clear();

                        teacherNameLabel.setText("Преподаватель не найден");
                        subjectsLabel.setText("");
                        scheduleLabel.setText(
                                "Не удалось определить преподавателя."
                        );
                        return;
                    }
                }
            }

            // Загружаем предметы именно этого преподавателя
            teacherSubjects.clear();

            try (PreparedStatement stmt =
                         connection.prepareStatement(subjectsQuery)) {

                stmt.setInt(1, currentTeacherId);

                try (ResultSet rs = stmt.executeQuery()) {
                    StringBuilder subjects = new StringBuilder();

                    while (rs.next()) {
                        String subject = rs.getString("name");

                        teacherSubjects.add(subject);

                        if (subjects.length() > 0) {
                            subjects.append(", ");
                        }

                        subjects.append(subject);
                    }

                    subjectsLabel.setText(
                            subjects.length() == 0
                                    ? "Нет назначенных предметов"
                                    : subjects.toString()
                    );
                }
            }

        } catch (SQLException e) {
            currentTeacherId = -1;

            showErrorAlert(
                    "Ошибка",
                    "Не удалось загрузить данные преподавателя:\n"
                            + e.getMessage()
            );

            e.printStackTrace();
        }
    }

    private void loadTeacherNotifications() {
        String query =
                "SELECT group_name, day_of_week, lesson_number, change_text, created_at " +
                        "FROM schedule_notifications " +
                        "WHERE teacher_id = ? " +
                        "ORDER BY created_at DESC LIMIT 10";

        StringBuilder notifications = new StringBuilder();

        try (Connection connection = Database.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setInt(1, currentTeacherId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String group = rs.getString("group_name");
                    String day = rs.getString("day_of_week");
                    int lesson = rs.getInt("lesson_number");
                    String change = rs.getString("change_text");

                    notifications.append("Изменение в расписании:\n")
                            .append(day.substring(0, 1).toUpperCase())
                            .append(day.substring(1)).append(", ")
                            .append(lesson).append(" пара [")
                            .append(group).append("]:\n")
                            .append(change).append("\n\n");
                }
            }

            if (notifications.length() == 0) {
                teacherNotificationLabel.setText("Изменений в расписании нет.");
            } else {
                teacherNotificationLabel.setText(notifications.toString());
            }

        } catch (SQLException e) {
            teacherNotificationLabel.setText("Не удалось загрузить уведомления: " + e.getMessage());
        }
    }


    private void showScheduleForDay(String day) {
        if (currentTeacherId < 0) {
            scheduleLabel.setText("Не удалось определить преподавателя.");
            return;
        }

        if (teacherSubjects.isEmpty()) {
            scheduleLabel.setText("Нет назначенных предметов.");
            return;
        }

        String sql =
                "SELECT ss.lesson_number, " +
                        "sub.name AS subject_name, " +
                        "c.number AS classroom_number, " +
                        "STRING_AGG(DISTINCT g.name, ', ' ORDER BY g.name) AS groups_list " +
                        "FROM lessons l " +
                        "JOIN schedule_slots ss ON ss.id = l.slot_id " +
                        "JOIN subjects sub ON sub.id = l.subject_id " +
                        "JOIN teacher_subjects ts " +
                        "ON ts.subject_id = l.subject_id AND ts.teacher_id = ? " +
                        "LEFT JOIN classrooms c ON c.id = l.classroom_id " +
                        "LEFT JOIN lesson_groups lg ON lg.lesson_id = l.id " +
                        "LEFT JOIN groups g ON g.id = lg.group_id " +
                        "WHERE LOWER(ss.day_of_week) = LOWER(?) " +
                        "AND l.teacher_id = ? " +
                        "GROUP BY ss.lesson_number, sub.name, c.number " +
                        "ORDER BY ss.lesson_number";

        Map<Integer, List<String>> lessons = new LinkedHashMap<>();

        try (Connection conn =
                     DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, currentTeacherId);
            stmt.setString(2, day);
            stmt.setInt(3, currentTeacherId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int number = rs.getInt("lesson_number");
                    String groups = rs.getString("groups_list");
                    String subject = rs.getString("subject_name");
                    String classroom = rs.getString("classroom_number");

                    lessons.computeIfAbsent(
                            number, k -> new ArrayList<>()
                    ).add(
                            (groups == null || groups.isBlank()
                                    ? "Группа не назначена"
                                    : groups)
                                    + ": " + subject
                                    + (classroom == null
                                    ? " — кабинет не назначен"
                                    : " — кабинет " + classroom)
                    );
                }
            }

            StringBuilder text = new StringBuilder()
                    .append(day.toUpperCase(Locale.ROOT))
                    .append("\n")
                    .append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");

            boolean any = false;

            for (int i = 1; i <= 4; i++) {
                text.append(i).append(" пара:\n");

                List<String> items = lessons.get(i);

                if (items == null || items.isEmpty()) {
                    text.append("  нет пары\n");
                } else {
                    any = true;

                    for (String item : items) {
                        text.append("  ")
                                .append(item)
                                .append("\n");
                    }
                }

                text.append("\n");
            }

            if (!any) {
                text.append("Нет занятий на этот день.\n");
            }

            scheduleLabel.setText(text.toString());

        } catch (SQLException e) {
            showErrorAlert(
                    "Ошибка",
                    "Не удалось загрузить расписание:\n" + e.getMessage()
            );
            e.printStackTrace();
        }
    }

    @FXML private void openTeacherTools() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("teacher_tools.fxml"));
            Parent root = loader.load();
            TeacherToolsController controller = loader.getController();
            controller.setTeacherData(currentTeacherUsername);
            Stage stage = (Stage) toolsButton.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            showErrorAlert("Ошибка", "Не удалось открыть инструменты: " + e.getMessage());
        }
    }

    @FXML private void goBack() {
        try {
            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(new Scene(FXMLLoader.load(getClass().getResource("hello-view.fxml"))));
        } catch (IOException e) {
            showErrorAlert("Ошибка", "Не удалось вернуться назад: " + e.getMessage());
        }
    }
    private void showErrorAlert(String title, String message) {
        Alert a = new Alert(Alert.AlertType.ERROR); a.setTitle(title); a.setHeaderText(null); a.setContentText(message); a.showAndWait();
    }
}
