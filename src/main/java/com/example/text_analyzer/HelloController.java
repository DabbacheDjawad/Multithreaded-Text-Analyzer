package com.example.text_analyzer;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


public class HelloController {

    @FXML private BorderPane border_pane;
    @FXML private Button btnLoad;
    @FXML private Button btnRemove;
    @FXML private Button btnShowAll;
    @FXML private Button btnAnalyze;
    @FXML private ListView<String> fileListView;
    @FXML private TextArea previewArea;
    @FXML private TableView<TableEntry> resultsTable;
    @FXML private TableColumn<TableEntry, String> colMetric;
    @FXML private TableColumn<TableEntry, String> colValue;
    @FXML private TableColumn<TableEntry, String> colFile;
    @FXML private Label statusLabel;
    @FXML private ProgressBar overallProgress;

    private final ObservableList<String> files = FXCollections.observableArrayList();


    private final Map<String, Map<String, String>> analysisCache = new ConcurrentHashMap<>();


    private final ExecutorService executor =
            Executors.newFixedThreadPool(
                    Runtime.getRuntime().availableProcessors()
            );

    private enum ViewMode {
        ALL,
        SINGLE
    }

    private ViewMode currentViewMode = ViewMode.ALL;



    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            // Get the window (Stage) and listen for the close request
            Stage stage = (Stage) border_pane.getScene().getWindow();
            stage.setOnCloseRequest(event -> {
                executor.shutdownNow(); // Kills the threads immediately
            });
        });
        fileListView.setItems(files);
        btnAnalyze.setDisable(true);
        btnShowAll.disableProperty().bind(
                Bindings.isEmpty(resultsTable.getItems())
        );

        colFile.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getFile()));

        colMetric.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getMetric()));

        colValue.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getValue()));




        // Preview listener (only added once)
        fileListView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldFile, newFile) -> {

                    if (newFile == null) return;

                    // Preview
                    try {
                        previewArea.setText(Files.readString(Path.of(newFile)));
                    } catch (IOException e) {
                        previewArea.setText("Cannot load file preview.");
                    }

                    // Switch to SINGLE view
                    currentViewMode = ViewMode.SINGLE;
                    showResults(newFile);
                }
        );


    }

    @FXML
    private void onShowAllResults() {
        currentViewMode = ViewMode.ALL;
        showAllResults();
        statusLabel.setText("Showing all results");
    }



    @FXML
    private void onLoadFiles() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select text files");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Text Files", "*.txt", "*.md")
        );
        List<File> chosen = fc.showOpenMultipleDialog(null);

        if (chosen != null) {
            for (File file : chosen) {
                if (!files.contains(file.getAbsolutePath())) {
                    files.add(file.getAbsolutePath());
                }
            }

            btnAnalyze.setDisable(files.isEmpty());
            statusLabel.setText("Loaded " + files.size() + " file(s)");

            if (!files.isEmpty()) {
                fileListView.getSelectionModel().select(0); // select first file
            }
        }
    }

    @FXML
    private void onRemoveSelected() throws IOException {
        String selected = fileListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            if(files.size()==1) resultsTable.getItems().clear();
            files.remove(selected);
            statusLabel.setText("Removed " + selected);
            btnAnalyze.setDisable(files.isEmpty());
            previewArea.clear();
            analysisCache.remove(selected);
        }
        if(files.size()==1) previewArea.setText(Files.readString(Path.of(fileListView.getSelectionModel().getSelectedItem().toString())));
    }

    @FXML
    private void onStartAnalysis() {

        if (files.isEmpty()) {
            statusLabel.setText("No files to analyze.");
            return;
        }

        analysisCache.clear();
        resultsTable.getItems().clear();

        overallProgress.setProgress(0);
        statusLabel.setText("Analyzing " + files.size() + " files...");

        int totalFiles = files.size();
        AtomicInteger completed = new AtomicInteger(0);

        for (String filePath : files) {
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    String content = Files.readString(Path.of(filePath));
                    Map<String, String> result = TextAnalyzer.analyze(content);
                    analysisCache.put(filePath, result);
                    return null;
                }
            };

            task.setOnSucceeded(e -> {
                completed.incrementAndGet();
                overallProgress.setProgress((double) completed.get() / totalFiles);

                statusLabel.setText("Processed " + completed.get() + "/" + totalFiles);

                if (completed.get() == totalFiles) {
                    currentViewMode = ViewMode.ALL;
                    showAllResults();
                    statusLabel.setText("Analysis complete");
                }
            });



            task.setOnFailed(e -> {
                completed.incrementAndGet();
                statusLabel.setText("Error analyzing a file");
            });

            executor.submit(task);
        }
    }


    private void showResults(String filePath) {
        resultsTable.getItems().clear();

        Map<String, String> res = analysisCache.get(filePath);
        if (res == null) return;

        String fileName = Path.of(filePath).getFileName().toString();
        res.forEach((k, v) ->
                resultsTable.getItems().add(new TableEntry(fileName , k, v))
        );
    }

    private void showAllResults() {
        resultsTable.getItems().clear();

        analysisCache.forEach((filePath, result) -> {
            String fileName = Path.of(filePath).getFileName().toString();

            result.forEach((metric, value) -> {
                resultsTable.getItems().add(
                        new TableEntry(fileName, metric, value)
                );
            });
        });
    }



    // ---------------------------
    // TABLE DATA CLASS
    // ---------------------------
    public static class TableEntry {
        private final String file;
        private final String metric;
        private final String value;

        public TableEntry(String file, String metric, String value) {
            this.file = file;
            this.metric = metric;
            this.value = value;
        }

        public String getFile() { return file; }
        public String getMetric() { return metric; }
        public String getValue() { return value; }
    }

}
