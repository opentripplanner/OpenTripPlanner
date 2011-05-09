package org.opentripplanner.routing.edgetype;

import org.opentripplanner.routing.core.StateData;
import org.opentripplanner.routing.core.TraverseOptions;

public class EdgeLibrary {

    public static boolean weHaveWalkedTooFar(StateData.Editor editor, TraverseOptions options) {

        /**
         * Only apply limit in transit-only case
         */
        if (!options.getModes().getTransit())
            return false;

        /**
         * A maxWalkDistance of 0 or less indicates no limit
         */
        if (options.maxWalkDistance <= 0)
            return false;

        return editor.getWalkDistance() >= options.maxWalkDistance;
    }
}
