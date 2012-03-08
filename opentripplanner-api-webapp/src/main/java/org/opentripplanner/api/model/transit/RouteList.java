package org.opentripplanner.api.model.transit;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "RouteList")
public class RouteList {
    @XmlElements(value = { @XmlElement(name="route") })
    public List<TransitRoute> routes = new ArrayList<TransitRoute>();
}
