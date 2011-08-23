package org.opentripplanner.routing.patch;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "MyMap")
public class MapType {
    @XmlElement(name = "entry", required = true)
    private final List<Entry> entry = new ArrayList<Entry>();
    
    public List<Entry> getEntry() {
        return this.entry;
    }
}