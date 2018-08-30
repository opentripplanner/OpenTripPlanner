package org.opentripplanner.analyst.request;

import java.util.List;

import org.opentripplanner.analyst.core.IsochroneData;
import org.opentripplanner.routing.core.RoutingRequest;

/**
 * Compute isochrones out of a shortest path tree request.
 * 
 * @author laurent
 */
public interface IsoChroneSPTRenderer {

    /**
     * @param isoChroneRequest Contains a list of cutoff times, etc...
     * @param sptRequest Contains path computation parameters (origin, modes, max walk...)
     * @return A list of IsochroneData, one for each cutoff time in the request (same order).
     */
    List<IsochroneData> getIsochrones(IsoChroneRequest isoChroneRequest, RoutingRequest sptRequest);
}
