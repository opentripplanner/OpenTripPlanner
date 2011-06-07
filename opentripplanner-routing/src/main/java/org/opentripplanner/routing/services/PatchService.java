package org.opentripplanner.routing.services;

import java.util.Collection;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.routing.patch.Patch;

public interface PatchService {

	Collection<Patch> getStopPatches(AgencyAndId stop);

	void apply(Patch patch);
	
}
