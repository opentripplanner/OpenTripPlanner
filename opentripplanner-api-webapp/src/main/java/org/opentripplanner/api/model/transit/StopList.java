package org.opentripplanner.api.model.transit;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="StopList")
public class StopList {
    @XmlElements(value = { @XmlElement(name="stop") })
    public List<Stop> stops = new ArrayList<Stop>();

}
