module com.example.demo12 {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;


    opens com.example.demo12 to javafx.fxml;
    exports com.example.demo12;
}