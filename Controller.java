package sample;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.ProgressBarTableCell;
import javafx.scene.layout.HBox;
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
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;


public class Controller implements Initializable {

    File directory = null; //chosen directory for final photos
    List<File> selectedFiles; //chosen files for convert
    ObservableList<ImageProcessingJob> tasks;
    ObservableList<String> variants;

    ExecutorService executor = Executors.newSingleThreadExecutor();
    int chosenVariant;

       @FXML Button threadsButton;
       @FXML Button startButton;
       @FXML Button selectDestinationButton;
       @FXML Button selectFilesButton;


    @FXML Label statusLabel;
    @FXML TableView imagesTableView;
    @FXML TableColumn<ImageProcessingJob, String> imageNameColumn;
    @FXML TableColumn<ImageProcessingJob, Double> progressColumn;
    @FXML TableColumn<ImageProcessingJob, String> statusColumn;

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        // Table column initialization:
        imageNameColumn.setCellValueFactory(  p -> new SimpleStringProperty(p.getValue().getFile().getName()));
        imageNameColumn.prefWidthProperty().bind(imagesTableView.widthProperty().divide(5.0/3.0));
        statusColumn.setCellValueFactory( p -> p.getValue().messageProperty());
        statusColumn.prefWidthProperty().bind(imagesTableView.widthProperty().divide(5));
        progressColumn.setCellFactory(ProgressBarTableCell.<ImageProcessingJob>forTableColumn());
        progressColumn.setCellValueFactory( p -> p.getValue().progressProperty().asObject());
        progressColumn.prefWidthProperty().bind(imagesTableView.widthProperty().divide(5));

        tasks = FXCollections.observableList(new ArrayList<>());
        imagesTableView.setItems(tasks);

        selectDestinationButton.setDisable(true);
        startButton.setDisable(true);
    }

    @FXML
    public void selectFiles(ActionEvent actionEvent) {
        //System.out.println("selecting files..");
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add( new FileChooser.ExtensionFilter("JPG images", "*.jpg"));
        selectedFiles = fileChooser.showOpenMultipleDialog(null);

        if(selectedFiles!=null){
            selectDestinationButton.setDisable(false);
            tasks.clear();
            for( File f : selectedFiles)
                tasks.add(new ImageProcessingJob(f));
        }
    }

    @FXML
    public void selectDestination(ActionEvent actionEvent) {

        //files chosen
           Window stage = ((Node)actionEvent.getSource()).getScene().getWindow();
           directory = new DirectoryChooser().showDialog(stage);

           if(directory!=null)
               startButton.setDisable(false);

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

        //System.out.println("Start processing..");
        threadsButton.setDisable(true);
        startButton.setDisable(true);
        selectDestinationButton.setDisable(true);
        selectFilesButton.setDisable(true);

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

            threadsButton.setDisable(false);
            selectFilesButton.setDisable(false);

        }catch(Exception e){
            System.err.println(e);
        }

   }

   @FXML
   public void changeNumberOfThreads(ActionEvent actionEvent) {

       variants = FXCollections.observableList(new ArrayList<>());
       variants.add("1 thread");
       variants.add("Common thread pool");
       variants.add("2 threads (custom pool)");
       variants.add("4 threads (custom pool)");

       ChoiceBox choiceBox = new ChoiceBox(variants);

       choiceBox.getSelectionModel().selectFirst();
       choiceBox.setPrefWidth(300);
       choiceBox.setMinWidth(300);

       HBox hBox = new HBox();
       hBox.getChildren().add(choiceBox);
       hBox.setPrefWidth(320);
       hBox.setMinWidth(320);

       Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
       alert.setHeaderText("Select the number of threads");
       alert.getDialogPane().setContent(hBox);
       alert.showAndWait();

       chosenVariant = choiceBox.getSelectionModel().getSelectedIndex();
       threadsButton.setText(choiceBox.getSelectionModel().getSelectedItem().toString());

       switch(chosenVariant){
           case 0: //thread: 1
               executor = Executors.newSingleThreadExecutor();
               break;

           case 1: //common thread pool
               executor = new ForkJoinPool();
               break;

           case 2:// threads: 2
               executor = Executors.newFixedThreadPool(2);
               break;

           case 3://threads:4
               executor = Executors.newFixedThreadPool(4);
               break;

           default:
               break;
       }

    }
}
