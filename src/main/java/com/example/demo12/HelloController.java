package com.example.demo12;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class HelloController {
    @FXML private Button login, login2, back, course1, course2, course3, course4, registration;
    @FXML private TextField loginField, passwordField;
    @FXML private Button loginButton;
    @FXML private Button isp11Button, isp12Button, isp13Button, mto11Button, mto12Button, odl11Button, orp11Button, tod11Button, ukp11Button, ekbu11Button;
    @FXML private Button isp21Button, isp22Button, isp23Button, mto21Button, mto22Button, odl21Button, osa21Button, tod21Button, ukp21Button, ekbu21Button;
    @FXML private Button isp31Button, isp32Button, isp33Button, isp34Button, mto31Button, mto32Button, osa31Button, tms31Button, tod31Button, ukp31Button;
    @FXML private Button isp41Button, isp42Button, isp43Button, mto41Button, mto42Button, osa41Button, tms41Button, tod41Button;
    @FXML private TextField registrationFullName, registrationQualification, registrationLogin, registrationPassword;

    @FXML
    public void initialize() {
        setupGroupButtons();
        setupGroupButtons2();
        setupGroupButtons3();
        setupGroupButtons4();
        if (loginButton != null) loginButton.setOnAction(e -> handleLogin());
    }

    @FXML
    public void handleLogin() {
        String username = loginField == null ? "" : loginField.getText().trim();
        String password = passwordField == null ? "" : passwordField.getText();
        if (username.isEmpty() || password.isEmpty()) {
            showErrorAlert("Ошибка входа", "Введите логин и пароль.");
            return;
        }
        String sql = "SELECT id, full_name FROM teachers WHERE username = ? AND password = ? AND COALESCE(is_active, TRUE) = TRUE";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    showErrorAlert("Ошибка входа", "Неверный логин или пароль.");
                    return;
                }
            }
            FXMLLoader loader = new FXMLLoader(getClass().getResource("teacher_schedule.fxml"));
            Parent root = loader.load();
            TeacherScheduleController controller = loader.getController();
            controller.setTeacherData(username);
            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (SQLException e) {
            showErrorAlert("Ошибка базы данных", "Не удалось выполнить вход: " + e.getMessage());
        } catch (IOException e) {
            showErrorAlert("Ошибка", "Не удалось открыть расписание преподавателя: " + e.getMessage());
        }
    }

    private void setupGroupButtons() {
        bind(isp11Button, "ИСП-11"); bind(isp12Button, "ИСП-12"); bind(isp13Button, "ИСП-13");
        bind(mto11Button, "МТО-11"); bind(mto12Button, "МТО-12"); bind(odl11Button, "ОДЛ-11");
        bind(orp11Button, "ОРП-11"); bind(tod11Button, "ТОД-11"); bind(ukp11Button, "УКП-11"); bind(ekbu11Button, "ЭКБУ-11");
    }
    private void setupGroupButtons2() {
        bind(isp21Button, "ИСП-21"); bind(isp22Button, "ИСП-22"); bind(isp23Button, "ИСП-23");
        bind(mto21Button, "МТО-21"); bind(mto22Button, "МТО-22"); bind(odl21Button, "ОДЛ-21");
        bind(osa21Button, "ОСА-21"); bind(tod21Button, "ТОД-21"); bind(ukp21Button, "УКП-21"); bind(ekbu21Button, "ЭКБУ-21");
    }
    private void setupGroupButtons3() {
        bind(isp31Button, "ИСП-31"); bind(isp32Button, "ИСП-32"); bind(isp33Button, "ИСП-33"); bind(isp34Button, "ИСП-34");
        bind(mto31Button, "МТО-31"); bind(mto32Button, "МТО-32"); bind(osa31Button, "ОСА-31");
        bind(tms31Button, "ТМС-31"); bind(tod31Button, "ТОД-31"); bind(ukp31Button, "УКП-31");
    }
    private void setupGroupButtons4() {
        bind(isp41Button, "ИСП-41"); bind(isp42Button, "ИСП-42"); bind(isp43Button, "ИСП-43");
        bind(mto41Button, "МТО-41"); bind(mto42Button, "МТО-42"); bind(osa41Button, "ОСА-41");
        bind(tms41Button, "ТМС-41"); bind(tod41Button, "ТОД-41");
    }
    private void bind(Button button, String groupName) {
        if (button != null) button.setOnAction(event -> navigateToStudentSchedule(groupName, button));
    }
    private void navigateToStudentSchedule(String groupName, Button source) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("student_schedule.fxml"));
            Parent root = loader.load();
            StudentScheduleController controller = loader.getController();
            controller.setGroupData(groupName);
            Stage stage = (Stage) source.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            showErrorAlert("Ошибка", "Не удалось открыть расписание группы: " + e.getMessage());
        }
    }

    @FXML public void setSchedule1(MouseEvent event) throws IOException { changeScene("schedule.fxml", login); }
    @FXML public void loginScene2(MouseEvent event) throws IOException { changeScene("Login2.fxml", login2); }
    @FXML public void setBack(MouseEvent event) throws IOException { changeScene("hello-view.fxml", back); }
    @FXML public void setCourse1(MouseEvent event) throws IOException { changeScene("course1.fxml", course1); }
    @FXML public void setCourse2(MouseEvent event) throws IOException { changeScene("course2.fxml", course2); }
    @FXML public void setCourse3(MouseEvent event) throws IOException { changeScene("course3.fxml", course3); }
    @FXML public void setCourse4(MouseEvent event) throws IOException { changeScene("course4.fxml", course4); }
    @FXML public void setSchedule(MouseEvent event) throws IOException { changeScene("schedule.fxml", back); }
    @FXML public void registration(MouseEvent event) throws IOException { changeScene("registration.fxml", registration); }

    private void changeScene(String fxml, Node source) throws IOException {
        Stage stage = (Stage) source.getScene().getWindow();
        stage.setScene(new Scene(FXMLLoader.load(getClass().getResource(fxml))));
    }

    @FXML
    public void handleRegistration() {
        String fullName = registrationFullName.getText().trim();
        String qualification = registrationQualification.getText().trim();
        String username = registrationLogin.getText().trim();
        String password = registrationPassword.getText();
        clearRegistrationErrors();
        StringBuilder errors = new StringBuilder();
        if (fullName.length() < 5 || !fullName.matches("^[а-яА-ЯёЁa-zA-Z\\s-]+$")) {
            errors.append("• Укажите корректное ФИО (не менее 5 символов).\n");
            registrationFullName.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
        }
        if (qualification.isBlank()) {
            errors.append("• Укажите предметы/квалификацию.\n");
            registrationQualification.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
        }
        if (username.length() < 3 || !username.matches("^[a-zA-Z0-9_]+$")) {
            errors.append("• Логин: минимум 3 символа, латинские буквы, цифры и _.\n");
            registrationLogin.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
        }
        if (password.length() < 4) {
            errors.append("• Пароль должен содержать минимум 4 символа.\n");
            registrationPassword.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
        }
        if (!errors.isEmpty()) {
            showErrorAlert("Ошибка регистрации", errors.toString());
            return;
        }
        String exists = "SELECT 1 FROM teachers WHERE username = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement check = conn.prepareStatement(exists)) {
            check.setString(1, username);
            try (ResultSet rs = check.executeQuery()) {
                if (rs.next()) {
                    showErrorAlert("Ошибка регистрации", "Такой логин уже существует.");
                    return;
                }
            }
            conn.setAutoCommit(false);
            try {
                int teacherId;
                String insert = "INSERT INTO teachers (full_name, qualification, username, password, is_active) VALUES (?, ?, ?, ?, TRUE) RETURNING id";
                try (PreparedStatement stmt = conn.prepareStatement(insert)) {
                    stmt.setString(1, fullName);
                    stmt.setString(2, qualification);
                    stmt.setString(3, username);
                    stmt.setString(4, password);
                    try (ResultSet rs = stmt.executeQuery()) {
                        rs.next();
                        teacherId = rs.getInt(1);
                    }
                }
                String[] subjectNames = qualification.split("[,;]");
                String subjectSql = "INSERT INTO teacher_subjects (teacher_id, subject_id) " +
                        "SELECT ?, id FROM subjects WHERE LOWER(name) = LOWER(?) AND COALESCE(is_active, TRUE) = TRUE " +
                        "ON CONFLICT DO NOTHING";
                try (PreparedStatement stmt = conn.prepareStatement(subjectSql)) {
                    for (String name : subjectNames) {
                        if (!name.trim().isEmpty()) {
                            stmt.setInt(1, teacherId);
                            stmt.setString(2, name.trim());
                            stmt.addBatch();
                        }
                    }
                    stmt.executeBatch();
                }
                conn.commit();
                showSuccessAlert("Регистрация", "Преподаватель зарегистрирован. Если предметы не назначились автоматически, проверьте их названия в базе.");
                clearRegistrationFields();
                changeScene("hello-view.fxml", registrationFullName);
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException | IOException e) {
            showErrorAlert("Ошибка регистрации", "Не удалось зарегистрировать преподавателя: " + e.getMessage());
        }
    }

    private void clearRegistrationErrors() {
        registrationFullName.setStyle(""); registrationQualification.setStyle("");
        registrationLogin.setStyle(""); registrationPassword.setStyle("");
    }
    private void clearRegistrationFields() {
        registrationFullName.clear(); registrationQualification.clear();
        registrationLogin.clear(); registrationPassword.clear();
    }
    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(message); alert.showAndWait();
    }
    private void showSuccessAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(message); alert.showAndWait();
    }
}
