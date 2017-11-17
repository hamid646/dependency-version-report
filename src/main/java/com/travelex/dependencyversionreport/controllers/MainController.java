// Copyright (c) 2015 Travelex Ltd

package com.travelex.dependencyversionreport.controllers;


import com.travelex.dependencyversionreport.enums.Show;
import com.travelex.dependencyversionreport.enums.Sort;
import com.travelex.dependencyversionreport.utils.Utils;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ListView;
import javafx.scene.layout.FlowPane;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.service.ContentsService;
import org.eclipse.egit.github.core.service.DataService;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    @FXML
    private FlowPane showPanel;

    @FXML
    private ListView<String> list;

    @FXML
    private ChoiceBox<Sort> comboSort;

    @FXML
    private ChoiceBox<Show> comboShow;

    @Autowired
    MainController() {

    }

    @FXML
    void loadRepo() {
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            repositories = repositories == null ?
                            repositoryService.getOrgRepositories("Travelex") :
                            repositories;
            repositories.forEach(e -> {
                Platform.runLater(() -> list.getItems().add(e.getName()));
            });

        } catch (IOException e) {
            System.out.println("Failed for : " + e);
        }

        watch.stop();
        log.info("loadRepo took {} ms", watch.getTotalTimeMillis());
    }

    @FXML
    void load() {
        StopWatch watch = new StopWatch();
        watch.start();
        String selectedItem = list.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            List<String> report = new ArrayList<>();

            try {
                for (Repository repo : repositories) {
                    if (repo.getName().equals(selectedItem)) {
                        Utils.scanProject(dataService, contentsService, repo);

                        Map<String, String> mainPom = performDependencyUpdate(selectedItem);
                        Map<String, String> all = performDependencyTree(selectedItem);
                        mainPom.forEach((k, v) -> {
                            String s = all.get(k);
                            if (s != null) {
                                report.add(k + ":" + s + ":" + v);
                            }
                        });
                    }
                }


            } catch (Exception e) {
                e.printStackTrace();
            }
            Collections.sort(report, Comparator.naturalOrder());

            report.forEach(System.out::println);
        }
        watch.stop();
        log.info("load took {} ms", watch.getTotalTimeMillis());
    }

    private Map<String, String> performDependencyUpdate(String project) {
        Map<String, String> allDependencies = new HashMap<>();
        String pattern = "([a-z0-9.-]+):([A-z0-9-_.]+)[\\. ]+ ([A-z0-9 .-]+) -> ([A-z0-9 .-]+)";
        System.out.println("Pattern " + pattern);
        Pattern r = Pattern.compile(pattern);

        String command = "mvn versions:display-dependency-updates";

        List<String> output = filterCommand(command, Paths.get("download/" + project).toFile());
        DependecyController dependecyController = new DependecyController();
        dependecyController.setProjectName(project);
        for (String line : output) {
            Matcher m = r.matcher(line);
            // m find if foudn it
            if (m.find()) {
                dependecyController.addDepend(m.group(1), m.group(2), m.group(3), m.group(4));
                allDependencies.put(m.group(2), m.group(4));
            } else {
                System.out.println("WTF       : " + line);
            }
        }
        showPanel.getChildren().add(dependecyController);
        return allDependencies;
    }

    private Map<String, String> performDependencyTree(String project) {
        String pattern = "\\] \\+- ([a-z0-9-_.]+):([a-z0-9-_.]+):[a-z]+:([A-z0-9.-]+)";
        Pattern r = Pattern.compile(pattern);
        String command = "mvn dependency:tree";
        Map<String, String> mainPom = new HashMap<>();

        List<String> output = filterCommand2(command, Paths.get("download/" + project).toFile());
        for (String line : output) {
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
    void loadAll() {

    }


    private static List<String> filterCommand(String command, File loc) {
        List<String> lines = Utils.executeCommand(command, loc);

        List<String> result = new ArrayList<>();

        for (String line : lines) {
            if (line.startsWith("[INFO]   ")) {
                if (line.startsWith("[INFO]                      ")) {
                    String s = result.get(result.size() - 1);
                    s += line.substring(15);
                    result.set(result.size() - 1, s);
                } else {
                    result.add(line);
                }
            }
        }
        return result;
    }

    private static List<String> filterCommand2(String command, File loc) {
        List<String> lines = Utils.executeCommand(command, loc);

        List<String> result = new ArrayList<>();

        for (String line : lines) {
            if (line.startsWith("[INFO] +-")) {
                result.add(line);
            }
        }
        return result;
    }
}
