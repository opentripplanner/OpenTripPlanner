package org.opentripplanner.routing.edgetype;

import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.vertextype.ElevatorOffboardVertex;
import org.opentripplanner.routing.vertextype.ElevatorOnboardVertex;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import java.util.Locale;

/**
 * A relatively low cost edge for alighting from an elevator.
 * All narrative generation is done by the ElevatorAlightEdge (other edges are silent), because
 * it is the only edge that knows where the user is to get off.
 * @author mattwigway
 *
 */
public class ElevatorAlightEdge extends Edge implements ElevatorEdge {

    private static final long serialVersionUID = 3925814840369402222L;

    /**
     * This is the level of this elevator exit, used in narrative generation.
     */
    private String level;

    /**
     * The polyline geometry of this edge.
     * It's generally a polyline with two coincident points, but some elevators have horizontal
     * dimension, e.g. the ones on the Eiffel Tower.
     */
    private LineString the_geom;
    
    /**
     * @param level It's a float for future expansion.
     */
    public ElevatorAlightEdge(ElevatorOnboardVertex from, ElevatorOffboardVertex to, String level) {
        super(from, to);
        this.level = level;

        // set up the geometry
        Coordinate[] coords = new Coordinate[2];
        coords[0] = new Coordinate(from.getX(), from.getY());
        coords[1] = new Coordinate(to.getX(), to.getY());
        the_geom = GeometryUtils.getGeometryFactory().createLineString(coords);
    }
    
    @Override
    public State traverse(State s0) {
        StateEditor s1 = s0.edit(this);
        s1.incrementWeight(1);
        s1.setBackMode(TraverseMode.WALK);
        return s1.makeState();
    }

    @Override
    public double getDistance() {
        return 0;
    }

    @Override
    public LineString getGeometry() {
        return the_geom;
    }

    /** 
     * The level from OSM is the name
     */
    @Override
    public String getName() {
        return level;
    }

    /**
     * The name is not bogus; it's level n from OSM.
     * @author mattwigway
     */
    @Override 
    public boolean hasBogusName() {
        return false;
    }
    
    public String toString() {
        return "ElevatorAlightEdge(" + fromv + " -> " + tov + ")";
    }

    @Override
    public String getName(Locale locale) {
        //FIXME: no localization currently
        return level;
    }
}
