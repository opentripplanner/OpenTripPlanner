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

import java.util.LinkedList;
import java.util.List;

import org.opentripplanner.common.geometry.PackedCoordinateSequence;
import org.opentripplanner.routing.core.AbstractEdge;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.TraverseResult;
import org.opentripplanner.routing.core.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

public class Street extends AbstractEdge implements WalkableEdge {

    private static Logger log = LoggerFactory.getLogger(Street.class);

    private static final long serialVersionUID = -3215764532108343102L;

    private static final String[] DIRECTIONS = { "north", "northeast", "east", "southeast",
            "south", "southwest", "west", "northwest" };

    String id;

    String name;

    LineString geometry;

    PackedCoordinateSequence elevationProfile;

    public double length;

    public StreetTraversalPermission permission;

    public boolean wheelchairAccessible;
    
    private boolean slopeOverride;

    /**
     * Streets with bike lanes are safer -- about twice as safe as streets without.
     * This is how long the street would have to be without bike lanes, to kill as many
     * people as it presently kills with bike lanes, statistically speaking.
     */

    public double bicycleSafetyEffectiveLength;

    private double slopeSpeedEffectiveLength;

    private double slopeCostEffectiveLength;

    private double maxSlope = 0;

    public Street(Vertex start, Vertex end, double length) {
        super(start, end);
        this.length = length;
        this.slopeSpeedEffectiveLength = length;
        this.bicycleSafetyEffectiveLength = length;
        this.permission = StreetTraversalPermission.ALL;
        this.wheelchairAccessible = true;
    }

    public Street(Vertex start, Vertex end, String id, String name, double length) {
        super(start, end);
        this.id = id;
        this.name = name;
        this.length = length;
        this.slopeSpeedEffectiveLength = length;
        this.bicycleSafetyEffectiveLength = length;
        this.permission = StreetTraversalPermission.ALL;
        this.wheelchairAccessible = true;
    }

    public Street(Vertex start, Vertex end, String id, String name, double length, StreetTraversalPermission permission) {
        super(start, end);
        this.id = id;
        this.name = name;
        this.length = length;
        this.slopeSpeedEffectiveLength = length;
        this.bicycleSafetyEffectiveLength = length;
        this.permission = permission;
        this.wheelchairAccessible = true;
    }


    public Street(Vertex start, Vertex end, String id, String name, double length, StreetTraversalPermission permission, boolean wheelchairAccessible) {
        super(start, end);
        this.id = id;
        this.name = name;
        this.length = length;
        this.slopeSpeedEffectiveLength = length;
        this.bicycleSafetyEffectiveLength = length;
        this.permission = permission;
        this.wheelchairAccessible = true;
    }

    public Street(Vertex start, Vertex end, String id, String name, double length, double bicycleSafetyEffectiveLength, StreetTraversalPermission permission) {
        super(start, end);
        this.id = id;
        this.name = name;
        this.length = length;
        this.slopeSpeedEffectiveLength = length;
        this.bicycleSafetyEffectiveLength = bicycleSafetyEffectiveLength;
        this.permission = permission;
        this.wheelchairAccessible = true;
    }

    public Street(Vertex start, Vertex end, String id, String name, double length, double bicycleSafetyEffectiveLength, StreetTraversalPermission permission, boolean wheelchairAccessible) {
        super(start, end);
        this.id = id;
        this.name = name;
        this.length = length;
        this.slopeSpeedEffectiveLength = length;
        this.bicycleSafetyEffectiveLength = bicycleSafetyEffectiveLength;
        this.permission = permission;
        this.wheelchairAccessible = wheelchairAccessible;
    }

    public void setGeometry(LineString g) {
        geometry = g;
    }

    public void setElevationProfile(PackedCoordinateSequence elev) {
        if (slopeOverride) {
            elev = new PackedCoordinateSequence.Float(new Coordinate[] {new Coordinate(0f, 0f)}, 2);
        }
        elevationProfile = elev;
        //compute the cost of the elevation changes
        Coordinate[] coordinates = elevationProfile.toCoordinateArray();
        slopeSpeedEffectiveLength = 0;
        slopeCostEffectiveLength = 0;
        for (int i = 0; i < coordinates.length - 1; ++i) {
            double run = coordinates[i + 1].x - coordinates[i].x;
            double rise = coordinates[i + 1].y - coordinates[i].y;
            double slope = rise / run;
            if (slope > 0.35 || slope < -0.35) {
                slope = 0; //Baldwin St in Dunedin, NZ, is the steepest street on earth, and has a grade of 35%.  So, this must be a data error.
                log.warn("Warning: street " + this + " steeper than Baldwin Street.  This is an error in the algorithm or the data");
            }
            if (maxSlope < Math.abs(slope)) {
                maxSlope  = Math.abs(slope);
            }
            slopeCostEffectiveLength += run * (1 + slope * slope * 10); //any slope is bad
            slopeSpeedEffectiveLength += run * slopeSpeedCoefficient(slope, coordinates[i].y);
        }
    }
    
    public static double slopeSpeedCoefficient (double slope, double altitude) {
        /* computed by asking ZunZun for a quadratic b-spline approximating some values from
         * http://www.analyticcycling.com/ForcesSpeed_Page.html
         * fixme: should clamp to local speed limits
         * (code is from ZunZun)
         */
        double tx [] = {0.0000000000000000E+00, 0.0000000000000000E+00, 0.0000000000000000E+00, 2.7987785324442748E+03, 5.0000000000000000E+03, 5.0000000000000000E+03, 5.0000000000000000E+03};
        double ty [] = {-3.4999999999999998E-01, -3.4999999999999998E-01, -3.4999999999999998E-01, -7.2695627831828688E-02, -2.4945814335295903E-03, 5.3500304527448035E-02, 1.2191105175593375E-01, 3.4999999999999998E-01, 3.4999999999999998E-01, 3.4999999999999998E-01};
        double coeff [] = {4.3843513168660255E+00, 3.6904323727375652E+00, 1.6791850199667697E+00, 5.5077866957024113E-01, 1.7977766419113900E-01, 8.0906832222762959E-02, 6.0239305785343762E-02, 4.6782343053423814E+00, 3.9250580214736304E+00, 1.7924585866601270E+00, 5.3426170441723031E-01, 1.8787442260720733E-01, 7.4706427576152687E-02, 6.2201805553147201E-02, 5.3131908923568787E+00, 4.4703901299120750E+00, 2.0085381385545351E+00, 5.4611063530784010E-01, 1.8034042959223889E-01, 8.1456939988273691E-02, 5.9806795955995307E-02, 5.6384893192212662E+00, 4.7732222200176633E+00, 2.1021485412233019E+00, 5.7862890496126462E-01, 1.6358571778476885E-01, 9.4846184210137130E-02, 5.5464612133430242E-02};
        int nx = 7;
        int ny = 10;
        int kx = 2;
        int ky = 2;

        double h [] = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
        double hh [] = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
        double w_x [] = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
        double w_y [] = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};

        int i, j, li, lj, lx, ky1, nky1, ly, i1, j1, l2;
        double f, temp;

        int kx1 = kx+1;
        int nkx1 = nx-kx1;
        int l = kx1;
        int l1 = l+1;

        while ((altitude >= tx[l1-1]) && (l != nkx1)) {
            l = l1;
            l1 = l+1;
        }

        h[0] = 1.0;
        for (j = 1; j < kx+1; j++)
        {
            for (i = 0; i < j; i++)
            {
                hh[i] = h[i];
            }
            h[0] = 0.0;
            for (i = 0; i < j; i++)
            {
                li = l+i;
                lj = li-j;
                if (tx[li] != tx[lj])
                {
                    f = hh[i] / (tx[li] - tx[lj]);
                    h[i] = h[i] + f * (tx[li] - altitude);
                    h[i+1] = f * (altitude - tx[lj]);
                } else {
                    h[i+1-1] = 0.0;
                }
            }
        }

        lx = l-kx1;
        for (j = 0; j < kx1; j++) {
            w_x[j] = h[j];
        }

        ky1 = ky+1;
        nky1 = ny-ky1;
        l = ky1;
        l1 = l+1;

        while ((slope >= ty[l1-1]) && (l != nky1)) {
            l = l1;
            l1 = l+1;
        }

        h[0] = 1.0;
        for (j = 1; j < ky+1; j++) {
            for (i = 0; i < j; i++) {
                hh[i] = h[i];
            }
            h[0] = 0.0;
            for (i = 0; i < j; i++) {
                li = l+i;
                lj = li-j;
                if (ty[li] != ty[lj]) {
                    f = hh[i] / (ty[li] - ty[lj]);
                    h[i] = h[i] + f * (ty[li] - slope);
                    h[i+1] = f * (slope - ty[lj]);
                } else {
                    h[i+1-1] = 0.0;
                }
            }
        }

        ly = l-ky1;
        for (j = 0; j < ky1; j++) {
            w_y[j] = h[j];
        }

        l = lx*nky1;
        for (i1 = 0; i1 < kx1; i1++) {
            h[i1] = w_x[i1];
        }

        l1 = l+ly;
        temp = 0.0;
        for (i1 = 0; i1 < kx1; i1++) {
            l2 = l1;
            for (j1 = 0; j1 < ky1; j1++)
            {
                l2 = l2+1;
                temp = temp + coeff[l2-1] * h[i1] * w_y[j1];
            }
            l1 = l1+nky1;
        }

        return temp;
    }

    public TraverseResult traverse(State s0, TraverseOptions wo) {
        if (!canTraverse(wo)) {
            return null;
        }
        
        State s1 = s0.clone();
        double time = this.length / wo.speed;
        double weight = computeWeight(s0, wo, time);
        s1.walkDistance += length;
        // it takes time to walk/bike along a street, so update state accordingly
        s1.incrementTimeInSeconds((int) time);
        return new TraverseResult(weight, s1);
    }

    private double computeWeight(State s0, TraverseOptions wo, double time) {
        double weight;
        if (wo.wheelchairAccessible) {
            //in fact, a wheelchair user will probably be going slower
            //than a cyclist, having less wind resistance, but will have 
            //a stronger preference for less work.  Maybe it
            //evens out?
            weight = slopeSpeedEffectiveLength / wo.speed; 
        } else if (wo.modes.contains(TraverseMode.BICYCLE))
            switch (wo.optimizeFor) {
            case SAFE:
                weight = bicycleSafetyEffectiveLength / wo.speed;
            break;
            case FLAT:
                weight = slopeCostEffectiveLength;
                break;
            case QUICK:
                weight = slopeSpeedEffectiveLength / wo.speed;
                break;
            default:
                //TODO: greenways
                weight = length / wo.speed;
        } else {
            weight = time;
        }
        if (s0.walkDistance > wo.maxWalkDistance && wo.modes.getTransit()) {
            weight *= 100;
        }
        return weight;
    }

    public TraverseResult traverseBack(State s0, TraverseOptions wo) {
        if (!canTraverse(wo)) {
            return null;
        }
        State s1 = s0.clone();
        double time = length / wo.speed;
        double weight;
        weight = computeWeight(s0, wo, time);
        s1.walkDistance += this.length;
        // time moves *backwards* when traversing an edge in the opposite direction
        s1.incrementTimeInSeconds(-(int) time);
        return new TraverseResult(weight, s1);
    }

    private boolean canTraverse(TraverseOptions wo) {
        if (wo.wheelchairAccessible) {
            if(!wheelchairAccessible) {
                return false;
            }
            if (maxSlope > wo.maxSlope) {
                return false;
            }
        }

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
            return "Street(" + this.id + ", " + this.name + ", " + this.length + ", " + this.permission + ")";
        } else {
            return "Street(" + this.length + ", " + this.permission + ")";
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
        if(elevationProfile == null) return null;
        List<Coordinate> coordList = new LinkedList<Coordinate>();
        
        if(start < 0) start = 0;
        if(end > length) end = length;
        
        for(Coordinate coord : elevationProfile.toCoordinateArray()) {
            if(coord.x >= start && coord.x <= end) {
                coordList.add(new Coordinate(coord.x-start, coord.y));
            }
        }
        
        Coordinate coordArr[] = new Coordinate[coordList.size()];
        return new PackedCoordinateSequence.Double(coordList.toArray(coordArr));
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

    public StreetTraversalPermission getTraversalPermission() {
        return permission;
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

    /**
     * Override slope calculation to always return 0.  This is useful when the street is a 
     * flat bridge over sloping terrain
     * @param slopeOverride the slopeOverride to set
     */
    public void setSlopeOverride(boolean slopeOverride) {
        this.slopeOverride = slopeOverride;
    }

    /**
     * @see{setSlopeOVerride}
     */
    public boolean getSlopeOverride() {
        return slopeOverride;
    }

}
