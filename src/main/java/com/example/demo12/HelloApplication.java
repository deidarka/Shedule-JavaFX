
package com.example.demo12;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

public class HelloApplication extends Application {

    @Override
    public void start(Stage stage) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("hello-view.fxml"));
            stage.initStyle(StageStyle.DECORATED);
            stage.setTitle("Расписание");
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();
        } catch (Exception e) {

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Ошибка запуска");
            alert.setHeaderText("Приложение упало при старте");
            alert.setContentText(e.toString() + "\n\n" + e.getStackTrace()[0].toString());
            alert.showAndWait();
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        launch(args);
    }
}
