package org.opentripplanner.api.adapters;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@XmlType
public class Entry {
    @XmlAttribute
    @JsonSerialize
    public String key;

    @XmlAttribute
    @JsonSerialize
    public String value;

    public Entry() {
        // empty constructor required by JAXB
    }

    public Entry(String key, String value) {
        this.key = key;
        this.value = value;
    }
}
