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

package org.opentripplanner.routing.vertextype;

import java.util.Set;

import org.opentripplanner.common.geometry.DirectionUtils;
import org.opentripplanner.common.geometry.PackedCoordinateSequence;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.edgetype.TurnEdge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.patch.Alert;
import org.opentripplanner.routing.util.ElevationProfileSegment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;

/**
 * This vertex represents one direction of a street. Its location is the start of that street in
 * that direction. It contains most of the data used for edges from the street.
 */
public class TurnVertex extends StreetVertex {

    private static final long serialVersionUID = -385126804908021091L;

    @SuppressWarnings("unused")
    private static final Logger LOG = LoggerFactory.getLogger(TurnVertex.class);

    /**
     * This declares that greenways are streets that are more than 10x as safe as ordinary streets.
     */
    public static final double GREENWAY_SAFETY_FACTOR = 0.1;

    public LineString geometry;

    protected boolean wheelchairAccessible = true;

    private ElevationProfileSegment elevationProfileSegment;

    protected StreetTraversalPermission permission;

    double length;

    public int inAngle;

    public int outAngle;

    /** is this street a staircase */
    private boolean stairs = false;

    protected int streetClass = StreetEdge.CLASS_OTHERPATH;

    protected String edgeId;

    protected static Coordinate getCoord(LineString geometry) {
        return geometry.getCoordinateN(0);
    }

    private boolean roundabout = false;

    private Set<Alert> notes;

    private boolean hasBogusName;

    private boolean noThruTraffic = false;

    private Set<Alert> wheelchairNotes;

    public TurnVertex(Graph g, String id, LineString geometry, String name, double length,
            boolean back, Set<Alert> notes) {
        this(g, id, geometry, name, length, back, notes, null);
    }

    public TurnVertex(Graph g, String id, LineString geometry, String name,
            ElevationProfileSegment elevationProfileSegment, boolean back, Set<Alert> notes) {
        this(g, id, geometry, name, elevationProfileSegment.getLength(), back, notes,
                elevationProfileSegment);
    }

    private TurnVertex(Graph g, String id, LineString geometry, String name, double length,
            boolean back, Set<Alert> notes, ElevationProfileSegment elevationProfileSegment) {
        super(g, id + (back ? " back" : ""), getCoord(geometry), name);
        this.edgeId = id;
        this.geometry = geometry;
        this.length = length;
        if (elevationProfileSegment != null) {
            this.elevationProfileSegment = elevationProfileSegment;
        } else {
            this.elevationProfileSegment = new ElevationProfileSegment(length);
        }
        this.permission = StreetTraversalPermission.ALL;
        this.notes = notes;

        if (geometry != null) {
            double angleR = DirectionUtils.getLastAngle(geometry);
            outAngle = ((int) (180 * angleR / Math.PI) + 180 + 360) % 360;
            angleR = DirectionUtils.getFirstAngle(geometry);
            inAngle = ((int) (180 * angleR / Math.PI) + 180 + 360) % 360;
        }
    }

    public void setGeometry(LineString g) {
        geometry = g;
    }

    public PackedCoordinateSequence getElevationProfile() {
        return elevationProfileSegment.getElevationProfile();
    }

    /**
     * Returns a subset of the elevation profile given a range. The x-values of the returned
     * coordinates are adjusted accordingly when the start value is greater than 0.
     * 
     * @param start
     * @param end
     * @return a PackedCoordinateSequence
     */
    public PackedCoordinateSequence getElevationProfile(double start, double end) {
        return elevationProfileSegment.getElevationProfile(start, end);
    }

    public boolean setElevationProfile(PackedCoordinateSequence elev, boolean computed) {
        return elevationProfileSegment.setElevationProfile(elev, computed,
                permission.allows(StreetTraversalPermission.CAR));
    }

    public boolean canTraverse(RoutingRequest wo) {
        if (wo.wheelchairAccessible) {
            if (!wheelchairAccessible) {
                return false;
            }
            if (elevationProfileSegment.getMaxSlope() > wo.maxSlope) {
                return false;
            }
        }
        if (wo.getModes().getWalk() && permission.allows(StreetTraversalPermission.PEDESTRIAN)) {
            return true;
        }
        if (wo.getModes().getBicycle() && permission.allows(StreetTraversalPermission.BICYCLE)) {
            return true;
        }
        if (wo.getModes().getCar() && permission.allows(StreetTraversalPermission.CAR)) {
            return true;
        }
        return false;
    }

    public boolean canTraverse(RoutingRequest wo, TraverseMode mode) {
        if (wo.wheelchairAccessible) {
            if (!wheelchairAccessible) {
                return false;
            }
            if (elevationProfileSegment.getMaxSlope() > wo.maxSlope) {
                return false;
            }
        }
        if (mode == TraverseMode.WALK && permission.allows(StreetTraversalPermission.PEDESTRIAN)) {
            return true;
        }
        if (mode == TraverseMode.BICYCLE && permission.allows(StreetTraversalPermission.BICYCLE)) {
            return true;
        }
        if (mode == TraverseMode.CAR && permission.allows(StreetTraversalPermission.CAR)) {
            return true;
        }
        return false;
    }

    public double computeWeight(State s0, RoutingRequest options, double time) {
        double weight;
        double speed = options.getSpeed(s0.getNonTransitMode(options));
        if (options.wheelchairAccessible) {
            // in fact, a wheelchair user will probably be going slower
            // than a cyclist, having less wind resistance, but will have
            // a stronger preference for less work. Maybe it
            // evens out?
            weight = elevationProfileSegment.getSlopeSpeedEffectiveLength() / speed;
        } else if (s0.getNonTransitMode(options).equals(TraverseMode.BICYCLE)) {
            switch (options.optimize) {
            case SAFE:
                weight = elevationProfileSegment.getBicycleSafetyEffectiveLength() / speed;
                break;
            case GREENWAYS:
                weight = elevationProfileSegment.getBicycleSafetyEffectiveLength() / speed;
                if (elevationProfileSegment.getBicycleSafetyEffectiveLength() / length <= GREENWAY_SAFETY_FACTOR) {
                    // greenways are treated as even safer than they really are
                    weight *= 0.66;
                }
                break;
            case FLAT:
                weight = length / speed + elevationProfileSegment.getSlopeWorkCost();
                break;
            case QUICK:
                weight = elevationProfileSegment.getSlopeSpeedEffectiveLength() / speed;
                break;
            case TRIANGLE:
                double quick = elevationProfileSegment.getSlopeSpeedEffectiveLength();
                double safety = elevationProfileSegment.getBicycleSafetyEffectiveLength();
                double slope = elevationProfileSegment.getSlopeWorkCost();
                weight = quick * options.getTriangleTimeFactor() + slope
                        * options.getTriangleSlopeFactor() + safety
                        * options.getTriangleSafetyFactor();
                weight /= speed;
                break;
            default:
                weight = length / speed;
            }
        } else {
            weight = time;
        }
        if (stairs) {
            weight *= options.stairsReluctance;
        } else {
            weight *= options.walkReluctance;
        }
        return weight;
    }

    public void setSlopeOverride(boolean slopeOverride) {
        elevationProfileSegment.setSlopeOverride(slopeOverride);
    }

    public String toString() {
        return "<" + getLabel() + " (" + getName() + ")>";
    }

    public String getEdgeId() {
        return edgeId;
    }

    public LineString getGeometry() {
        return geometry;
    }

    public double getLength() {
        return length;
    }

    public StreetTraversalPermission getPermission() {
        return permission;
    }

    public double getBicycleSafetyEffectiveLength() {
        return elevationProfileSegment.getBicycleSafetyEffectiveLength();
    }

    public void setPermission(StreetTraversalPermission permission) {
        this.permission = permission;
    }

    public void setWheelchairAccessible(boolean wheelchairAccessible) {
        this.wheelchairAccessible = wheelchairAccessible;
    }

    public boolean isWheelchairAccessible() {
        return wheelchairAccessible;
    }

    public void setRoundabout(boolean roundabout) {
        this.roundabout = roundabout;
    }

    public boolean isRoundabout() {
        return roundabout;
    }

    public Set<Alert> getNotes() {
        return notes;
    }

    public boolean hasBogusName() {
        return hasBogusName;
    }

    public void setBogusName(boolean hasBogusName) {
        this.hasBogusName = hasBogusName;
    }

    public void setNoThruTraffic(boolean noThruTraffic) {
        this.noThruTraffic = noThruTraffic;
    }

    public boolean isNoThruTraffic() {
        return noThruTraffic;
    }

    public void setStairs(boolean stairs) {
        this.stairs = stairs;
    }

    public boolean isStairs() {
        return stairs;
    }

    public double getEffectiveLength(TraverseMode traverseMode) {
        if (traverseMode == TraverseMode.BICYCLE) {
            return elevationProfileSegment.getSlopeSpeedEffectiveLength();
        } else {
            return length;
        }
    }

    public void setWheelchairNotes(Set<Alert> wheelchairNotes) {
        this.wheelchairNotes = wheelchairNotes;
    }

    public Set<Alert> getWheelchairNotes() {
        return wheelchairNotes;
    }

    public void setStreetClass(int streetClass) {
        this.streetClass = streetClass;
    }
    
    public int getStreetClass() {
        return streetClass;
    }

    public TurnEdge makeTurnEdge(StreetVertex tov) {
        if (tov instanceof TurnVertex) {
            return new TurnEdge(this, (TurnVertex) tov);            
        }
        return new TurnEdge(this, tov);
    }

    public ElevationProfileSegment getElevationProfileSegment() {
        return elevationProfileSegment;
    }

    public void setElevationProfileSegment(ElevationProfileSegment elevationProfileSegment) {
        this.elevationProfileSegment = elevationProfileSegment;
    }
}
