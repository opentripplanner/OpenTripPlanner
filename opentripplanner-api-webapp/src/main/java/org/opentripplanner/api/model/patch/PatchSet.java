package org.opentripplanner.api.model.patch;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import org.opentripplanner.routing.patch.Patch;
import org.opentripplanner.routing.patch.RouteNotePatch;
import org.opentripplanner.routing.patch.StopNotePatch;

@XmlRootElement(name="PatchSet")
public class PatchSet {
	@XmlElements({
	    @XmlElement(name = "StopNotePatch", type = StopNotePatch.class),
	    @XmlElement(name = "RouteNotePatch", type = RouteNotePatch.class)
	    })
	public List<Patch> patches;
	
}
