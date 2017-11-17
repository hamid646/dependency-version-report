// Copyright (c) 2015 Travelex Ltd

package com.travelex.dependencyversionreport.enums;

public enum Sort {
    DEFAULT("Default"),
    EXTERNAL_LIBS("External Libs"),
    INNER_LIBS("Inner Libs");

    private final String name;
    Sort(String name)
    {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
