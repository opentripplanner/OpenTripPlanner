package org.opentripplanner.routing.edgetype;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.vertextype.ElevatorOffboardVertex;
import org.opentripplanner.routing.vertextype.ElevatorOnboardVertex;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.NonLocalizedString;


/**
 * A relatively high cost edge for boarding an elevator.
 * @author mattwigway
 *
 */
public class ElevatorBoardEdge extends Edge implements BikeWalkableEdge, ElevatorEdge {

    private static final long serialVersionUID = 3925814840369402222L;

    /**
     * The polyline geometry of this edge.
     * It's generally a polyline with two coincident points, but some elevators have horizontal
     * dimension, e.g. the ones on the Eiffel Tower.
     */
    private LineString the_geom;

    public ElevatorBoardEdge(ElevatorOffboardVertex from, ElevatorOnboardVertex to) {
        super(from, to);

        // set up the geometry
        Coordinate[] coords = new Coordinate[2];
        coords[0] = new Coordinate(from.getX(), from.getY());
        coords[1] = new Coordinate(to.getX(), to.getY());
        the_geom = GeometryUtils.getGeometryFactory().createLineString(coords);
    }
    
    @Override
    public State traverse(State s0) {
        StateEditor s1 = createEditorForDrivingOrWalking(s0, this);
        if (s1 == null) {
            return null;
        }

        RoutingRequest options = s0.getOptions();
        s1.incrementWeight(options.elevatorBoardCost);
        s1.incrementTimeInSeconds(options.elevatorBoardTime);

        return s1.makeState();
    }

    @Override
    public double getDistanceMeters() {
        return 0;
    }

    @Override
    public LineString getGeometry() {
        return the_geom;
    }

    @Override
    public I18NString getName() {
        // TODO: i18n
        return new NonLocalizedString( "Elevator");
    }

    /** 
     * Since board edges always are called Elevator,
     * the name is utterly and completely bogus but is never included
     * in plans..
     */
    @Override
    public boolean hasBogusName() {
        return true;
    }
    
    public String toString() {
        return "ElevatorBoardEdge(" + fromv + " -> " + tov + ")";
    }
}
