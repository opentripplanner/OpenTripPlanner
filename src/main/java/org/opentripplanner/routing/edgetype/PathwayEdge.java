package org.opentripplanner.routing.edgetype;

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

import java.util.Objects;

/**
 * A walking pathway as described in GTFS
 */
public class PathwayEdge extends Edge implements BikeWalkableEdge {

    private static final long serialVersionUID = -3311099256178798982L;
    public static final I18NString DEFAULT_NAME = new NonLocalizedString("pathway");

    private final I18NString name;
    private int traversalTime;
    private double distance;
    private int steps;
    private double angle;

    private boolean wheelchairAccessible = true;
    private FeedScopedId id;

    public PathwayEdge(Vertex fromv, Vertex tov, I18NString name) {
        super(fromv, tov);
        this.name = Objects.requireNonNullElse(name, DEFAULT_NAME);
    }

    public PathwayEdge(
        Vertex fromv,
        Vertex tov,
        FeedScopedId id,
        I18NString name,
        int traversalTime,
        double distance,
        int steps,
        double angle,
        boolean wheelchairAccessible
    ) {
        this(fromv, tov, name != null ? name : tov.getName());
        this.id = id;
        this.traversalTime = traversalTime;
        this.distance = distance;
        this.steps = steps;
        this.angle = angle;
        this.wheelchairAccessible = wheelchairAccessible;
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

    @Override
    public I18NString getName() {
        return name;
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
