module com.example.text_analyzer {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.text_analyzer to javafx.fxml;
    exports com.example.text_analyzer;
}