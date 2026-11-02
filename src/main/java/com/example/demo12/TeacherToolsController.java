package com.example.demo12;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

public class TeacherToolsController {
    @FXML private ComboBox<String> homeworkGroupCombo, homeworkSubjectCombo;
    @FXML private TextArea homeworkDescriptionArea;
    @FXML private DatePicker dueDatePicker;
    @FXML private ListView<String> homeworkListView;

    @FXML private ComboBox<String> reminderGroupCombo, reminderDayCombo;
    @FXML private ComboBox<Integer> reminderLessonCombo;
    @FXML private TextArea reminderMessageArea;
    @FXML private DatePicker reminderDatePicker;
    @FXML private ListView<String> reminderListView;

    @FXML private ComboBox<String> materialSubjectCombo, materialTypeCombo, materialGroupCombo;
    @FXML private TextField materialTitleField;
    @FXML private TextArea materialDescriptionArea;
    @FXML private ListView<String> materialsListView;
    @FXML private Button uploadMaterialButton;

    @FXML private ComboBox<String> classroomTypeCombo;
    @FXML private ComboBox<String> classroomDayCombo;
    @FXML private ComboBox<Integer> classroomLessonCombo;
    @FXML private ListView<String> classroomSearchResultList;

    // Для нагрузки
    @FXML private Label workloadSummaryLabel;
    @FXML private ListView<String> workloadListView;

    @FXML private Button backButton;

    private String currentTeacherUsername;
    private String currentTeacherName;
    private int currentTeacherId = -1;
    private Set<String> teacherSubjects = new HashSet<>();

    @FXML
    public void initialize() {
        reminderDayCombo.getItems().setAll("понедельник", "вторник", "среда", "четверг", "пятница");
        classroomDayCombo.getItems().setAll("понедельник", "вторник", "среда", "четверг", "пятница");

        reminderLessonCombo.getItems().setAll(1, 2, 3, 4);
        classroomLessonCombo.getItems().setAll(1, 2, 3, 4);

        materialTypeCombo.getItems().setAll("Лекция", "Презентация", "Методичка", "Практикум");

        loadClassroomTypes();
    }

    private void loadClassroomTypes() {
        String sql = "SELECT DISTINCT ct.name FROM classrooms c JOIN classroom_types ct ON ct.id = c.type_id WHERE c.is_active = TRUE ORDER BY ct.name";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            classroomTypeCombo.getItems().clear();
            while (rs.next()) {
                String type = rs.getString("name");
                if (type != null && !type.isBlank()) {
                    classroomTypeCombo.getItems().add(type);
                }
            }
        } catch (SQLException e) {
            classroomSearchResultList.getItems().add("Ошибка загрузки типов: " + e.getMessage());
        }
    }

    public void setTeacherData(String username) {
        currentTeacherUsername = username;

        String query = "SELECT id, full_name FROM teachers WHERE username = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    currentTeacherId = rs.getInt("id");
                    currentTeacherName = rs.getString("full_name");
                } else {
                    showErrorAlert("Ошибка", "Преподаватель не найден.");
                    return;
                }
            }

            loadTeacherSubjects();
            loadGroups();
            loadHomeworkList();
            loadReminderList();
            loadMaterialsList();
            loadWorkload();

        } catch (SQLException e) {
            showErrorAlert("Ошибка", "Не удалось загрузить данные: " + e.getMessage());
        }
    }

    private void loadGroups() {
        String query = "SELECT DISTINCT g.name FROM teachers t " +
                "JOIN teacher_subjects ts ON ts.teacher_id = t.id " +
                "JOIN lessons l ON l.subject_id = ts.subject_id " +
                "JOIN lesson_groups lg ON lg.lesson_id = l.id " +
                "JOIN groups g ON g.id = lg.group_id " +
                "WHERE t.username = ? " +
                "AND (l.teacher_id = t.id OR l.teacher_id IS NULL) ORDER BY g.name";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, currentTeacherUsername);

            homeworkGroupCombo.getItems().clear();
            reminderGroupCombo.getItems().clear();
            materialGroupCombo.getItems().clear();

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String group = rs.getString("name");
                    homeworkGroupCombo.getItems().add(group);
                    reminderGroupCombo.getItems().add(group);
                    materialGroupCombo.getItems().add(group);
                }
            }
            materialGroupCombo.getItems().add(0, "");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadTeacherSubjects() {
        String query = "SELECT DISTINCT s.name FROM teacher_subjects ts " +
                "JOIN teachers t ON t.id = ts.teacher_id " +
                "JOIN subjects s ON s.id = ts.subject_id " +
                "WHERE t.username = ? ORDER BY s.name";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, currentTeacherUsername);
            homeworkSubjectCombo.getItems().clear();
            materialSubjectCombo.getItems().clear();

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String subject = rs.getString("name");
                    homeworkSubjectCombo.getItems().add(subject);
                    materialSubjectCombo.getItems().add(subject);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void searchFreeClassrooms() {
        String type = classroomTypeCombo.getValue();
        String day = classroomDayCombo.getValue();
        Integer lesson = classroomLessonCombo.getValue();

        classroomSearchResultList.getItems().clear();

        if (type == null || day == null || lesson == null) {
            classroomSearchResultList.getItems().add("Выберите тип кабинета, день недели и номер пары.");
            return;
        }

        String sql = "SELECT c.number, ct.name AS type_name FROM classrooms c " +
                "JOIN classroom_types ct ON ct.id = c.type_id " +
                "WHERE ct.name = ? AND c.is_active = TRUE " +
                "AND NOT EXISTS ( " +
                "    SELECT 1 FROM lessons l " +
                "    JOIN schedule_slots ss ON ss.id = l.slot_id " +
                "    WHERE l.classroom_id = c.id " +
                "      AND LOWER(ss.day_of_week) = LOWER(?) " +
                "      AND ss.lesson_number = ? " +
                ") ORDER BY c.number";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, type);
            stmt.setString(2, day);
            stmt.setInt(3, lesson);

            try (ResultSet rs = stmt.executeQuery()) {
                boolean found = false;
                while (rs.next()) {
                    found = true;
                    classroomSearchResultList.getItems().add("Кабинет " + rs.getString("number") + " (" + rs.getString("type_name") + ")");
                }
                if (!found) {
                    classroomSearchResultList.getItems().add("Нет свободных кабинетов этого типа.");
                }
            }
        } catch (SQLException e) {
            classroomSearchResultList.getItems().add("Ошибка поиска: " + e.getMessage());
        }
    }

    private void loadWorkload() {
        workloadListView.getItems().clear();
        String sql = "SELECT ss.day_of_week, ss.lesson_number, sub.name AS subject_name, " +
                "STRING_AGG(DISTINCT g.name, ', ') AS groups_list " +
                "FROM lessons l " +
                "JOIN schedule_slots ss ON ss.id = l.slot_id " +
                "JOIN subjects sub ON sub.id = l.subject_id " +
                "LEFT JOIN lesson_groups lg ON lg.lesson_id = l.id " +
                "LEFT JOIN groups g ON g.id = lg.group_id " +
                "WHERE l.teacher_id = ? " +
                "GROUP BY ss.day_of_week, ss.lesson_number, sub.name " +
                "ORDER BY CASE LOWER(ss.day_of_week) " +
                "WHEN 'понедельник' THEN 1 WHEN 'вторник' THEN 2 " +
                "WHEN 'среда' THEN 3 WHEN 'четверг' THEN 4 " +
                "WHEN 'пятница' THEN 5 ELSE 6 END, ss.lesson_number";

        int totalLessons = 0;
        Map<String, Integer> lessonsByDay = new HashMap<>();
        Map<String, Integer> lessonsBySubject = new HashMap<>();
        Set<String> uniqueGroups = new HashSet<>();

        try (Connection conn = Database.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, currentTeacherId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    totalLessons++;
                    String day = rs.getString("day_of_week");
                    String subject = rs.getString("subject_name");
                    String groups = rs.getString("groups_list");

                    lessonsByDay.put(day, lessonsByDay.getOrDefault(day, 0) + 1);
                    lessonsBySubject.put(subject, lessonsBySubject.getOrDefault(subject, 0) + 1);
                    if (groups != null && !groups.isBlank()) {
                        for (String g : groups.split(",")) uniqueGroups.add(g.trim());
                    }

                    workloadListView.getItems().add(String.format("%s, %d пара: %s [%s]",
                            day.substring(0, 1).toUpperCase() + day.substring(1),
                            rs.getInt("lesson_number"),
                            subject,
                            groups == null ? "—" : groups));
                }
            }

            if (totalLessons == 0) {
                workloadListView.getItems().add("Нет назначенных пар.");
                workloadSummaryLabel.setText("Нагрузка отсутствует.");
                return;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Всего пар в неделю: ").append(totalLessons).append("\n");
            sb.append("Количество групп: ").append(uniqueGroups.size()).append("\n\n");
            sb.append("По дням: ");
            for (String d : new String[]{"понедельник", "вторник", "среда", "четверг", "пятница"}) {
                int count = lessonsByDay.getOrDefault(d, 0);
                if (count > 0) sb.append(d.substring(0, 2)).append(" (").append(count).append("), ");
            }
            sb.setLength(sb.length() - 2);
            sb.append("\n");
            sb.append("По предметам: ");
            for (Map.Entry<String, Integer> entry : lessonsBySubject.entrySet()) {
                sb.append(entry.getKey()).append(" (").append(entry.getValue()).append("), ");
            }
            sb.setLength(sb.length() - 2);

            workloadSummaryLabel.setText(sb.toString());

        } catch (SQLException e) {
            workloadSummaryLabel.setText("Ошибка: " + e.getMessage());
        }
    }

    @FXML private void addHomework() {
        String group = homeworkGroupCombo.getValue();
        String subject = homeworkSubjectCombo.getValue();
        String description = homeworkDescriptionArea.getText().trim();
        LocalDate dueDate = dueDatePicker.getValue();
        if (group == null || subject == null || description.isEmpty() || dueDate == null) {
            showErrorAlert("Ошибка", "Заполните все поля для ДЗ.");
            return;
        }
        String sql = "INSERT INTO homework (group_name, subject_name, description, due_date, teacher_name) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = Database.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, group); stmt.setString(2, subject); stmt.setString(3, description);
            stmt.setDate(4, Date.valueOf(dueDate)); stmt.setString(5, currentTeacherName);
            stmt.executeUpdate();
            showSuccessAlert("Успех", "Домашнее задание добавлено.");
            homeworkDescriptionArea.clear(); dueDatePicker.setValue(null); loadHomeworkList();
        } catch (SQLException e) { showErrorAlert("Ошибка", "Не удалось добавить ДЗ: " + e.getMessage()); }
    }

    @FXML private void addReminder() {
        String group = reminderGroupCombo.getValue();
        String message = reminderMessageArea.getText().trim();
        LocalDate date = reminderDatePicker.getValue();
        if (group == null || message.isEmpty() || date == null) {
            showErrorAlert("Ошибка", "Заполните все поля для напоминания.");
            return;
        }
        String day = reminderDayCombo.getValue();
        Integer lesson = reminderLessonCombo.getValue();
        LocalDateTime dateTime = date.atTime(LocalTime.of(8, 0));
        String sql = "INSERT INTO reminders (group_name, teacher_name, message, day_of_week, lesson_number, reminder_date) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = Database.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, group); stmt.setString(2, currentTeacherName); stmt.setString(3, message);
            if (day == null) stmt.setNull(4, Types.VARCHAR); else stmt.setString(4, day);
            if (lesson == null) stmt.setNull(5, Types.INTEGER); else stmt.setInt(5, lesson);
            stmt.setTimestamp(6, Timestamp.valueOf(dateTime));
            stmt.executeUpdate();
            showSuccessAlert("Успех", "Напоминание добавлено.");
            reminderMessageArea.clear(); reminderDatePicker.setValue(null); loadReminderList();
        } catch (SQLException e) { showErrorAlert("Ошибка", "Не удалось добавить: " + e.getMessage()); }
    }

    @FXML private void uploadMaterial() {
        String subject = materialSubjectCombo.getValue();
        String type = materialTypeCombo.getValue();
        String title = materialTitleField.getText().trim();
        String description = materialDescriptionArea.getText().trim();
        String group = materialGroupCombo.getValue();
        if (group != null && group.isBlank()) group = null;
        if (subject == null || type == null || title.isEmpty()) {
            showErrorAlert("Ошибка", "Выберите предмет и тип материала, введите название.");
            return;
        }
        File file = FileStorageService.chooseFile((Stage) uploadMaterialButton.getScene().getWindow(),
                "Выберите файл", "*.pdf", "*.pptx", "*.docx", "*.txt", "*.jpg", "*.png");
        if (file == null) return;
        if (FileStorageService.saveEducationalMaterial(subject, type, title, description, file, currentTeacherName, group)) {
            showSuccessAlert("Успех", "Учебный материал загружен.");
            materialTitleField.clear(); materialDescriptionArea.clear(); loadMaterialsList();
        } else {
            showErrorAlert("Ошибка", "Не удалось загрузить файл. Проверьте таблицу educational_materials.");
        }
    }

    private void loadHomeworkList() {
        homeworkListView.getItems().clear();
        String sql = "SELECT group_name, subject_name, description, due_date FROM homework " +
                "WHERE teacher_name = ? ORDER BY due_date DESC";
        try (Connection conn = Database.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, currentTeacherName);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    homeworkListView.getItems().add(String.format("[%s] %s: %s (до %s)",
                            rs.getString("group_name"), rs.getString("subject_name"),
                            rs.getString("description"), rs.getDate("due_date")));
                }
            }
            if (homeworkListView.getItems().isEmpty()) {
                homeworkListView.getItems().add("Нет добавленных домашних заданий.");
            }
        } catch (SQLException e) {
            homeworkListView.getItems().setAll("Ошибка загрузки: " + e.getMessage());
        }
    }

    private void loadReminderList() {
        reminderListView.getItems().clear();
        String sql = "SELECT group_name, message, day_of_week, lesson_number, reminder_date FROM reminders " +
                "WHERE teacher_name = ? ORDER BY reminder_date DESC";
        try (Connection conn = Database.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, currentTeacherName);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String day = rs.getString("day_of_week");
                    Object lesson = rs.getObject("lesson_number");
                    String when = day == null ? "" : " (" + day + (lesson == null ? "" : ", " + lesson + " пара") + ")";
                    reminderListView.getItems().add(String.format("[%s]%s: %s (дата %s)",
                            rs.getString("group_name"), when, rs.getString("message"),
                            rs.getTimestamp("reminder_date").toLocalDateTime().toLocalDate()));
                }
            }
            if (reminderListView.getItems().isEmpty()) {
                reminderListView.getItems().add("Нет добавленных напоминаний.");
            }
        } catch (SQLException e) {
            reminderListView.getItems().setAll("Ошибка загрузки: " + e.getMessage());
        }
    }

    private void loadMaterialsList() {
        materialsListView.getItems().clear();
        String sql = "SELECT subject_name, material_type, title, file_name, group_name FROM educational_materials " +
                "WHERE uploaded_by = ? ORDER BY upload_date DESC";
        try (Connection conn = Database.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, currentTeacherName);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String group = rs.getString("group_name");
                    materialsListView.getItems().add(String.format("[%s] %s — %s%s (%s)",
                            rs.getString("subject_name"), rs.getString("material_type"), rs.getString("title"),
                            group == null ? " [для всех]" : " [" + group + "]", rs.getString("file_name")));
                }
            }
            if (materialsListView.getItems().isEmpty()) {
                materialsListView.getItems().add("Нет загруженных материалов.");
            }
        } catch (SQLException e) {
            materialsListView.getItems().setAll("Ошибка загрузки: " + e.getMessage());
        }
    }

    @FXML
    private void goBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("teacher_schedule.fxml"));
            Parent root = loader.load();
            TeacherScheduleController controller = loader.getController();
            controller.setTeacherData(currentTeacherUsername);
            ((Stage) backButton.getScene().getWindow()).setScene(new Scene(root));
        } catch (IOException e) {
            showErrorAlert("Ошибка", "Не удалось вернуться к расписанию: " + e.getMessage());
        }
    }

    private void showErrorAlert(String title, String message) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }

    private void showSuccessAlert(String title, String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }
}