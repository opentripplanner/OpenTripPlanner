package org.opentripplanner.routing.edgetype;

import com.vividsolutions.jts.geom.Geometry;

import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TransportationMode;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.TraverseResult;

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
