package org.opentripplanner.routing.edgetype;

import org.opentripplanner.routing.core.TraverseOptions;

/** Interface for edges representing streets */
public interface StreetEdge {
    public boolean canTraverse(TraverseOptions options);
}
