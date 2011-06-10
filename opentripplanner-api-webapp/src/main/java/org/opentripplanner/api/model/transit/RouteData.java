package org.opentripplanner.api.model.transit;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.routing.patch.AgencyAndIdAdapter;
import org.opentripplanner.routing.transit_index.RouteVariant;

@XmlRootElement(name="RouteData")
public class RouteData {
	@XmlElement
	@XmlJavaTypeAdapter(AgencyAndIdAdapter.class)
	public AgencyAndId id;
	
	@XmlElementWrapper
	public List<RouteVariant> variants;
	
	@XmlElementWrapper
	public List<String> directions;
}
