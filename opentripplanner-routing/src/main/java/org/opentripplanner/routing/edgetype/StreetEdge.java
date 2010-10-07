package org.opentripplanner.routing.edgetype;

import org.opentripplanner.routing.core.TraverseOptions;

/** Interface for edges representing streets */
public interface StreetEdge extends EdgeWithElevation {
    public boolean canTraverse(TraverseOptions options);
    public double getLength();
    public StreetTraversalPermission getPermission();
}
