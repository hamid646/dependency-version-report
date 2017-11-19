// Copyright (c) 2015 Travelex Ltd

package com.travelex.dependencyversionreport.model;

public class Report {

    private final String artifactId;
    private final String currentVersion;
    private final String newVersion;

    public Report(String artifactId, String currentVersion, String newVersion) {
        this.artifactId = artifactId;
        this.currentVersion = currentVersion;
        this.newVersion = newVersion;
    }

    public Depend transform()
    {
        return new Depend(artifactId, currentVersion, newVersion);
    }
}
