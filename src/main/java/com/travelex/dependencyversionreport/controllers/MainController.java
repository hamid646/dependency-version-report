// Copyright (c) 2015 Travelex Ltd

package com.travelex.dependencyversionreport.controllers;


import com.travelex.dependencyversionreport.Utils;
import com.travelex.dependencyversionreport.enums.Show;
import com.travelex.dependencyversionreport.enums.Sort;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.FlowPane;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryContents;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.ContentsService;
import org.eclipse.egit.github.core.service.DataService;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MainController {

    @Autowired
    private Environment env;
    @FXML
    private FlowPane showPanel;

    @FXML
    private ListView<String> list;

    @FXML
    private ChoiceBox<Sort> comboSort;

    @FXML
    private ChoiceBox<Show> comboShow;
    @Autowired
        MainController(){

        }

    @FXML
    void loadRepo() {
        GitHubClient client = new GitHubClient();
        client.setOAuth2Token(env.getProperty("github.token"));
        RepositoryService service = new RepositoryService(client);
        DataService dataService = new DataService(client);
        ContentsService contentsService = new ContentsService(client);

        try {
            List<Repository> repositories = service.getRepositories();

            for (Repository repo : repositories) {
                if (repo.getUrl().contains(env.getProperty("project.name"))) {
                    Platform.runLater(()->list.getItems().add(repo.getName()));
                    // Utils.scanProject(dataService, contentsService, repo);
                }
            }
        } catch (IOException e) {
            System.out.println("Failed for : " + e);
        }

    }

    @FXML
    void load() {
        String selectedItem = list.getSelectionModel().getSelectedItem();
        if (selectedItem != null)
        {
            GitHubClient client = new GitHubClient();
            client.setOAuth2Token(env.getProperty("github.token"));
            RepositoryService service = new RepositoryService(client);
            DataService dataService = new DataService(client);
            ContentsService contentsService = new ContentsService(client);

            try {
                List<Repository> repositories = service.getRepositories();
                for (Repository repo : repositories) {
                    if (repo.getUrl().contains(env.getProperty("project.name")) && repo.getName().equals(selectedItem)) {
                        Map<String, RepositoryContents> stringRepositoryContentsMap = Utils.scanProject(dataService, contentsService, repo);
                        calc(selectedItem);
                        calc2(selectedItem);
                    }
                }

            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    private void calc(String project)
    {

        String pattern = "([a-z0-9.-]+):([A-z0-9-_.]+)[\\. ]+ ([A-z0-9 .-]+) -> ([A-z0-9 .-]+)";
        System.out.println("Pattern " + pattern);
        Pattern r = Pattern.compile(pattern);

        String command = "mvn versions:display-dependency-updates";

        List<String> output = filterCommand(command, Paths.get("download/" + project).toFile());
        DependecyController dependecyController = new DependecyController();
        dependecyController.setProjectName(project);
        for (String line : output)
        {
            Matcher m = r.matcher(line);
            // m find if foudn it
            if (m.find( )) {
                dependecyController.addDepend(m.group(1), m.group(2), m.group(3), m.group(4));
            }else {
                System.out.println("WTF       : " + line);
            }
        }
        showPanel.getChildren().add(dependecyController);
    }

    private void calc2(String project)
    {
        String pattern = "\\] \\+- ([a-z0-9-_.]+):([a-z0-9-_.]+):[a-z]+:([A-z0-9.-]+)";
        Pattern r = Pattern.compile(pattern);
        String command = "mvn dependency:tree";

        List<String> output = filterCommand2(command, Paths.get("download/" + project).toFile());
        for (String line : output)
        {
            Matcher m = r.matcher(line);
            if (m.find( )) {
                System.out.println("|" + m.group(1) + " " + m.group(2) + " " + m.group(3));
            }else {
                System.out.println("WTF       : " + line);
            }
        }
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
                } else
                {
                    result.add(line);
                }
            }
        }
        return  result;
    }

    private static List<String> filterCommand2(String command, File loc) {
        List<String> lines = Utils.executeCommand(command, loc);

        List<String> result = new ArrayList<>();

        for (String line : lines) {
            if (line.startsWith("[INFO] +-")) {
                result.add(line);
            }
        }
        return  result;
    }
}
