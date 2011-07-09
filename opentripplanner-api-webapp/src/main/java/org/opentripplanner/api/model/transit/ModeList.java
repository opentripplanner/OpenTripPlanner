package org.opentripplanner.api.model.transit;

import java.util.List;

import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.opentripplanner.routing.core.TraverseMode;

@XmlRootElement
public class ModeList {

	@XmlElementWrapper
	public
	List<TraverseMode> modes;
	
}
