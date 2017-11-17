// Copyright (c) 2015 Travelex Ltd

package com.travelex.dependencyversionreport.controllers;


import com.travelex.dependencyversionreport.command.MavenCommand;
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

                        CountDownLatch latch = new CountDownLatch(2);

                        MavenCommand update = createMavenCommand(latch, selectedItem, "mvn versions:display-dependency-updates");
                        MavenCommand tree = createMavenCommand(latch, selectedItem, "mvn dependency:tree");

                        new Thread(update).start();
                        new Thread(tree).start();
                        latch.await();
                        Map<String, String> mainPom = renderTree(tree.getLines());
                        Map<String, String> all = renderUpdate(update.getLines());

                        mainPom.forEach((k, v) -> {
                            String s = all.get(k);
                            if (s != null) {
                                report.add(k + ":" + s + ":" + v);
                            }
                        });

                        break;
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

    private MavenCommand createMavenCommand(CountDownLatch latch, String project, String command) {
        return new MavenCommand(latch, command, Paths.get("download/" + project).toFile());
    }

    private Map<String, String> renderUpdate(List<String> output) {
        String pattern = "([a-z0-9.-]+):([A-z0-9-_.]+)[\\. ]+ ([A-z0-9 .-]+) -> ([A-z0-9 .-]+)";
        Map<String, String> allDependencies = new HashMap<>();
        DependecyController dependecyController = new DependecyController();
        Pattern r = Pattern.compile(pattern);
//        dependecyController.setProjectName(project);

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

    private Map<String, String> renderTree(List<String> output) {
        String pattern = "\\] \\+- ([a-z0-9-_.]+):([a-z0-9-_.]+):[a-z]+:([A-z0-9.-]+)";
        Pattern r = Pattern.compile(pattern);
        Map<String, String> mainPom = new HashMap<>();

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
}
