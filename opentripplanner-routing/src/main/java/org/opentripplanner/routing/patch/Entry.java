package org.opentripplanner.routing.patch;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

@XmlType
public class Entry {
    @XmlAttribute
    public String key;

    @XmlAttribute
    public String value;

    public Entry() {
        // empty constructor required by JAXB
    }

    public Entry(String key, String value) {
        this.key = key;
        this.value = value;
    }
}
