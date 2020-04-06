package org.opentripplanner.api.adapters;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "MyMap")
public class MapType {
    @XmlElement(name = "entry", required = true)
    public final List<Entry> entry = new ArrayList<Entry>();
}