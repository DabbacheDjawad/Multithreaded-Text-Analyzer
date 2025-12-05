package com.example.text_analyzer;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class HelloController {

    @FXML private Button btnLoad;
    @FXML private Button btnRemove;
    @FXML private Button btnAnalyze;
    @FXML private ListView<String> fileListView;
    @FXML private TextArea previewArea;
    @FXML private TableView<TableEntry> resultsTable;
    @FXML private TableColumn<TableEntry, String> colMetric;
    @FXML private TableColumn<TableEntry, String> colValue;
    @FXML private Label statusLabel;
    @FXML private ProgressBar overallProgress;

    private final ObservableList<String> files = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        fileListView.setItems(files);
        btnAnalyze.setDisable(true);

        colMetric.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getMetric()));

        colValue.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getValue()));

        // Preview listener (only added once)
        fileListView.getSelectionModel().selectedItemProperty().addListener((obs, oldFile, newFile) -> {
            if (newFile != null) {
                try {
                    previewArea.setText(Files.readString(Path.of(newFile)));
                } catch (IOException e) {
                    previewArea.setText("Cannot load file preview.");
                }
            }
        });
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
    private void onRemoveSelected() {
        String selected = fileListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            files.remove(selected);
            statusLabel.setText("Removed " + selected);
            btnAnalyze.setDisable(files.isEmpty());
            previewArea.clear();
        }
    }

    @FXML
    private void onStartAnalysis() {
        String selected = fileListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("No file selected.");
            return;
        }

        statusLabel.setText("Analyzing...");
        resultsTable.getItems().clear();
        overallProgress.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);

        Task<Map<String, String>> task = new Task<>() {
            @Override
            protected Map<String, String> call() throws Exception {
                String content = Files.readString(Path.of(selected));
                Thread.sleep(800); // just to show animation
                return TextAnalyzer.analyze(content);
            }
        };

        task.setOnSucceeded(e -> {
            overallProgress.setProgress(1);
            statusLabel.setText("Analysis complete");

            Map<String, String> res = task.getValue();
            res.forEach((k, v) -> resultsTable.getItems().add(new TableEntry(k, v)));
        });

        task.setOnFailed(e -> {
            overallProgress.setProgress(0);
            statusLabel.setText("Error processing file.");
        });

        // ❌ WRONG: new Thread(String.valueOf(task)).start();
        // ✔️ CORRECT:
        new Thread(task).start();
    }

    // ---------------------------
    // TABLE DATA CLASS
    // ---------------------------
    public static class TableEntry {
        private final String metric;
        private final String value;

        public TableEntry(String metric, String value) {
            this.metric = metric;
            this.value = value;
        }

        public String getMetric() { return metric; }
        public String getValue() { return value; }
    }
}
