package org.opentripplanner.api.model;


import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "PatternShort")
public class ApiPatternShort {

    public String id;
    public String desc;

    public ApiPatternShort(org.opentripplanner.index.model.PatternShort other) {
        this.id = other.id;
        this.desc = other.desc;
    }
}
