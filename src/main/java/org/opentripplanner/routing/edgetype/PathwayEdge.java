package org.opentripplanner.routing.edgetype;

import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.routing.core.TraverseMode;
import java.util.Locale;

/**
 * A walking pathway as described in GTFS
 */
public class PathwayEdge extends Edge implements BikeWalkableEdge {

    private static final long serialVersionUID = -3311099256178798982L;

    private int traversalTime;
    private double distance;
    private int steps;
    private double angle;
    private String name = "pathway";

    private boolean wheelchairAccessible = true;
    private FeedScopedId id;

    public PathwayEdge(Vertex fromv, Vertex tov, String name) {
        super(fromv, tov);
        if (name != null) this.name = name;
    }

    public PathwayEdge(
        Vertex fromv,
        Vertex tov,
        FeedScopedId id,
        String name,
        int traversalTime,
        double distance,
        int steps,
        double angle,
        boolean wheelchairAccessible
    ) {
        super(fromv, tov);
        this.id = id;
        this.traversalTime = traversalTime;
        this.distance = distance;
        this.steps = steps;
        this.angle = angle;
        this.wheelchairAccessible = wheelchairAccessible;
        if (name != null) {
            this.name = name;
        } else if (tov.getName() != null) {
            this.name = tov.getName();
        }
    }

    public String getDirection() {
        return null;
    }

    public double getDistanceMeters() {
        return this.distance;
    }

    @Override
    public double getEffectiveWalkDistance() {
        if (traversalTime > 0) {
            return 0;
        } else {
            return distance;
        }
    }

    @Override
    public int getDistanceIndependentTime() {
        return traversalTime;
    }

    public LineString getGeometry() {
        Coordinate[] coordinates = new Coordinate[] { getFromVertex().getCoordinate(),
                getToVertex().getCoordinate() };
        return GeometryUtils.getGeometryFactory().createLineString(coordinates);
    }

    public String getName() {
        return name;
    }

    @Override
    public String getName(Locale locale) {
        //TODO: localize
        return this.getName();
    }

    public void setWheelchairAccessible(boolean wheelchairAccessible) {
        this.wheelchairAccessible = wheelchairAccessible;
    }

    public FeedScopedId getId( ){ return id; }

    public State traverse(State s0) {
        StateEditor s1 = createEditorForWalking(s0, this);
        if (s1 == null) {
            return null;
        }

        /* TODO: Consider mode, so that passing through multiple fare gates is not possible */
        int time = traversalTime;
        if (s0.getOptions().wheelchairAccessible) {
            if (!this.wheelchairAccessible) {
                return null;
            }
            if (this.angle > s0.getOptions().maxWheelchairSlope) {
                return null;
            }
        }

        if (time == 0) {
            if (distance > 0) {
                time = (int) (distance * s0.getOptions().walkSpeed);
            }
            else if (steps > 0) {
                // 1 step corresponds to 20cm, doubling that to compensate for elevation;
                time = (int) (0.4 * Math.abs(steps) * s0.getOptions().walkSpeed);
            }
        }

        if (time <= 0) {
            return null;
        }

        double weight = time * s0.getOptions().getReluctance(TraverseMode.WALK, s0.getNonTransitMode() == TraverseMode.BICYCLE);

        s1.incrementTimeInSeconds(time);
        s1.incrementWeight(weight);
        return s1.makeState();
    }
}
