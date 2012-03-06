package org.opentripplanner.api.model.transit;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.routing.patch.AgencyAndIdAdapter;

@XmlRootElement(name = "route")
public class TransitRoute {
    @XmlElement
    @XmlJavaTypeAdapter(AgencyAndIdAdapter.class)
    public AgencyAndId id;

    @XmlAttribute
    public String routeShortName;

    @XmlAttribute
    public String routeLongName;

    @XmlAttribute
    public String routeName;

    @XmlAttribute
    public String url;
}
