package org.opentripplanner.api.model;


public class PatternShort {

    public String id;
    public String desc;

    public PatternShort(org.opentripplanner.index.model.PatternShort other) {
        this.id = other.id;
        this.desc = other.desc;
    }
}
