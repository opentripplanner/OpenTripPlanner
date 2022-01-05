package org.opentripplanner.routing.edgetype;

import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.TraverseMode;


/**
 * This interface allows us to set ONE extension for adding extra cost to an {@link StreetEdge}.
 */
public interface StreetEdgeCostExtension {

    /**
     * This is method is called from the street edge and allows an extension to add extra cost.
     *
     * @return zero(0) - no extra cost is added, or a positive value.
     */
    double calculateExtraCost(
            RoutingRequest options,
            int edgeLength,
            TraverseMode traverseMode
    );
}
