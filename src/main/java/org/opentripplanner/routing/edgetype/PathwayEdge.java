package org.opentripplanner.routing.edgetype;

import java.util.Objects;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.NonLocalizedString;

/**
 * A walking pathway as described in GTFS
 */
public class PathwayEdge extends Edge implements BikeWalkableEdge {

    private static final long serialVersionUID = -3311099256178798982L;
    public static final I18NString DEFAULT_NAME = new NonLocalizedString("pathway");

    private final I18NString name;
    private final int traversalTime;
    private final double distance;
    private final int steps;
    private final double slope;

    private final boolean wheelchairAccessible;
    private final FeedScopedId id;

    /**
     * {@link PathwayEdge#lowCost(Vertex, Vertex, FeedScopedId, I18NString, boolean)}
     */
    public static PathwayEdge lowCost(Vertex fromV, Vertex toV, I18NString name) {
        return PathwayEdge.lowCost(
                fromV,
                toV,
                null,
                name,
                true
        );
    }

    /**
     * Create a PathwayEdge that doesn't have a traversal time, distance or steps.
     *
     * These are for edges which have an implied cost of almost zero just like a FreeEdge has.
     */
    public static PathwayEdge lowCost(Vertex fromV, Vertex toV, FeedScopedId id, I18NString name, boolean wheelchairAccessible) {
        return new PathwayEdge(
                fromV,
                toV,
                id,
                name,
                0,
                0,
                0,
                0,
                wheelchairAccessible
        );
    }

    public PathwayEdge(
            Vertex fromv,
            Vertex tov,
            FeedScopedId id,
            I18NString name,
            int traversalTime,
            double distance,
            int steps,
            double slope,
            boolean wheelchairAccessible
    ) {
        super(fromv, tov);
        this.name = Objects.requireNonNullElse(name, DEFAULT_NAME);
        this.id = id;
        this.traversalTime = traversalTime;
        this.steps = steps;
        this.slope = slope;
        this.wheelchairAccessible = wheelchairAccessible;
        this.distance = distance;
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
        }
        else {
            return distance;
        }
    }

    @Override
    public int getDistanceIndependentTime() {
        return traversalTime;
    }

    public LineString getGeometry() {
        Coordinate[] coordinates = new Coordinate[]{
                getFromVertex().getCoordinate(),
                getToVertex().getCoordinate()
        };
        return GeometryUtils.getGeometryFactory().createLineString(coordinates);
    }

    @Override
    public I18NString getName() {
        return name;
    }

    public FeedScopedId getId() {return id;}

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
            if (this.slope > s0.getOptions().maxWheelchairSlope) {
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

        if (time > 0){
            double weight = time * s0.getOptions()
                .getReluctance(TraverseMode.WALK, s0.getNonTransitMode() == TraverseMode.BICYCLE);

            s1.incrementTimeInSeconds(time);
            s1.incrementWeight(weight);
        } else {
            // elevators often don't have a traversal time, distance or steps, so we need to add
            // _some_ cost. the real cost is added in ElevatorHopEdge.
            // adding a cost of 1 is analogous to FreeEdge
            s1.incrementWeight(1);
        }

        return s1.makeState();
    }
}
