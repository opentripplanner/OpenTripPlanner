/* This program is free software: you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public License
as published by the Free Software Foundation, either version 3 of
the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.edgetype;

import org.onebusaway.gtfs.model.Elevator;
import org.onebusaway.gtfs.model.Pathway;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.routing.alertpatch.AlertPatch;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import org.opentripplanner.routing.core.TraverseMode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * A walking pathway as described in GTFS
 */
public class PathwayEdge extends Edge {

    private enum Mode { NONE, PATH, STAIRS, ELEVATOR }

    private int traversalTime;

    private int wheelchairTraversalTime = -1;

    private Mode pathwayMode = Mode.NONE;

    private List<Elevator> elevators = new ArrayList<>();

    public PathwayEdge(Vertex fromv, Vertex tov, int pathwayMode, int traversalTime, int wheelchairTraversalTime) {
        super(fromv, tov);
        this.traversalTime = traversalTime;
        this.wheelchairTraversalTime = wheelchairTraversalTime;
        switch (pathwayMode) {
            case Pathway.MODE_PATH:
                this.pathwayMode = Mode.PATH;
                break;
            case Pathway.MODE_ELEVATOR:
                this.pathwayMode = Mode.ELEVATOR;
                break;
            case Pathway.MODE_STAIRS:
                this.pathwayMode = Mode.STAIRS;
                break;
        }
    }

    public PathwayEdge(Vertex fromv, Vertex tov, int pathwayMode, int traversalTime) {
        this(fromv, tov, pathwayMode, traversalTime, -1);
    }

    private static final long serialVersionUID = -3311099256178798981L;

    public String getDirection() {
        return null;
    }

    public double getDistance() {
        return 0;
    }
    
    public TraverseMode getMode() {
       return TraverseMode.WALK;
    }

    public LineString getGeometry() {
        Coordinate[] coordinates = new Coordinate[] { getFromVertex().getCoordinate(),
                getToVertex().getCoordinate() };
        return GeometryUtils.getGeometryFactory().createLineString(coordinates);
    }

    public String getName() {
        switch(pathwayMode) {
            case ELEVATOR:
                return "elevator";
            case STAIRS:
                return "stairs";
            default:
                return "pathway";
        }
    }

    @Override
    public String getName(Locale locale) {
        //TODO: localize
        return this.getName();
    }

    public State traverse(State s0) {
        int time = traversalTime;
        if (s0.getOptions().wheelchairAccessible) {
            if (wheelchairTraversalTime < 0 || elevatorIsOutOfService(s0)) {
                return null;
            }
            time = wheelchairTraversalTime;
        }
        StateEditor s1 = s0.edit(this);
        s1.incrementTimeInSeconds(time);
        s1.incrementWeight(time);
        s1.setBackMode(getMode());
        return s1.makeState();
    }

    private boolean elevatorIsOutOfService(State s0) {
        Set<String> outages = new HashSet<>();
        for (AlertPatch alert : s0.getOptions().rctx.graph.getAlertPatches(this)) {
            if (alert.getStop() != null) {
                outages.add(alert.getStop().getId());
            }
        }
        for (Elevator elevator : elevators) {
            if (outages.contains(elevator.getElevatorId().getId())) {
                return true;
            }
        }
        return false;
    }

    public void addElevator(Elevator elevator) {
        elevators.add(elevator);
    }

    public List<Elevator> getElevators() {
        return elevators;
    }
}
