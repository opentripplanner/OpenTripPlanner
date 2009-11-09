package org.opentripplanner.jags.edgetype;

import com.vividsolutions.jts.geom.Geometry;

import org.opentripplanner.jags.core.State;
import org.opentripplanner.jags.core.TransportationMode;
import org.opentripplanner.jags.core.TraverseOptions;
import org.opentripplanner.jags.core.TraverseResult;

public interface Traversable {

    public TransportationMode getMode();

    public String getName();

    public String getDirection();

    public Geometry getGeometry();

    public String getStart();

    public String getEnd();

    public double getDistance();

    TraverseResult traverse(State s0, TraverseOptions wo);

    TraverseResult traverseBack(State s0, TraverseOptions wo);
}
