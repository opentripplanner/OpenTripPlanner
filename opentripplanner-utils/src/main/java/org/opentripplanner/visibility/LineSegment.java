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

public class LineSegment {

    VLPoint[] endpoints;

    double distance(VLPoint point_temp) {
        return point_temp.distance(this);
    }

    LineSegment() {
        endpoints = null;
    }

    public int size() {
        if (endpoints == null) {
            return 0;
        } else {
            return endpoints.length;
        }
    }

    LineSegment(LineSegment line_segment_temp) {
        switch (line_segment_temp.size()) {
        case 0:
            endpoints = null;
            break;
        case 1:
            endpoints = new VLPoint[1];
            endpoints[0] = line_segment_temp.endpoints[0].clone();
            break;
        case 2:
            endpoints = new VLPoint[2];
            endpoints[0] = line_segment_temp.endpoints[0].clone();
            endpoints[1] = line_segment_temp.endpoints[1].clone();
        }
    }

    LineSegment(VLPoint point_temp) {
        endpoints = new VLPoint[1];
        endpoints[0] = point_temp;
    }

    LineSegment(VLPoint first_point_temp, VLPoint second_point_temp) {
        this(first_point_temp, second_point_temp, 0);
    }

    LineSegment(VLPoint first_point_temp, VLPoint second_point_temp, double epsilon) {
        if (first_point_temp.distance(second_point_temp) <= epsilon) {
            endpoints = new VLPoint[1];
            endpoints[0] = first_point_temp;
        } else {
            endpoints = new VLPoint[2];
            endpoints[0] = first_point_temp;
            endpoints[1] = second_point_temp;
        }
    }

    VLPoint first() {
        assert (size() > 0);

        return endpoints[0];
    }

    VLPoint second() {
        assert (size() > 0);

        if (size() == 2)
            return endpoints[1];
        else
            return endpoints[0];
    }

    VLPoint midpoint() {
        assert (size() > 0);

        return first().plus(second()).times(0.5);
    }

    double length() {
        assert (size() > 0);

        return first().distance(second());
    }

    boolean is_in_standard_form() {
        assert (size() > 0);

        if (size() < 2)
            return true;
        return first().compareTo(second()) <= 0;
    }

    /*
     * assignment operator -- need to audit what's going on with this -DMT LineSegment& operator =
     * (const LineSegment& line_segment_temp) { //Makes sure not to delete dynamic vars before
     * they're copied. if(this==&line_segment_temp) return *this; delete [] endpoints;
     * switch(line_segment_temp.size_){ case 0: endpoints = null; size_ = 0; break; case 1:
     * endpoints = new Point[1]; endpoints[0] = line_segment_temp.endpoints[0]; size_ = 1; break;
     * case 2: endpoints = new Point[2]; endpoints[0] = line_segment_temp.endpoints[0]; endpoints[1]
     * = line_segment_temp.endpoints[1]; size_ = 2; } return *this; }
     */

    void set_first(VLPoint point_temp, double epsilon) {
        VLPoint second_point_temp;
        switch (size()) {
        case 0:
            endpoints = new VLPoint[1];
            endpoints[0] = point_temp;
            break;
        case 1:
            if (endpoints[0].distance(point_temp) <= epsilon) {
                endpoints[0] = point_temp;
                return;
            }
            second_point_temp = endpoints[0];
            endpoints = new VLPoint[2];
            endpoints[0] = point_temp;
            endpoints[1] = second_point_temp;
            break;
        case 2:
            if (point_temp.distance(endpoints[1]) > epsilon) {
                endpoints[0] = point_temp;
                return;
            }
            endpoints = new VLPoint[1];
            endpoints[0] = point_temp;
        }
    }

    void set_second(VLPoint point_temp, double epsilon) {
        VLPoint first_point_temp;
        switch (size()) {
        case 0:
            endpoints = new VLPoint[1];
            endpoints[0] = point_temp;
            break;
        case 1:
            if (endpoints[0].distance(point_temp) <= epsilon) {
                endpoints[0] = point_temp;
                return;
            }
            first_point_temp = endpoints[0];
            endpoints = new VLPoint[2];
            endpoints[0] = first_point_temp;
            endpoints[1] = point_temp;
            break;
        case 2:
            if (endpoints[0].distance(point_temp) > epsilon) {
                endpoints[1] = point_temp;
                return;
            }
            endpoints = new VLPoint[1];
            endpoints[0] = point_temp;
            break;
        }
    }

    void reverse() {
        if (size() < 2)
            return;
        VLPoint point_temp = endpoints[0];
        endpoints[0] = endpoints[1];
        endpoints[1] = point_temp;
    }

    void enforce_standard_form() {
        if (first().compareTo(second()) > 0)
            reverse();
    }

    void clear() {
        endpoints = null;
    }

    public boolean equals(Object o) {
        if (!(o instanceof LineSegment)) {
            return false;
        }
        LineSegment line_segment2 = (LineSegment) o;
        if (size() != line_segment2.size() || size() == 0 || line_segment2.size() == 0)
            return false;
        else
            return (first().equals(line_segment2.first()) && second()
                    .equals(line_segment2.second()));
    }

    boolean equivalent(LineSegment line_segment2, double epsilon) {
        if (size() != line_segment2.size() || size() == 0 || line_segment2.size() == 0)
            return false;
        else
            return (first().distance(line_segment2.first()) <= epsilon && second().distance(
                    line_segment2.second()) <= epsilon)
                    || (first().distance(line_segment2.second()) <= epsilon && second().distance(
                            line_segment2.first()) <= epsilon);
    }

    double distance(LineSegment line_segment2) {
        assert (size() > 0 && line_segment2.size() > 0);

        if (intersect_proper(line_segment2))
            return 0;
        // But if two line segments intersect improperly, the distance
        // between them is equal to the minimum of the distances between
        // all 4 endpoints and their respective projections onto the line
        // segment they don't belong to.
        double running_min, distance_temp;
        running_min = first().distance(line_segment2);
        distance_temp = second().distance(line_segment2);
        if (distance_temp < running_min)
            running_min = distance_temp;
        distance_temp = line_segment2.first().distance(this);
        if (distance_temp < running_min)
            running_min = distance_temp;
        distance_temp = line_segment2.second().distance(this);
        if (distance_temp < running_min)
            return distance_temp;
        return running_min;
    }

    double boundary_distance(VLPolygon polygon) {
        assert (size() > 0 && polygon.n() > 0);

        double running_min = distance(polygon.get(0));
        if (polygon.n() > 1)
            for (int i = 0; i < polygon.n(); i++) {
                double d = distance(new LineSegment(polygon.get(i), polygon.get(i + 1)));
                if (running_min > d)
                    running_min = d;
            }
        return running_min;
    }

    boolean intersect(LineSegment line_segment2, double epsilon) {
        if (size() == 0 || line_segment2.size() == 0)
            return false;
        if (distance(line_segment2) <= epsilon)
            return true;
        return false;
    }

    boolean intersect_proper(LineSegment line_segment2) {
        return intersect_proper(line_segment2, 0);
    }

    boolean intersect_proper(LineSegment line_segment2, double epsilon) {
        if (size() == 0 || line_segment2.size() == 0)
            return false;

        // Declare new vars just for readability.
        VLPoint a = new VLPoint(first());
        VLPoint b = new VLPoint(second());
        VLPoint c = new VLPoint(line_segment2.first());
        VLPoint d = new VLPoint(line_segment2.second());
        // First find the minimum of the distances between all 4 endpoints
        // and their respective projections onto the opposite line segment.
        double running_min, distance_temp;
        running_min = a.distance(line_segment2);
        distance_temp = b.distance(line_segment2);
        if (distance_temp < running_min)
            running_min = distance_temp;
        distance_temp = c.distance(this);
        if (distance_temp < running_min)
            running_min = distance_temp;
        distance_temp = d.distance(this);
        if (distance_temp < running_min)
            running_min = distance_temp;
        // If an endpoint is close enough to the other segment, the
        // intersection is not considered proper.
        if (running_min <= epsilon)
            return false;
        // This test is from O'Rourke's "Computational Geometry in C",
        // p.30. Checks left and right turns.
        if (b.minus(a).cross(c.minus(b)) * b.minus(a).cross(d.minus(b)) < 0
                && d.minus(c).cross(b.minus(d)) * d.minus(c).cross(a.minus(d)) < 0)
            return true;
        return false;
    }

    LineSegment intersection(LineSegment line_segment2, double epsilon) {
        // Initially empty.
        LineSegment line_segment_temp = new LineSegment();

        if (size() == 0 || line_segment2.size() == 0)
            return line_segment_temp;

        // No intersection => return empty segment.
        if (!intersect(line_segment2, epsilon))
            return line_segment_temp;
        // Declare new vars just for readability.
        VLPoint a = new VLPoint(first());
        VLPoint b = new VLPoint(second());
        VLPoint c = new VLPoint(line_segment2.first());
        VLPoint d = new VLPoint(line_segment2.second());
        if (intersect_proper(line_segment2, epsilon)) {
            // Use formula from O'Rourke's "Computational Geometry in C", p. 221.
            // Note D=0 iff the line segments are parallel.
            double D = a.x * (d.y - c.y) + b.x * (c.y - d.y) + d.x * (b.y - a.y) + c.x
                    * (a.y - b.y);
            double s = (a.x * (d.y - c.y) + c.x * (a.y - d.y) + d.x * (c.y - a.y)) / D;
            line_segment_temp.set_first(a.plus(b.minus(a).times(s)), epsilon);
            return line_segment_temp;
        }
        // Otherwise if improper...
        double distance_temp_a = a.distance(line_segment2);
        double distance_temp_b = b.distance(line_segment2);
        double distance_temp_c = c.distance(this);
        double distance_temp_d = d.distance(this);
        // Check if the intersection is nondegenerate segment.
        if (distance_temp_a <= epsilon && distance_temp_b <= epsilon) {
            line_segment_temp.set_first(a, epsilon);
            line_segment_temp.set_second(b, epsilon);
            return line_segment_temp;
        } else if (distance_temp_c <= epsilon && distance_temp_d <= epsilon) {
            line_segment_temp.set_first(c, epsilon);
            line_segment_temp.set_second(d, epsilon);
            return line_segment_temp;
        } else if (distance_temp_a <= epsilon && distance_temp_c <= epsilon) {
            line_segment_temp.set_first(a, epsilon);
            line_segment_temp.set_second(c, epsilon);
            return line_segment_temp;
        } else if (distance_temp_a <= epsilon && distance_temp_d <= epsilon) {
            line_segment_temp.set_first(a, epsilon);
            line_segment_temp.set_second(d, epsilon);
            return line_segment_temp;
        } else if (distance_temp_b <= epsilon && distance_temp_c <= epsilon) {
            line_segment_temp.set_first(b, epsilon);
            line_segment_temp.set_second(c, epsilon);
            return line_segment_temp;
        } else if (distance_temp_b <= epsilon && distance_temp_d <= epsilon) {
            line_segment_temp.set_first(b, epsilon);
            line_segment_temp.set_second(d, epsilon);
            return line_segment_temp;
        }
        // Check if the intersection is a single point.
        else if (distance_temp_a <= epsilon) {
            line_segment_temp.set_first(a, epsilon);
            return line_segment_temp;
        } else if (distance_temp_b <= epsilon) {
            line_segment_temp.set_first(b, epsilon);
            return line_segment_temp;
        } else if (distance_temp_c <= epsilon) {
            line_segment_temp.set_first(c, epsilon);
            return line_segment_temp;
        } else if (distance_temp_d <= epsilon) {
            line_segment_temp.set_first(d, epsilon);
            return line_segment_temp;
        }
        return line_segment_temp;
    }

    LineSegment intersection(Ray ray_temp, double epsilon) {
        return ray_temp.intersection(this, epsilon);
    }

    LineSegment intersection(Ray ray_temp) {
        return ray_temp.intersection(this, 0);
    }

    public String toString() {
        switch (size()) {
        case 0:
            return "";
        case 1:
        case 2:
            return first() + "\n" + second() + "\n";
        default:
            throw new IllegalArgumentException();
        }
    }

}