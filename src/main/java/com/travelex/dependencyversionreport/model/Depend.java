// Copyright (c) 2015 Travelex Ltd

package com.travelex.dependencyversionreport.model;

import javafx.beans.property.SimpleStringProperty;

public class Depend {
    private final SimpleStringProperty artifactId;
    private final SimpleStringProperty currentVersion;
    private final SimpleStringProperty newVersion;

    public Depend(String artifactId, String currentVersion, String newVersion) {
        this.artifactId = new SimpleStringProperty(artifactId);
        this.currentVersion = new SimpleStringProperty(currentVersion);
        this.newVersion = new SimpleStringProperty(newVersion);
    }

    public String getArtifactId() {
        return artifactId.get();
    }

    public String getCurrentVersion() {
        return currentVersion.get();
    }

    public String getNewVersion() {
        return newVersion.get();
    }
}
