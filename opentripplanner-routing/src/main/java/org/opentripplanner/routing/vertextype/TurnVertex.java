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

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.opentripplanner.common.geometry.DirectionUtils;
import org.opentripplanner.common.geometry.PackedCoordinateSequence;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.patch.Alert;
import org.opentripplanner.routing.util.ElevationUtils;
import org.opentripplanner.routing.util.SlopeCosts;
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

    private static final Logger LOG = LoggerFactory.getLogger(TurnVertex.class);

    /**
     * This declares that greenways are streets that are more than 10x as safe as ordinary streets.
     */
    public static final double GREENWAY_SAFETY_FACTOR = 0.1;

    public LineString geometry;

    protected boolean wheelchairAccessible = true;

    protected double maxSlope;

    protected PackedCoordinateSequence elevationProfile;

    protected boolean slopeOverride;

    protected double slopeSpeedEffectiveLength;

    protected double slopeWorkCost;

    protected StreetTraversalPermission permission;

    double length;

    public int inAngle;

    public int outAngle;

    /** is this street a staircase */
    private boolean stairs = false;

    protected boolean crossable = true; // can this street be safely crossed? (unused)

    protected double bicycleSafetyEffectiveLength;

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
        super(g, id + (back ? " back" : ""), getCoord(geometry), name);
        this.edgeId = id;
        this.geometry = geometry;
        this.length = length;
        this.bicycleSafetyEffectiveLength = length;
        this.slopeWorkCost = length;
        this.slopeSpeedEffectiveLength = length;
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
        return elevationProfile;
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
        if (elevationProfile == null)
            return null;
        List<Coordinate> coordList = new LinkedList<Coordinate>();

        if (start < 0)
            start = 0;
        if (end > length)
            end = length;

        for (Coordinate coord : elevationProfile.toCoordinateArray()) {
            if (coord.x >= start && coord.x <= end) {
                coordList.add(new Coordinate(coord.x - start, coord.y));
            }
        }

        Coordinate coordArr[] = new Coordinate[coordList.size()];
        return new PackedCoordinateSequence.Float(coordList.toArray(coordArr), 2);
    }

    // TODO: there is duplicate code in TurnVertex and PlainStreetEdge. 
    // We may want a separate StreetSegment class/interface.
    public boolean setElevationProfile(PackedCoordinateSequence elev, boolean computed) {
        if (elev == null || elev.size() < 2) {
            return false;
        }
        if (slopeOverride && !computed) {
            return false;
        }

        elevationProfile = elev;

        // compute the various costs of the elevation changes
        double lengthMultiplier = ElevationUtils.getLengthMultiplierFromElevation(elev);
        if (Double.isNaN(lengthMultiplier)) {
            LOG.error("lengthMultiplier from elevation profile is NaN, setting to 1");
            lengthMultiplier = 1;
        }
        length *= lengthMultiplier;
        bicycleSafetyEffectiveLength *= lengthMultiplier;

        SlopeCosts costs = ElevationUtils.getSlopeCosts(elev, permission.allows(StreetTraversalPermission.CAR));
        slopeSpeedEffectiveLength = costs.slopeSpeedEffectiveLength;
        maxSlope = costs.maxSlope;
        slopeWorkCost = costs.slopeWorkCost;
        bicycleSafetyEffectiveLength += costs.slopeSafetyCost;
        return costs.flattened;
    }

    public boolean canTraverse(TraverseOptions wo) {
        if (wo.wheelchairAccessible) {
            if (!wheelchairAccessible) {
                return false;
            }
            if (maxSlope > wo.maxSlope) {
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

    public double computeWeight(State s0, TraverseOptions options, double time) {
        double weight;
        if (options.wheelchairAccessible) {
            // in fact, a wheelchair user will probably be going slower
            // than a cyclist, having less wind resistance, but will have
            // a stronger preference for less work. Maybe it
            // evens out?
            weight = slopeSpeedEffectiveLength / options.speed;
        } else if (options.getModes().contains(TraverseMode.BICYCLE)) {
            switch (options.optimize) {
            case SAFE:
                weight = bicycleSafetyEffectiveLength / options.speed;
                break;
            case GREENWAYS:
                weight = bicycleSafetyEffectiveLength / options.speed;
                if (bicycleSafetyEffectiveLength / length <= GREENWAY_SAFETY_FACTOR) {
                    // greenways are treated as even safer than they really are
                    weight *= 0.66;
                }
                break;
            case FLAT:
                weight = length / options.speed + slopeWorkCost;
                break;
            case QUICK:
                weight = slopeSpeedEffectiveLength / options.speed;
                break;
            case TRIANGLE:
                double quick = slopeSpeedEffectiveLength;
                double safety = bicycleSafetyEffectiveLength;
                double slope = slopeWorkCost;
                weight = quick * options.getTriangleTimeFactor() + slope
                        * options.getTriangleSlopeFactor() + safety
                        * options.getTriangleSafetyFactor();
                weight /= options.speed;
                break;
            default:
                weight = length / options.speed;
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
        this.slopeOverride = slopeOverride;
    }

    public void setBicycleSafetyEffectiveLength(double bicycleSafetyEffectiveLength) {
        this.bicycleSafetyEffectiveLength = bicycleSafetyEffectiveLength;
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
        return bicycleSafetyEffectiveLength;
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

    public void setCrossable(boolean crossable) {
        this.crossable = crossable;
    }

    public boolean isCrossable() {
        return crossable;
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
            return slopeSpeedEffectiveLength;
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
}
