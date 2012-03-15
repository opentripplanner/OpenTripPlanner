package org.opentripplanner.api.model.transit;

import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.routing.patch.AgencyAndIdAdapter;

@XmlRootElement(name="stop")
public class Stop {
    @XmlElement
    @XmlJavaTypeAdapter(AgencyAndIdAdapter.class)
    public AgencyAndId id;
    
    @XmlAttribute
    public double lon;
    
    @XmlAttribute
    public double lat;
    
    @XmlAttribute 
    public String stopCode;
    
    @XmlAttribute 
    public String stopName;

    @XmlElements(value=@XmlElement(name="route"))
    public List<AgencyAndId> routes;
    
}
