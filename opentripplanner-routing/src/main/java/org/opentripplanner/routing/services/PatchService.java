package org.opentripplanner.routing.services;

import java.util.List;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.routing.patch.Patch;

public interface PatchService {

	List<Patch> getStopPatches(AgencyAndId stop);

	void apply(Patch patch);
	
}
