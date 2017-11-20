// Copyright (c) 2015 Travelex Ltd

package com.travelex.dependencyversionreport.model;

public class Report implements DependTransform {

    private final String groupId;
    private final String artifactId;
    private final String currentVersion;
    private String newVersion;
    private boolean isTop;

    public Report(String groupId, String artifactId, String currentVersion) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.currentVersion = currentVersion;
    }

    public Report(String groupId, String artifactId, String currentVersion, String newVersion) {
        this(groupId, artifactId, currentVersion);
        this.newVersion = newVersion;
    }

    public void setTop(boolean top) {
        isTop = top;
    }

    @Override
    public Depend transform() {
        return new Depend(groupId, artifactId, currentVersion, newVersion, isTop);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Report{");
        sb.append("groupId='").append(groupId).append('\'');
        sb.append(", artifactId='").append(artifactId).append('\'');
        sb.append(", currentVersion='").append(currentVersion).append('\'');
        sb.append(", newVersion='").append(newVersion).append('\'');
        sb.append(", isTop=").append(isTop);
        sb.append('}');
        return sb.toString();
    }
}
