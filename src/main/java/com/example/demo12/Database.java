package com.example.demo12;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Database {
    private static final String URL = "jdbc:postgresql://localhost:5432/raspisanie";
    private static final String USER = "postgres";
    // Если пароль PostgreSQL у вас другой, измените только эту строку.
    private static final String PASSWORD = "3785";

    private Database() {}

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
