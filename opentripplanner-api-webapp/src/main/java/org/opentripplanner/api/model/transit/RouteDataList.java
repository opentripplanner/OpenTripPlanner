package org.opentripplanner.api.model.transit;

import java.util.ArrayList;
import java.util.Collection;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "RouteDataList")
public class RouteDataList {

    @XmlElement(name = "RouteData")
    public Collection<RouteData> routeData = new ArrayList<RouteData>();

}