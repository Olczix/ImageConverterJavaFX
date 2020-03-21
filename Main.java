package sample;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

//All is done here

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        //From this *.fxml file we get application window "settings"
        Parent root = FXMLLoader.load(getClass().getResource("sample.fxml"));
        Scene scene = new Scene(root);
        stage.setMinHeight(200);
        stage.setMinWidth(600);
        stage.setTitle("Image Converter");
        stage.setScene(scene);
        stage.show();

    }

    public static void main(String[] args) {
        launch(args);
    }
}

