// Copyright (c) 2015 Travelex Ltd

package com.travelex.dependencyversionreport.model;

import javafx.beans.property.SimpleStringProperty;

public class Depend {
    private final String groupId;
    private final SimpleStringProperty artifactId;
    private final SimpleStringProperty currentVersion;
    private final SimpleStringProperty newVersion;
    private final boolean isTop;

    public Depend(String groupId, String artifactId, String currentVersion, String newVersion, boolean isTop) {
        this.groupId = groupId;
        this.artifactId = new SimpleStringProperty(artifactId);
        this.currentVersion = new SimpleStringProperty(currentVersion);
        this.newVersion = new SimpleStringProperty(newVersion);
        this.isTop = isTop;
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

    public String getGroupId() {
        return groupId;
    }

    public boolean isTop() {
        return isTop;
    }
}
