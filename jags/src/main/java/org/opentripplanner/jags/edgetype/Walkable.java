package org.opentripplanner.jags.edgetype;

import org.opentripplanner.jags.core.State;
import org.opentripplanner.jags.core.WalkOptions;
import org.opentripplanner.jags.core.WalkResult;

public interface Walkable{
    WalkResult walk( State s0, WalkOptions wo );
    WalkResult walkBack( State s0, WalkOptions wo );
}

