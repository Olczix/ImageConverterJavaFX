package sample;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.ProgressBarTableCell;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class Controller implements Initializable {

    File directory = null; //chosen directory for final photos
    List<File> selectedFiles; //chosen files for convert
    ObservableList<ImageProcessingJob> tasks;

    ExecutorService executor = Executors.newSingleThreadExecutor();

    @FXML Label statusLabel;
    @FXML TableView imagesTableView;
    @FXML TableColumn<ImageProcessingJob, String> imageNameColumn;
    @FXML TableColumn<ImageProcessingJob, Double> progressColumn;
    @FXML TableColumn<ImageProcessingJob, String> statusColumn;

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        // Table column initialization:
        imageNameColumn.setCellValueFactory(  p -> new SimpleStringProperty(p.getValue().getFile().getName()));
        statusColumn.setCellValueFactory( p -> p.getValue().messageProperty());
        progressColumn.setCellFactory(ProgressBarTableCell.<ImageProcessingJob>forTableColumn());
        progressColumn.setCellValueFactory( p -> p.getValue().progressProperty().asObject());

        tasks = FXCollections.observableList(new ArrayList<>());
        imagesTableView.setItems(tasks);
    }

    @FXML
    public void selectFiles(ActionEvent actionEvent) {
        //System.out.println("selecting files..");
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add( new FileChooser.ExtensionFilter("JPG images", "*.jpg"));
        selectedFiles = fileChooser.showOpenMultipleDialog(null);

        if(selectedFiles!=null){
            tasks.clear();
            for( File f : selectedFiles)
                tasks.add(new ImageProcessingJob(f));
        }
    }

    @FXML
    public void selectDestination(ActionEvent actionEvent) {

       if(tasks.isEmpty()){

            Alert er = new Alert(Alert.AlertType.ERROR);
            er.setHeaderText("Whoops!");
            er.setContentText("Pick some files first!");
            er.showAndWait();

            return;
        }

        Window stage = ((Node)actionEvent.getSource()).getScene().getWindow();
        directory = new DirectoryChooser().showDialog(stage);
    }


    @FXML
    private void processFiles(ActionEvent event) {

        if(tasks.isEmpty())
        {
            Alert er = new Alert(Alert.AlertType.ERROR);
            er.setHeaderText("Warning!");
            er.setContentText("Pick some files first!");
            er.showAndWait();
            return;
        }

        if(directory==null)
        {
            Alert er = new Alert(Alert.AlertType.ERROR);
            er.setHeaderText("Warning!");
            er.setContentText("Select directory first!");
            er.showAndWait();
            return;
        }

        new Thread(this::backgroundJob).start();
    }


   private void backgroundJob(){
        System.out.println("Start processing..");

        long start = System.currentTimeMillis(); //zwraca aktualny czas [ms]

       Platform.runLater(() -> {
           statusLabel.setText("Converter: ON");
       });

        tasks.stream().forEach(task -> {
            task.destination = directory;
            executor.submit(task);
        });

        try {
            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.HOURS);

            long duration = System.currentTimeMillis() - start;
            Platform.runLater(() -> {
                statusLabel.setText("Convert completed. Time elapsed: " + duration + " [ms]");
            });

        }catch(Exception e){

        }


   }

    public void changeNumberOfThreads(ActionEvent actionEvent) {
    }
}
