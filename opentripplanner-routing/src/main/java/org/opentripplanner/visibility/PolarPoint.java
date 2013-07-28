/**
 Ported by David Turner from Visilibity, by Karl J. Obermeyer

 This port undoubtedly introduced a number of bugs (and removed some features).
 
 Bug reports should be directed to the OpenTripPlanner project, unless they 
 can be reproduced in the original VisiLibity.
  
 This program is free software: you can redistribute it and/or modify it under
 the terms of the GNU Lesser General Public License as published by the
 Free Software Foundation, either version 3 of the License, or (at your
 option) any later version.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
 License for more details.

 You should have received a copy of the GNU Lesser General Public
 License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.opentripplanner.visibility;

class PolarPoint extends VLPoint implements Comparable<VLPoint> {

    VLPoint polar_origin;

    // Polar coordinates where radius always positive, and angle
    // measured ccw from the world coordinate system's x-axis.
    double range = Double.NaN;

    Angle bearing = new Angle(Double.NaN);

    public PolarPoint(VLPoint polar_origin_temp, VLPoint point_temp) {
        this(polar_origin_temp, point_temp, 0);
    }

    public PolarPoint(VLPoint polar_origin_temp, VLPoint point_temp, double epsilon) {
        super(point_temp);
        polar_origin = polar_origin_temp.clone();
        if (polar_origin.distance(point_temp) <= epsilon) {
            bearing = new Angle(0.0);
            range = 0.0;
        } else {
            bearing = new Angle(point_temp.y - polar_origin_temp.y, point_temp.x
                    - polar_origin_temp.x);
            range = polar_origin_temp.distance(point_temp);
        }
    }

    public void set_polar_origin(VLPoint polar_origin_temp) {
        PolarPoint newPoint = new PolarPoint(polar_origin_temp, new VLPoint(x, y));
        setFromPolarPoint(newPoint);
    }

    void setFromPolarPoint(PolarPoint newPoint) {
        this.polar_origin = newPoint.polar_origin.clone();
        this.range = newPoint.range;
        this.bearing = newPoint.bearing.clone();
        this.x = newPoint.x;
        this.y = newPoint.y;
    }

    public PolarPoint clone() {
        PolarPoint clone = new PolarPoint();
        clone.setFromPolarPoint(this);
        return clone;
    }

    public void set_x(double x_temp) {
        PolarPoint newPoint = new PolarPoint(polar_origin, new VLPoint(x_temp, y));
        setFromPolarPoint(newPoint);
    }

    public void set_y(double y_temp) {
        PolarPoint newPoint = new PolarPoint(polar_origin, new VLPoint(x, y_temp));
        setFromPolarPoint(newPoint);
    }

    public void set_range(double range_temp) {
        range = range_temp;
        x = polar_origin.x + range * Math.cos(bearing.get());
        y = polar_origin.y + range * Math.sin(bearing.get());
    }

    public void set_bearing(Angle bearing_temp) {
        bearing = bearing_temp.clone();
        x = polar_origin.x + range * Math.cos(bearing.get());
        y = polar_origin.y + range * Math.sin(bearing.get());
    }

    public boolean equals(Object o) {
        if (!(o instanceof PolarPoint)) {
            return false;
        }
        PolarPoint polar_point2 = (PolarPoint) o;
        return polar_origin.equals(polar_point2.polar_origin) && range == polar_point2.range
                && bearing.equals(polar_point2.bearing);
    }

    public int compareTo(VLPoint point2) {
        if (!(point2 instanceof PolarPoint)) {
            return super.compareTo(point2);
        }
        PolarPoint polar_point2 = (PolarPoint) point2;
        int bearingComp = bearing.compareTo(polar_point2.bearing);
        if (bearingComp == 0) {
            return (int) Math.signum(range - polar_point2.range);
        }
        return bearingComp;

    }

    void set_bearing_to_2pi() {
        bearing.set_to_2pi();
    }

    public PolarPoint() {
        super();
        range = Double.NaN;
        bearing = new Angle(Double.NaN);
    }

    public String toString() {
        return "PolarPoint(" + bearing + "  " + range + ") of " + super.toString();
    }

}