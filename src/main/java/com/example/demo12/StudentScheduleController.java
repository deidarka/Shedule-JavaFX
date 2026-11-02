package com.example.demo12;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.shape.Line;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.*;

public class StudentScheduleController {
    @FXML private Label groupNameLabel;
    @FXML private AnchorPane schedulePane;
    @FXML private Label homeworkLabel;
    @FXML private Label reminderLabel;
    @FXML private ListView<String> materialsListView;
    @FXML private Button backButton;
    @FXML private Label scheduleNotificationLabel;
    @FXML private AnchorPane teacherSearchPane;

    // Для поиска преподавателя
    @FXML private ComboBox<String> teacherSearchCombo;
    @FXML private Label teacherSearchResultLabel;

    private String currentGroupName;
    private final Map<Integer, Integer> materialIdMap = new HashMap<>();

    public void setGroupData(String groupName) {
        currentGroupName = groupName;
        groupNameLabel.setText("Группа " + groupName);
        loadSchedule();
        loadScheduleNotifications();
        loadHomework();
        loadReminders();
        loadMaterials();

        materialsListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) downloadSelectedMaterial();
        });

        // Загружаем список преподавателей для поиска
        loadTeacherList();

        teacherSearchCombo.setOnAction(e -> {
            String selectedTeacher = teacherSearchCombo.getValue();
            if (selectedTeacher != null && !selectedTeacher.isEmpty()) {
                loadTeacherSchedule(selectedTeacher);
            }
        });
    }

    // --- НОВАЯ ЛОГИКА ДЛЯ ПОИСКА ПРЕПОДАВАТЕЛЯ ---
    private void loadTeacherList() {
        teacherSearchCombo.getItems().clear();
        String sql = "SELECT full_name FROM teachers WHERE is_active = TRUE ORDER BY full_name";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                teacherSearchCombo.getItems().add(rs.getString("full_name"));
            }
        } catch (SQLException e) {
            teacherSearchResultLabel.setText("Ошибка загрузки списка преподавателей: " + e.getMessage());
        }
    }

    private void loadTeacherSchedule(String teacherName) {
        teacherSearchPane.getChildren().clear();

        // Получаем ID преподавателя
        int teacherId = -1;
        String idSql = "SELECT id FROM teachers WHERE full_name = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(idSql)) {
            stmt.setString(1, teacherName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) teacherId = rs.getInt("id");
            }
        } catch (SQLException e) {
            Label error = new Label("Ошибка: " + e.getMessage());
            error.setLayoutX(10);
            error.setLayoutY(10);
            teacherSearchPane.getChildren().add(error);
            return;
        }

        if (teacherId == -1) return;

        String[] days = {"понедельник", "вторник", "среда", "четверг", "пятница"};
        String[] displayDays = {"ПОНЕДЕЛЬНИК", "ВТОРНИК", "СРЕДА", "ЧЕТВЕРГ", "ПЯТНИЦА"};

        String sql = "SELECT ss.day_of_week, ss.lesson_number, sub.name AS subject_name, " +
                "STRING_AGG(DISTINCT g.name, ', ') AS groups_list, c.number AS classroom " +
                "FROM lessons l " +
                "JOIN schedule_slots ss ON ss.id = l.slot_id " +
                "JOIN subjects sub ON sub.id = l.subject_id " +
                "LEFT JOIN lesson_groups lg ON lg.lesson_id = l.id " +
                "LEFT JOIN groups g ON g.id = lg.group_id " +
                "LEFT JOIN classrooms c ON c.id = l.classroom_id " +
                "WHERE l.teacher_id = ? " +
                "GROUP BY ss.day_of_week, ss.lesson_number, sub.name, c.number " +
                "ORDER BY CASE LOWER(ss.day_of_week) WHEN 'понедельник' THEN 1 WHEN 'вторник' THEN 2 WHEN 'среда' THEN 3 WHEN 'четверг' THEN 4 WHEN 'пятница' THEN 5 ELSE 6 END, ss.lesson_number";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, teacherId);

            // Сохраняем расписание в Map для удобного вывода (как в loadSchedule)
            Map<String, List<String>> scheduleMap = new LinkedHashMap<>();
            for (String day : days) scheduleMap.put(day, new ArrayList<>());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String day = rs.getString("day_of_week").toLowerCase();
                    int lesson = rs.getInt("lesson_number");
                    String subject = rs.getString("subject_name");
                    String groups = rs.getString("groups_list");
                    String classroom = rs.getString("classroom");

                    // Формируем точно такую же строку, как в обычном расписании группы
                    String line = lesson + ". пара: " + subject +
                            (groups != null ? " — " + groups : " — группа не назначена") +
                            (classroom != null ? " — кабинет " + classroom : " — кабинет не назначен");
                    scheduleMap.get(day).add(line);
                }
            }

            // Теперь рисуем на AnchorPane, как в loadSchedule
            double y = 5; // Уменьшили начальный отступ (было 15)

            for (int i = 0; i < days.length; i++) {
                // Заголовок дня
                Label dayLabel = new Label(displayDays[i]);
                dayLabel.setLayoutX(10);
                dayLabel.setLayoutY(y);
                dayLabel.setPrefWidth(720);
                dayLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #1E2A2A;");
                teacherSearchPane.getChildren().add(dayLabel);

                y += 25; // Чуть уменьшили отступ до линии (было 27)
                // Разделительная линия
                Line separator = new Line(10, y, 720, y);
                separator.setStroke(javafx.scene.paint.Color.web("#607D75"));
                separator.setStrokeWidth(1.2);
                teacherSearchPane.getChildren().add(separator);

                y += 10; // Уменьшили отступ от линии до первой пары

                // Выводим пары
                List<String> lessons = scheduleMap.get(days[i]);
                if (lessons.isEmpty()) {
                    Label noLessonLabel = new Label("нет пар");
                    noLessonLabel.setLayoutX(10);
                    noLessonLabel.setLayoutY(y);
                    noLessonLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #1E2A2A;");
                    teacherSearchPane.getChildren().add(noLessonLabel);
                    y += 25;
                } else {
                    for (String l : lessons) {
                        Label lessonLabel = new Label(l);
                        lessonLabel.setLayoutX(10);
                        lessonLabel.setLayoutY(y);
                        lessonLabel.setPrefWidth(720);
                        lessonLabel.setWrapText(true);
                        lessonLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #1E2A2A;");
                        teacherSearchPane.getChildren().add(lessonLabel);
                        y += 25; // Расстояние между парами
                    }
                }
                y += 10; // Уменьшили отступ перед следующим днем (было 15)
            }

        } catch (SQLException e) {
            Label error = new Label("Ошибка загрузки расписания преподавателя: " + e.getMessage());
            error.setLayoutX(10);
            error.setLayoutY(10);
            teacherSearchPane.getChildren().add(error);
        }
    }

    private void loadSchedule() {
        List<List<String>> days = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            days.add(new ArrayList<>(Arrays.asList("", "", "", "")));
        }

        String sql = "SELECT LOWER(s.day_of_week) AS day_name, " +
                "s.lesson_number, sub.name AS subject_name, " +
                "t.full_name AS teacher_name, c.number AS classroom_number " +
                "FROM groups g " +
                "JOIN lesson_groups lg ON lg.group_id = g.id " +
                "JOIN lessons l ON l.id = lg.lesson_id " +
                "JOIN schedule_slots s ON s.id = l.slot_id " +
                "JOIN subjects sub ON sub.id = l.subject_id " +
                "LEFT JOIN teachers t ON t.id = l.teacher_id " +
                "LEFT JOIN classrooms c ON c.id = l.classroom_id " +
                "WHERE g.name = ? " +
                "ORDER BY CASE LOWER(s.day_of_week) " +
                "WHEN 'понедельник' THEN 1 WHEN 'вторник' THEN 2 " +
                "WHEN 'среда' THEN 3 WHEN 'четверг' THEN 4 " +
                "WHEN 'пятница' THEN 5 ELSE 6 END, s.lesson_number";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, currentGroupName);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int day = dayIndex(rs.getString("day_name"));
                    int lesson = rs.getInt("lesson_number");
                    if (day < 0 || lesson < 1 || lesson > 4) continue;

                    String subject = rs.getString("subject_name");
                    String teacher = rs.getString("teacher_name");
                    String classroom = rs.getString("classroom_number");

                    String line = lesson + ". пара: " +
                            (subject == null ? "" : subject) +
                            (teacher == null || teacher.isBlank() ? " — преподаватель не назначен" : " — " + teacher) +
                            (classroom == null ? " — кабинет не назначен" : " — кабинет " + classroom);

                    String old = days.get(day).get(lesson - 1);
                    days.get(day).set(lesson - 1, old.isEmpty() ? line : old + " / " + line);
                }
            }

            schedulePane.getChildren().clear();
            String[] names = {"ПОНЕДЕЛЬНИК", "ВТОРНИК", "СРЕДА", "ЧЕТВЕРГ", "ПЯТНИЦА"};
            double y = 15;

            for (int i = 0; i < 5; i++) {
                Label dayLabel = new Label(names[i]);
                dayLabel.setLayoutX(10);
                dayLabel.setLayoutY(y);
                dayLabel.setPrefWidth(750);
                dayLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #1E2A2A;");
                schedulePane.getChildren().add(dayLabel);

                y += 27;
                Line separator = new Line(10, y, 750, y);
                separator.setStroke(javafx.scene.paint.Color.web("#607D75"));
                separator.setStrokeWidth(1.2);
                schedulePane.getChildren().add(separator);
                y += 12;

                for (int j = 0; j < 4; j++) {
                    Label lessonLabel = new Label(days.get(i).get(j).isEmpty() ? (j + 1) + ". пара:" : days.get(i).get(j));
                    lessonLabel.setLayoutX(10);
                    lessonLabel.setLayoutY(y);
                    lessonLabel.setPrefWidth(750);
                    lessonLabel.setWrapText(true);
                    lessonLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #1E2A2A;");
                    schedulePane.getChildren().add(lessonLabel);
                    y += 27;
                }
                y += 15;
            }
        } catch (SQLException e) {
            Label error = new Label("Ошибка загрузки расписания: " + e.getMessage());
            error.setLayoutX(10);
            error.setLayoutY(10);
            schedulePane.getChildren().setAll(error);
        }
    }

    private void loadScheduleNotifications() {
        String sql = "SELECT sn.day_of_week, sn.lesson_number, sn.change_text, t.full_name AS teacher_name " +
                "FROM schedule_notifications sn " +
                "LEFT JOIN teachers t ON t.id = sn.teacher_id " +
                "WHERE sn.group_name = ? " +
                "ORDER BY sn.created_at DESC LIMIT 10";

        StringBuilder text = new StringBuilder();
        text.append("Изменения расписания:\n\n");

        try (Connection conn = Database.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, currentGroupName);
            try (ResultSet rs = stmt.executeQuery()) {
                boolean found = false;
                while (rs.next()) {
                    found = true;
                    String day = rs.getString("day_of_week");
                    String teacher = rs.getString("teacher_name");
                    text.append(day.substring(0, 1).toUpperCase()).append(day.substring(1)).append(", ")
                            .append(rs.getInt("lesson_number")).append(" пара:\n")
                            .append(rs.getString("change_text"));
                    if (teacher != null) {
                        text.append(" (Преподаватель: ").append(teacher).append(")");
                    }
                    text.append("\n\n");
                }
                if (!found) {
                    text.append("Изменений в расписании нет.");
                }
            }
            scheduleNotificationLabel.setText(text.toString());
        } catch (SQLException e) {
            scheduleNotificationLabel.setText("Не удалось загрузить уведомления: " + e.getMessage());
        }
    }

    private int dayIndex(String day) {
        if (day == null) return -1;
        switch (day.trim().toLowerCase(Locale.ROOT)) {
            case "понедельник": return 0;
            case "вторник": return 1;
            case "среда": return 2;
            case "четверг": return 3;
            case "пятница": return 4;
            default: return -1;
        }
    }

    private void loadHomework() {
        StringBuilder text = new StringBuilder();
        String sql = "SELECT subject_name, description, due_date, teacher_name FROM homework WHERE group_name = ? ORDER BY due_date";
        try (Connection conn = Database.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, currentGroupName);
            try (ResultSet rs = stmt.executeQuery()) {
                boolean any = false;
                while (rs.next()) {
                    any = true;
                    text.append(rs.getString("subject_name")).append("\n   Задание: ")
                            .append(rs.getString("description")).append("\n   Срок: ")
                            .append(rs.getDate("due_date")).append("\n   Преподаватель: ")
                            .append(rs.getString("teacher_name")).append("\n\n");
                }
                if (!any) text.append("Нет домашних заданий.\n");
            }
        } catch (SQLException e) { text.append("Ошибка загрузки: ").append(e.getMessage()); }
        homeworkLabel.setText(text.toString());
    }

    private void loadReminders() {
        StringBuilder text = new StringBuilder();
        String sql = "SELECT message, day_of_week, lesson_number, reminder_date FROM reminders " +
                "WHERE group_name = ? AND reminder_date >= CURRENT_DATE ORDER BY reminder_date";
        try (Connection conn = Database.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, currentGroupName);
            try (ResultSet rs = stmt.executeQuery()) {
                boolean any = false;
                while (rs.next()) {
                    any = true;
                    String day = rs.getString("day_of_week");
                    Object number = rs.getObject("lesson_number");
                    text.append(rs.getString("message"));
                    if (day != null) text.append(" (").append(day).append(number == null ? "" : ", " + number + " пара").append(")");
                    text.append("\n   Дата: ").append(rs.getTimestamp("reminder_date").toLocalDateTime().toLocalDate()).append("\n\n");
                }
                if (!any) text.append("Нет активных напоминаний.\n");
            }
        } catch (SQLException e) { text.append("Ошибка загрузки: ").append(e.getMessage()); }
        reminderLabel.setText(text.toString());
    }

    private void loadMaterials() {
        materialsListView.getItems().clear();
        materialIdMap.clear();
        String sql = "SELECT id, subject_name, material_type, title, file_name FROM educational_materials " +
                "WHERE group_name IS NULL OR group_name = ? ORDER BY upload_date DESC";
        try (Connection conn = Database.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, currentGroupName);
            try (ResultSet rs = stmt.executeQuery()) {
                int index = 0;
                while (rs.next()) {
                    materialsListView.getItems().add(String.format("[%s] %s — %s (%s)",
                            rs.getString("subject_name"), rs.getString("material_type"),
                            rs.getString("title"), rs.getString("file_name")));
                    materialIdMap.put(index++, rs.getInt("id"));
                }
            }
            if (materialIdMap.isEmpty()) materialsListView.getItems().add("Нет учебных материалов");
        } catch (SQLException e) { materialsListView.getItems().add("Ошибка загрузки: " + e.getMessage()); }
    }

    @FXML private void downloadSelectedMaterial() {
        int index = materialsListView.getSelectionModel().getSelectedIndex();
        Integer id = materialIdMap.get(index);
        if (id == null) { showErrorAlert("Ошибка", "Выберите доступный материал."); return; }
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Выберите папку для сохранения файла");
        File folder = chooser.showDialog((Stage) materialsListView.getScene().getWindow());
        if (folder != null) {
            if (FileStorageService.downloadFile(id, folder.getAbsolutePath()))
                showSuccessAlert("Успех", "Файл сохранён в:\n" + folder.getAbsolutePath());
            else showErrorAlert("Ошибка", "Не удалось скачать файл.");
        }
    }

    @FXML private void goBack() {
        try {
            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(new Scene(FXMLLoader.load(getClass().getResource("schedule.fxml"))));
        } catch (IOException e) { showErrorAlert("Ошибка", "Не удалось вернуться назад: " + e.getMessage()); }
    }
    private void showErrorAlert(String title, String message) {
        Alert a = new Alert(Alert.AlertType.ERROR); a.setTitle(title); a.setHeaderText(null); a.setContentText(message); a.showAndWait();
    }
    private void showSuccessAlert(String title, String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION); a.setTitle(title); a.setHeaderText(null); a.setContentText(message); a.showAndWait();
    }
}