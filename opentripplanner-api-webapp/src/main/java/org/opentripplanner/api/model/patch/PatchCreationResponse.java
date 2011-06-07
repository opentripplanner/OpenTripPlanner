package org.opentripplanner.api.model.patch;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "PatchCreationResponse")
public class PatchCreationResponse {

	@XmlElement
	public String status;

}
