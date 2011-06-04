package org.opentripplanner.api.model.patch;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.opentripplanner.routing.patch.Patch;

@XmlRootElement
public class PatchResponse {

	@XmlElementWrapper
	public List<Patch> patches;
	
	public void addPatch(Patch patch) {
		if (patches == null) {
			patches = new ArrayList<Patch>();
		}
		patches.add(patch);
	}

}
