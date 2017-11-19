// Copyright (c) 2015 Travelex Ltd

package com.travelex.dependencyversionreport;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

public class LoadingScreen {

    private static LoadingScreen instance;

    private Stage dialog;
    private VBox dialogVbox;
    private Scene dialogScene;

    public LoadingScreen(Window win) {
        dialog = new Stage();
        dialogVbox = new VBox(20);
        dialogVbox.setAlignment(Pos.CENTER);
        dialogScene = new Scene(dialogVbox, 300, 200);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(win);
        dialogVbox.getChildren().add(new ProgressIndicator(ProgressIndicator.INDETERMINATE_PROGRESS));

        dialog.setScene(dialogScene);


    }

    public void start(String text) {
        Label l = new Label(text);
        l.setFont(Font.font(24));
        dialogVbox.getChildren().add(l);
        dialog.show();
    }

    public void remove() {
        dialog.hide();
    }
}
