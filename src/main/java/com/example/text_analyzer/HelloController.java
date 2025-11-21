package com.example.text_analyzer;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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
    }

    @FXML
    private void onLoadFiles() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select text files");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Text Files", "*.txt", "*.md", "*.log")
        );

        //adding files
        List<File> chosen = fc.showOpenMultipleDialog(null);
        if (chosen != null) {
            for (File file : chosen) {
                if (!files.contains(file.getAbsolutePath())) {
                    files.add(file.getAbsolutePath());
                }
            }
            //updating ui state after adding buttons
            btnAnalyze.setDisable(files.isEmpty());
            statusLabel.setText("Loaded " + files.size() + " file(s)");

            //initial preview
            if (!files.isEmpty()) {
                String first = files.get(0);
                fileListView.getSelectionModel().select(first);
                try {
                    String content = Files.readString(Path.of(first));
                    previewArea.setText(content);
                } catch (IOException e) {
                    previewArea.setText("Cannot load file preview.");
                }

                //listener
                fileListView.getSelectionModel().selectedItemProperty().addListener((obs, oldFile, newFile) -> {
                    if (newFile != null) {
                        try {
                            String content = Files.readString(Path.of(newFile));
                            previewArea.setText(content);
                        } catch (IOException e) {
                            previewArea.setText("Cannot load file preview.");
                        }
                    }
                });
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
        }
    }

    @FXML
    private void onStartAnalysis() {
        statusLabel.setText("Analysis will start here (next sprint).");
        overallProgress.setProgress(0);
    }


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
