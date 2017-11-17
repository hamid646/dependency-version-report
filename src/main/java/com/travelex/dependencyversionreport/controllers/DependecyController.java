// Copyright (c) 2015 Travelex Ltd

package com.travelex.dependencyversionreport.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class DependecyController extends VBox {

    @FXML
    private Text projectName;

    @FXML
    private Text upInner;

    @FXML
    private Text upExternal;

    @FXML
    private Text upTotal;

    @FXML
    private Text outInner;

    @FXML
    private Text outExternal;

    @FXML
    private Text outTotal;

    @FXML
    private Text perInner;

    @FXML
    private Text perExternal;

    @FXML
    private Text perTotal;

    @FXML
    private VBox holder;

    private ContextMenu contextMenu = new ContextMenu();

    public DependecyController() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("../../../../fxml/Dependency.fxml"));
        System.out.println(loader.getLocation());
        loader.setRoot(this);
        loader.setController(this);

        try {
            loader.load();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    void listUpdates(ContextMenuEvent event) {
        contextMenu.show(holder, event.getScreenX(), event.getScreenY());
    }

    public void setProjectName(String name) {
        projectName.setText(name);
    }

    Map<String, Menu> map = new HashMap<>();

    int outdate = 0;
    public void addDepend(String groupId, String artifactId, String oldVersion, String newVersion) {

        Menu menu = map.getOrDefault(groupId, new Menu(groupId));
        if (!map.containsKey(groupId))
        {
            map.put(groupId, menu);
            contextMenu.getItems().add(menu);
        }
        outdate++;
        menu.getItems().add(new MenuItem(artifactId + " : " + oldVersion + " -> " + newVersion));
    }

    public int getOutdate()
    {
        return outdate;
    }
}
