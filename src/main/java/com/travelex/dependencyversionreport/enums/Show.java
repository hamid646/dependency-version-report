// Copyright (c) 2015 Travelex Ltd

package com.travelex.dependencyversionreport.enums;

public enum Show {

    EXTERNAL("External"),
    ALL("All");

    private final String name;

    Show(String name)
    {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
