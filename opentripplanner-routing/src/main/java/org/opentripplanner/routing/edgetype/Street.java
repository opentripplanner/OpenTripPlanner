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

import org.opentripplanner.routing.core.AbstractEdge;
import org.opentripplanner.routing.core.OptimizeType;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.TraverseResult;
import org.opentripplanner.routing.core.Vertex;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

public class Street extends AbstractEdge implements WalkableEdge {

    private static final long serialVersionUID = -3215764532108343102L;

    private static final String[] DIRECTIONS = { "north", "northeast", "east", "southeast",
            "south", "southwest", "west", "northwest" };

    String id;

    String name;

    LineString geometry;

    public double length;

    public StreetTraversalPermission permission;

    public boolean wheelchairAccessible;

    /**
     * Streets with bike lanes are safer -- about twice as safe as streets without.
     * This is how long the street would have to be without bike lanes, to kill as many
     * people as it presently kills with bike lanes, statistically speaking.
     */

    public double bicycleSafetyEffectiveLength;

    public Street(Vertex start, Vertex end, double length) {
        super(start, end);
        this.length = length;
        this.permission = StreetTraversalPermission.ALL;
        this.wheelchairAccessible = true;
    }

    public Street(Vertex start, Vertex end, String id, String name, double length) {
        super(start, end);
        this.id = id;
        this.name = name;
        this.length = length;
        this.permission = StreetTraversalPermission.ALL;
        this.wheelchairAccessible = true;
    }

    public Street(Vertex start, Vertex end, String id, String name, double length, StreetTraversalPermission permission) {
        super(start, end);
        this.id = id;
        this.name = name;
        this.length = length;
        this.permission = permission;
        this.wheelchairAccessible = true;
    }


    public Street(Vertex start, Vertex end, String id, String name, double length, StreetTraversalPermission permission, boolean wheelchairAccessible) {
        super(start, end);
        this.id = id;
        this.name = name;
        this.length = length;
        this.permission = permission;
        this.wheelchairAccessible = true;
    }

    public Street(Vertex start, Vertex end, String id, String name, double length, double bicycleSafetyEffectiveLength, StreetTraversalPermission permission) {
        super(start, end);
        this.id = id;
        this.name = name;
        this.length = length;
        this.bicycleSafetyEffectiveLength = bicycleSafetyEffectiveLength;
        this.permission = permission;
        this.wheelchairAccessible = true;
    }

    public void setGeometry(LineString g) {
        geometry = g;
    }

    public TraverseResult traverse(State s0, TraverseOptions wo) {
        if (!canTraverse(wo)) {
            return null;
        }
        State s1 = s0.clone();
        double time = this.length / wo.speed;
        double weight;
        if (wo.modes.contains(TraverseMode.BICYCLE) && wo.optimizeFor.equals(OptimizeType.SAFE)) {
            weight = bicycleSafetyEffectiveLength / wo.speed;
        } else {
            weight = time;
        }
        if (s0.walkDistance > wo.maxWalkDistance && wo.modes.getTransit()) {
            weight *= 100;
        }
        s1.walkDistance += length;
        // it takes time to walk/bike along a street, so update state accordingly
        s1.incrementTimeInSeconds((int) time);
        return new TraverseResult(weight, s1);
    }

    public TraverseResult traverseBack(State s0, TraverseOptions wo) {
        if (!canTraverse(wo)) {
            return null;
        }
        State s1 = s0.clone();
        double time = length / wo.speed;
        double weight;
        if (wo.modes.contains(TraverseMode.BICYCLE) && wo.optimizeFor.equals(OptimizeType.SAFE)) {
            if (bicycleSafetyEffectiveLength < length * 0.99) {
                System.out.println ("a shorter path on : " + getName() + ": " + bicycleSafetyEffectiveLength + " / " + length);
            }
            weight = bicycleSafetyEffectiveLength / wo.speed;
        } else {
            weight = time;
        }
        if (s0.walkDistance > wo.maxWalkDistance && wo.modes.getTransit()) {
            weight *= 100;
        }
        s1.walkDistance += this.length;
        // time moves *backwards* when traversing an edge in the opposite direction
        s1.incrementTimeInSeconds(-(int) time);
        return new TraverseResult(weight, s1);
    }

    private boolean canTraverse(TraverseOptions wo) {
        if(!wheelchairAccessible && wo.wheelchairAccessible)
            return false;

        if(wo.modes.getWalk() && permission.allows(StreetTraversalPermission.PEDESTRIAN))
            return true;

        if(wo.modes.getBicycle() && permission.allows(StreetTraversalPermission.BICYCLE))
            return true;

        if(wo.modes.getCar() && permission.allows(StreetTraversalPermission.CAR))
            return true;

        return false;
    }

    public String toString() {
        if (this.name != null) {
            return "Street(" + this.id + ", " + this.name + ", " + this.length + ")";
        } else {
            return "Street(" + this.length + ")";
        }
    }

    public String getDirection() {
        Coordinate[] coordinates = geometry.getCoordinates();
        return getDirection(coordinates[0], coordinates[coordinates.length - 1]);
    }

    private static String getDirection(Coordinate a, Coordinate b) {
        double run = b.x - a.x;
        double rise = b.y - a.y;
        double direction = Math.atan2(run, rise);
        int octant = (int) (8 + Math.round(direction * 8 / (Math.PI * 2))) % 8;

        return DIRECTIONS[octant];
    }

    public static String computeDirection(Point startPoint, Point endPoint) {
        return getDirection(startPoint.getCoordinate(), endPoint.getCoordinate());
    }

    public double getDistance() {
        return length;
    }

    public LineString getGeometry() {
        return geometry;
    }

    public TraverseMode getMode() {
        // this is actually WALK or BICYCLE depending on the TraverseOptions
        return TraverseMode.WALK;
    }

    public String getName() {
        return name;
    }

    public void setTraversalPermission(StreetTraversalPermission permission) {
        this.permission = permission;
    }

    public void setBicycleSafetyEffectiveLength(double bicycleSafetyEffectiveLength) {
        this.bicycleSafetyEffectiveLength = bicycleSafetyEffectiveLength;
    }

    public double getBicycleSafetyEffectiveLength() {
        return bicycleSafetyEffectiveLength;
    }

	public void setWheelchairAccessible(boolean wheelchairAccessible) {
		this.wheelchairAccessible = wheelchairAccessible;
	}

	public boolean getWheelchairAccessible() {
		return wheelchairAccessible;
	}

    public double getLength() {
        return length;
    }

}
