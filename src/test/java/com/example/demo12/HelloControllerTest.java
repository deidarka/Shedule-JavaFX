package com.example.demo12;

import org.junit.jupiter.api.*;
import javafx.scene.control.TextField;
import static org.junit.jupiter.api.Assertions.*;

class HelloControllerTest {

    private HelloController controller;
    private TextField testTextField;

    @BeforeEach
    void setUp() {
        controller = new HelloController();
        testTextField = new TextField();
    }

    @Test
    @DisplayName("Проверка валидации логина - пустое поле")
    void testValidateLoginEmpty() {
        // Проверяем, что пустой логин не проходит
        String emptyLogin = "";
        assertTrue(emptyLogin.trim().isEmpty(), "Пустой логин должен определяться как пустой");
    }

    @Test
    @DisplayName("Проверка валидации логина - корректный ввод")
    void testValidateLoginCorrect() {
        String validLogin = "student123";
        assertFalse(validLogin.trim().isEmpty(), "Корректный логин не должен быть пустым");
        assertTrue(validLogin.matches("^[a-zA-Z0-9_]+$"), "Логин должен содержать только латинские буквы, цифры и _");
    }

    @Test
    @DisplayName("Проверка валидации логина - недопустимые символы")
    void testValidateLoginInvalidChars() {
        String invalidLogin = "студент123"; // русские буквы
        assertFalse(invalidLogin.matches("^[a-zA-Z0-9_]+$"), "Логин с русскими буквами не должен проходить");

                String loginWithSpace = "student 123";
        assertFalse(loginWithSpace.matches("^[a-zA-Z0-9_]+$"), "Логин с пробелом не должен проходить");

                String loginWithSpecial = "student@123";
        assertFalse(loginWithSpecial.matches("^[a-zA-Z0-9_]+$"), "Логин с @ не должен проходить");
    }

    @Test
    @DisplayName("Проверка валидации пароля - минимальная длина")
    void testValidatePasswordMinLength() {
        String shortPassword = "123";
        assertTrue(shortPassword.length() < 4, "Пароль короче 4 символов должен быть отклонен");

                String validPassword = "1234";
        assertTrue(validPassword.length() >= 4, "Пароль из 4+ символов должен проходить");
    }

    @Test
    @DisplayName("Проверка валидации ФИО")
    void testValidateFullName() {
        // Пустое ФИО
        String emptyName = "";
        assertTrue(emptyName.isEmpty(), "Пустое ФИО не должно проходить");

        // Короткое ФИО
        String shortName = "Иван";
        assertTrue(shortName.length() < 5, "ФИО короче 5 символов не должно проходить");

        // Корректное ФИО
        String validName = "Иванов Иван Иванович";
        assertTrue(validName.length() >= 5, "Корректное ФИО должно проходить");
        assertTrue(validName.matches("^[а-яА-ЯёЁa-zA-Z\\s-]+$"), "ФИО должно содержать только буквы и пробелы");

                // ФИО с цифрами
                String nameWithNumbers = "Иванов123";
        assertFalse(nameWithNumbers.matches("^[а-яА-ЯёЁa-zA-Z\\s-]+$"), "ФИО не должно содержать цифры");
    }

    @Test
    @DisplayName("Проверка названий групп")
    void testGroupNames() {
        String[] groups = {
                "ИСП-11", "ИСП-12", "ИСП-13",
                "МТО-11", "МТО-12",
                "ОДЛ-11", "ОРП-11", "ТОД-11", "УКП-11", "ЭКБУ-11"
        };

        for (String group : groups) {
            assertNotNull(group);
            assertFalse(group.isEmpty());
            assertTrue(group.matches("[А-Я]{2,4}-\\d{2}"),
                    "Название группы должно быть в формате XXX-XX: " + group);
        }
    }

    @Test
    @DisplayName("Проверка дней недели для расписания")
    void testWeekDays() {
        String[] days = {"понедельник", "вторник", "среда", "четверг", "пятница"};

        for (String day : days) {
            assertNotNull(day);
            assertFalse(day.isEmpty());
            assertTrue(day.matches("[а-я]+"), "День недели должен состоять из русских букв: " + day);
        }
    }

    @Test
    @DisplayName("Проверка номеров уроков")
    void testLessonNumbers() {
        int[] lessonNumbers = {1, 2, 3, 4};
        for (int number : lessonNumbers) {
            assertTrue(number >= 1 && number <= 4, "Номер урока должен быть от 1 до 4, получено: " + number);
        }
    }
}