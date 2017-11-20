// Copyright (c) 2015 Travelex Ltd

package com.travelex.dependencyversionreport.controllers;


import com.travelex.dependencyversionreport.LoadingScreen;
import com.travelex.dependencyversionreport.model.Depend;
import com.travelex.dependencyversionreport.utils.Utils;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;
import org.eclipse.egit.github.core.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.util.Comparator;

@Component
public class MainController {

    private static final Logger log = LoggerFactory.getLogger(MainController.class);

    @Autowired
    private GitController gitController;

    @FXML
    private TableView<Depend> table;

    @FXML
    private Label projectName;

    @FXML
    private Label outdateLibs;

    @FXML
    private CheckBox isTop;

    @FXML
    private TableColumn<Depend, String> colArtifact;

    @FXML
    private TableColumn<Depend, String> colCurrent;

    @FXML
    private TableColumn<Depend, String> colNew;

    @FXML
    private ListView<Button> loadButtons;

    @FXML
    private TextField searchRepo;

    @FXML
    private TextField searchArtifact;

    ObservableList<Button> dataRepo = FXCollections.observableArrayList();
    FilteredList<Button> filterDataRepo = new FilteredList<>(dataRepo, s -> true);

    ObservableList<Depend> dataTable = FXCollections.observableArrayList();
    FilteredList<Depend> filteredTable = new FilteredList<>(dataTable, p -> isTop.isSelected() ? p.isTop() : true);

    @Autowired
    MainController() {

    }

    @FXML
    public void initialize() {
        StopWatch watch = new StopWatch();
        watch.start();

        colArtifact.setCellValueFactory(new PropertyValueFactory<>("artifactId"));
        colCurrent.setCellValueFactory(new PropertyValueFactory<>("currentVersion"));
        colNew.setCellValueFactory(new PropertyValueFactory<>("newVersion"));
        table.setPlaceholder(new Label("Contentless"));

        searchRepo.textProperty().addListener(obs -> {
            String filter = searchRepo.getText();
            if (filter == null || filter.isEmpty()) {
                filterDataRepo.setPredicate(s -> true);
            } else {
                filterDataRepo.setPredicate(s -> s.getText().contains(filter));
            }
        });

        loadButtons.setItems(filterDataRepo);
        colorTableCell();
        searchArtifact.textProperty().addListener((observable, oldValue, newValue) -> {
            colNew.setCellValueFactory(new PropertyValueFactory<>("newVersion"));
            filterTable();

            colorTableCell();
        });
        SortedList<Depend> sortedData = new SortedList<>(filteredTable);
        sortedData.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sortedData);

        gitController.loadProjects().stream().sorted(Comparator.comparing(Repository::getName)).
                        forEach(repo -> Platform.runLater(() -> {
                            Button b = new Button(repo.getName());
                            b.setOnAction(a -> {
                                final LoadingScreen ls = new LoadingScreen(table.getScene().getWindow());
                                Thread t = new Thread(() -> {
                                    Platform.runLater(() -> ls.start("Loading project files..."));
                                    dataTable.clear();
                                    gitController.loadRepo(repo).forEach(f -> dataTable.add(f.transform()));
                                    Platform.runLater(() -> ls.remove());
                                });
                                t.start();
                            });
                            b.minWidth(100);
                            dataRepo.add(b);
                            projectName.setText(repo.getName());
                            outdateLibs.setText(String.valueOf(filteredTable.size()));
                        }));

        watch.stop();
        log.info("loadRepo took {} ms", watch.getTotalTimeMillis());
    }

    private void colorTableCell() {
        colNew.setCellFactory(new Callback<TableColumn<Depend, String>, TableCell<Depend, String>>() {
            public TableCell call(TableColumn<Depend, String> param) {
                return new TableCell<Depend, String>() {
                    @Override
                    public void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (!empty) {
                            Depend rowDataItem = (Depend) this.getTableRow().getItem();
                            if (rowDataItem != null) {
                                Utils.color(rowDataItem, this);
                                setText(item);
                            }
                        }
                    }
                };
            }
        });
    }

    @FXML
    void filterTable() {

        final String searchAr = searchArtifact.getText() != null ? searchArtifact.getText().toLowerCase() : null;
        filteredTable.setPredicate(row -> {
            boolean result = row.getArtifactId().toLowerCase().contains(searchAr);
            if (result && isTop.isSelected()) {
                result = row.isTop();
            }
            return result;
        });
        outdateLibs.setText(String.valueOf(filteredTable.size()));
    }
}
