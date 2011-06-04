package org.opentripplanner.routing.patch;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="AgencyAndId")
public class AgencyAndIdType {
	public AgencyAndIdType(String agency, String id) {
		this.agency = agency;
		this.id = id;
	}
	public AgencyAndIdType() {
	}
	
	@XmlAttribute
	String agency;
	
	@XmlAttribute
	String id;
	
}
