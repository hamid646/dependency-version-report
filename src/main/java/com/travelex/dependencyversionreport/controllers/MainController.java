// Copyright (c) 2015 Travelex Ltd

package com.travelex.dependencyversionreport.controllers;


import com.travelex.dependencyversionreport.command.MavenCommand;
import com.travelex.dependencyversionreport.utils.Utils;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.service.ContentsService;
import org.eclipse.egit.github.core.service.DataService;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MainController {

    private static final Logger log = LoggerFactory.getLogger(MainController.class);

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private DataService dataService;

    @Autowired
    private ContentsService contentsService;

    private List<Repository> repositories;

    // FXML

    @FXML
    private TableView<Depend> table;

    @FXML
    private TableColumn<Depend, String> colArtifact;

    @FXML
    private TableColumn<Depend, String> colCurrent;

    @FXML
    private TableColumn<Depend, String> colNew;

    @FXML
    private TextArea pomArea;

    @FXML
    private ListView<Button> loadButtons;

    @FXML
    private TextField searchRepo;

    @FXML
    private TextField searchArtifact;

    ObservableList<Button> dataRepo = FXCollections.observableArrayList();
    FilteredList<Button> filterDataRepo = new FilteredList<>(dataRepo, s -> true);

    ObservableList<Depend> dataTable = FXCollections.observableArrayList();
    FilteredList<Depend> filteredTable = new FilteredList<>(dataTable, p -> true);

    @Autowired
    MainController() {

    }

    @FXML
    public void initialize() {
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            repositories = repositories == null ?
                            repositoryService.getOrgRepositories("Travelex") :
                            repositories;
            repositories.stream().sorted(Comparator.comparing(Repository::getName)).
                            forEach(e -> {
                                Platform.runLater(() -> {
                                    Button b = new Button(e.getName());
                                    b.setOnAction(a -> load(e.getName()));
                                    b.minWidth(100);
                                    dataRepo.add(b);
                                });
                            });

        } catch (IOException e) {
            System.out.println("Failed for : " + e);
        }
        colArtifact.setCellValueFactory(new PropertyValueFactory<>("artifactId"));
        colCurrent.setCellValueFactory(new PropertyValueFactory<>("currentVersion"));
        colNew.setCellValueFactory(new PropertyValueFactory<>("newVersion"));
        table.setPlaceholder(new Label("Contentless"));

        searchRepo.textProperty().addListener(obs->{
            String filter = searchRepo.getText();
            if(filter == null || filter.length() == 0) {
                filterDataRepo.setPredicate(s -> true);
            }
            else {
                filterDataRepo.setPredicate(s -> s.getText().contains(filter));
            }
        });

        loadButtons.setItems(filterDataRepo);
        colorTableCell();
        searchArtifact.textProperty().addListener((observable, oldValue, newValue) -> {
            colNew.setCellValueFactory(new PropertyValueFactory<>("newVersion"));
            filteredTable.setPredicate(myObject -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();
                return String.valueOf(myObject.getArtifactId()).toLowerCase().contains(lowerCaseFilter);
            });

            colorTableCell();
        });
        SortedList<Depend> sortedData = new SortedList<>(filteredTable);
        sortedData.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sortedData);

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
                                color(rowDataItem, this);
                                setText(item);
                            }
                        }
                    }
                };
            }
        });
    }

    private void color(Depend rowDataItem, TableCell<Depend, String> tableCell) {
        String[] current = rowDataItem.getCurrentVersion().split("\\.");
        String[] next = rowDataItem.getNewVersion().split("\\.");
        int max = Math.max(current.length, next.length);
        int step = 0;
        for (step = 0; step < max; step++) {
            if (!current[step].equalsIgnoreCase(next[step])) {
                switch (step) {
                    case 0:
                        tableCell.setStyle("-fx-background-color: indianred");
                        break;
                    case 1:
                        tableCell.setStyle("-fx-background-color: orange");
                        break;
                    case 2:
                        tableCell.setStyle("-fx-background-color: yellow");
                        break;
                }
                return;
            }
        }
    }


    void load(String project) {
        StopWatch watch = new StopWatch();
        watch.start();
        List<String> report = new ArrayList<>();

        try {
            for (Repository repo : repositories) {
                if (repo.getName().equals(project)) {

                    Utils.scanProject(dataService, contentsService, repo);

                    CountDownLatch latch = new CountDownLatch(2);

                    MavenCommand update = new MavenCommand(latch, "mvn versions:display-dependency-updates", Paths.get("download/" + project).toFile());
                    MavenCommand tree = new MavenCommand(latch, "mvn dependency:tree", Paths.get("download/" + project).toFile());

                    new Thread(update).start();
                    new Thread(tree).start();
                    latch.await();
                    Map<String, String> mainPom = renderTree(filterCommand2(tree.getLines()));
                    Map<String, String> all = renderUpdate(filterCommand(update.getLines()));
                    dataTable.clear();

                    mainPom.forEach((k, v) -> {
                        String s = all.get(k);
                        if (s != null) {
                            report.add(k + ":" + s + ":" + v);
                            log.info("k: {}, v: {}, s: {}", k, v , s);
                            dataTable.add(new Depend( k, v, s));
                        }
                    });

                    break;
                }
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
        Collections.sort(report, Comparator.naturalOrder());

        watch.stop();
        log.info("load took {} ms", watch.getTotalTimeMillis());
    }

    private Map<String, String> renderUpdate(List<String> output) {
        String pattern = "([a-z0-9.-]+):([A-z0-9-_.]+)[\\. ]+ ([A-z0-9 .-]+) -> ([A-z0-9 .-]+)";
        Map<String, String> allDependencies = new HashMap<>();
        Pattern r = Pattern.compile(pattern);

        for (String line : output) {
            Matcher m = r.matcher(line);
            // m find if foudn it
            if (m.find()) {
                allDependencies.put(m.group(2), m.group(4));
            } else {
                System.out.println("WTF       : " + line);
            }
        }

        return allDependencies;
    }

    private Map<String, String> renderTree(List<String> output) {
        String pattern = "\\] \\+- ([a-z0-9-_.]+):([a-z0-9-_.]+):[a-z]+:([A-z0-9.-]+)";
        Pattern r = Pattern.compile(pattern);
        Map<String, String> mainPom = new HashMap<>();

        for (String line : output) {
            System.out.println(line);
            Matcher m = r.matcher(line);
            if (m.find()) {
                mainPom.put(m.group(2), m.group(3));
            } else {
                System.out.println("WTF       : " + line);
            }
        }
        return mainPom;
    }

    @FXML
    void search() {

    }

    private static List<String> filterCommand(List<String> lines) {
        List<String> result = new ArrayList<>();

        for (String line : lines) {
            if (line.startsWith("[INFO]   ")) {
                if (line.startsWith("[INFO]                      ")) {
                    String s = result.get(result.size() - 1);
                    s += line.substring(15);
                    result.set(result.size() - 1, s);
                } else
                {
                    result.add(line);
                }
            }
        }
        return  result;
    }

    private static List<String> filterCommand2(List<String> lines) {
        List<String> result = new ArrayList<>();

        for (String line : lines) {
            if (line.startsWith("[INFO] +-")) {
                result.add(line);
            }
        }
        return  result;
    }
}
