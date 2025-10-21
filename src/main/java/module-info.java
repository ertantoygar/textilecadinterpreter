module tr.com.logidex.cad.logidexcadinterpreter {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires javafx.graphics;
    requires javafx.base;


    exports tr.com.logidex.cad.helper;
    exports tr.com.logidex.cad.model;
    exports tr.com.logidex.cad.processor;
    exports tr.com.logidex.cad;



    opens tr.com.logidex.cad to javafx.fxml;
    opens tr.com.logidex.cad.model to javafx.fxml;
    opens tr.com.logidex.cad.processor to javafx.fxml;
    opens tr.com.logidex.cad.helper to javafx.fxml;

}