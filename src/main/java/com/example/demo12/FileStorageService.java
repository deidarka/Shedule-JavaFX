package com.example.demo12;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class FileStorageService {
    public static File chooseFile(Stage stage, String title, String... extensions) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        if (extensions.length > 0) {
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Файлы", extensions));
        }
        return chooser.showOpenDialog(stage);
    }

    public static boolean saveEducationalMaterial(String subject, String type, String title,
            String description, File file, String teacherName, String groupName) {
        String sql = "INSERT INTO educational_materials " +
                "(subject_name, material_type, title, description, file_name, file_data, file_size, uploaded_by, group_name) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            byte[] bytes = Files.readAllBytes(file.toPath());
            stmt.setString(1, subject);
            stmt.setString(2, type);
            stmt.setString(3, title);
            stmt.setString(4, description);
            stmt.setString(5, file.getName());
            stmt.setBytes(6, bytes);
            stmt.setLong(7, file.length());
            stmt.setString(8, teacherName);
            if (groupName == null || groupName.isBlank()) stmt.setNull(9, java.sql.Types.VARCHAR);
            else stmt.setString(9, groupName);
            return stmt.executeUpdate() > 0;
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean downloadFile(int materialId, String savePath) {
        String sql = "SELECT file_data, file_name FROM educational_materials WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, materialId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) return false;
                byte[] bytes = rs.getBytes("file_data");
                String fileName = new File(rs.getString("file_name")).getName();
                File output = new File(savePath, fileName);
                Files.write(output.toPath(), bytes);
                return true;
            }
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
