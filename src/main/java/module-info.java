module main.graphtool {
    requires javafx.controls;
    requires javafx.fxml;


    opens main.graphtool to javafx.fxml;
    exports main.graphtool;
}