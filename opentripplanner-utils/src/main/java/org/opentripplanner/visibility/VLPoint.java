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

import java.lang.Math;

public class VLPoint implements Comparable<VLPoint>, Cloneable {

    public double x, y;

    public VLPoint() {
        x = Double.NaN;
        y = Double.NaN;
    }

    public VLPoint(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public VLPoint(VLPoint p) {
        x = p.x;
        y = p.y;
    }

    public VLPoint projection_onto(LineSegment line_segment_temp) {

        if (line_segment_temp.size() == 1)
            return line_segment_temp.first();
        // The projection of point_temp onto the line determined by
        // line_segment_temp can be represented as an affine combination
        // expressed in the form projection of Point =
        // theta*line_segment_temp.first +
        // (1.0-theta)*line_segment_temp.second. if theta is outside
        // the interval [0,1], then one of the LineSegment's endpoints
        // must be closest to calling Point.
        double theta = ((line_segment_temp.second().x - x)
                * (line_segment_temp.second().x - line_segment_temp.first().x) + (line_segment_temp
                .second().y - y) * (line_segment_temp.second().y - line_segment_temp.first().y))
                / (Math.pow(line_segment_temp.second().x - line_segment_temp.first().x, 2) + Math
                        .pow(line_segment_temp.second().y - line_segment_temp.first().y, 2));
        // std::cout << "\E[1;37;40m" << "Theta is: " << theta << "\x1b[0m"
        // << std::endl;
        if ((0.0 <= theta) && (theta <= 1.0))
            return line_segment_temp.first().times(theta)
                    .plus(line_segment_temp.second().times(1.0 - theta));
        // Else pick closest endpoint.
        if (distance(line_segment_temp.first()) < distance(line_segment_temp.second()))
            return line_segment_temp.first();
        return line_segment_temp.second();
    }

    public VLPoint projection_onto(Ray ray_temp) {

        // Construct a LineSegment parallel with the Ray which is so long,
        // that the projection of the the calling Point onto that
        // LineSegment must be the same as the projection of the calling
        // Point onto the Ray.
        double R = distance(ray_temp.base_point());
        LineSegment seg_approx = new LineSegment(ray_temp.base_point(), ray_temp.base_point().plus(
                new VLPoint(R * Math.cos(ray_temp.bearing().get()), R
                        * Math.sin(ray_temp.bearing().get()))));
        return projection_onto(seg_approx);
    }

    public VLPoint projection_onto(Polyline polyline_temp) {

        VLPoint running_projection = polyline_temp.get(0);
        double running_min = distance(running_projection);
        VLPoint point_temp;
        for (int i = 0; i <= polyline_temp.size() - 1; i++) {
            point_temp = projection_onto(new LineSegment(polyline_temp.get(i),
                    polyline_temp.get(i + 1)));
            if (distance(point_temp) < running_min) {
                running_projection = point_temp;
                running_min = distance(running_projection);
            }
        }
        return running_projection;
    }

    public VLPoint projection_onto_vertices_of(VLPolygon polygon_temp) {
        VLPoint running_projection = polygon_temp.get(0);
        double running_min = distance(running_projection);
        for (int i = 1; i <= polygon_temp.n() - 1; i++) {
            if (distance(polygon_temp.get(i)) < running_min) {
                running_projection = polygon_temp.get(i);
                running_min = distance(running_projection);
            }
        }
        return running_projection;
    }

    public VLPoint projection_onto_vertices_of(Environment environment_temp) {
        VLPoint running_projection = projection_onto_vertices_of(environment_temp.outer_boundary);
        double running_min = distance(running_projection);
        VLPoint point_temp;
        for (int i = 0; i < environment_temp.h(); i++) {
            point_temp = projection_onto_vertices_of(environment_temp.holes.get(i));
            if (distance(point_temp) < running_min) {
                running_projection = point_temp;
                running_min = distance(running_projection);
            }
        }
        return running_projection;
    }

    public VLPoint projection_onto_boundary_of(VLPolygon polygon_temp) {

        VLPoint running_projection = polygon_temp.get(0);
        double running_min = distance(running_projection);
        VLPoint point_temp;
        for (int i = 0; i <= polygon_temp.n() - 1; i++) {
            point_temp = projection_onto(new LineSegment(polygon_temp.get(i),
                    polygon_temp.get(i + 1)));
            if (distance(point_temp) < running_min) {
                running_projection = point_temp;
                running_min = distance(running_projection);
            }
        }
        return running_projection;
    }

    public VLPoint projection_onto_boundary_of(Environment environment_temp) {

        VLPoint running_projection = projection_onto_boundary_of(environment_temp.outer_boundary);
        double running_min = distance(running_projection);
        VLPoint point_temp;
        for (int i = 0; i < environment_temp.h(); i++) {
            point_temp = projection_onto_boundary_of(environment_temp.holes.get(i));
            if (distance(point_temp) < running_min) {
                running_projection = point_temp;
                running_min = distance(running_projection);
            }
        }
        return running_projection;
    }

    public boolean on_boundary_of(VLPolygon polygon_temp, double epsilon) {

        if (distance(projection_onto_boundary_of(polygon_temp)) <= epsilon) {
            return true;
        }
        return false;
    }

    public boolean on_boundary_of(Environment environment_temp, double epsilon) {

        if (distance(projection_onto_boundary_of(environment_temp)) <= epsilon) {
            return true;
        }
        return false;
    }

    public boolean in(LineSegment line_segment_temp, double epsilon) {

        if (distance(line_segment_temp) < epsilon)
            return true;
        return false;
    }

    public boolean in_relative_interior_of(LineSegment line_segment_temp, double epsilon) {

        return in(line_segment_temp, epsilon) && distance(line_segment_temp.first()) > epsilon
                && distance(line_segment_temp.second()) > epsilon;
    }

    public void set_y(double y) {
        this.y = y;
    }

    public void set_x(double x) {
        this.x = x;
    }

    public boolean in(VLPolygon polygon_temp)

    {
        return in(polygon_temp, 0);
    }

    public boolean in(VLPolygon polygon_temp, double epsilon) {

        int n = polygon_temp.vertices.size();
        if (on_boundary_of(polygon_temp, epsilon))
            return true;
        // Then check the number of times a ray emanating from the Point
        // crosses the boundary of the Polygon. An odd number of
        // crossings indicates the Point is in the interior of the
        // Polygon. Based on
        // http://www.ecse.rpi.edu/Homepages/wrf/Research/Short_Notes/pnpoly.html

        boolean c = false;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            if ((((polygon_temp.get(i).y <= y) && (y < polygon_temp.get(j).y)) || ((polygon_temp
                    .get(j).y <= y) && (y < polygon_temp.get(i).y)))
                    && (x < (polygon_temp.get(j).x - polygon_temp.get(i).x)
                            * (y - polygon_temp.get(i).y)
                            / (polygon_temp.get(j).y - polygon_temp.get(i).y)
                            + polygon_temp.get(i).x))
                c = !c;
        }
        return c;
    }

    public boolean in(Environment environment_temp, double epsilon) {
        // On outer boundary?
        if (on_boundary_of(environment_temp, epsilon))
            return true;
        // Not in outer boundary?
        if (!in(environment_temp.outer_boundary, epsilon))
            return false;
        // In hole?
        for (int i = 0; i < environment_temp.h(); i++)
            if (in(environment_temp.holes.get(i)))
                return false;
        // Must be in interior.
        return true;
    }

    public boolean is_endpoint_of(LineSegment line_segment_temp, double epsilon) {

        if (distance(line_segment_temp.first()) <= epsilon
                || distance(line_segment_temp.second()) <= epsilon)
            return true;
        return false;
    }

    // these mean that points are not immutable (aieee!) - DT

    public void snap_to_vertices_of(VLPolygon polygon_temp, double epsilon) {

        VLPoint point_temp = new VLPoint(projection_onto_vertices_of(polygon_temp));
        if (distance(point_temp) <= epsilon) {
            x = point_temp.x;
            y = point_temp.y;
        }
    }

    public void snap_to_vertices_of(Environment environment_temp, double epsilon) {

        VLPoint point_temp = new VLPoint(projection_onto_vertices_of(environment_temp));
        if (distance(point_temp) <= epsilon) {
            x = point_temp.x;
            y = point_temp.y;
        }
    }

    public void snap_to_boundary_of(VLPolygon polygon_temp, double epsilon) {

        VLPoint point_temp = new VLPoint(projection_onto_boundary_of(polygon_temp));
        if (distance(point_temp) <= epsilon) {
            x = point_temp.x;
            y = point_temp.y;
        }
    }

    public void snap_to_boundary_of(Environment environment_temp, double epsilon) {

        VLPoint point_temp = new VLPoint(projection_onto_boundary_of(environment_temp));
        {
            if (distance(point_temp) <= epsilon) {
                x = point_temp.x;
                y = point_temp.y;
            }
        }
    }

    public boolean equals(Object o) {
        if (!(o instanceof VLPoint)) {
            return false;
        }
        VLPoint point2 = (VLPoint) o;
        return x == point2.x && y == point2.y;
    }

    public int compareTo(VLPoint point2) {

        if (x < point2.x)
            return -1;
        else if (x == point2.x) {
            if (y < point2.y) {
                return -1;
            } else if (y == point2.y) {
                return 0;
            }
            return 1;
        }
        return 1;
    }

    public VLPoint plus(VLPoint point2) {
        return new VLPoint(x + point2.x, y + point2.y);
    }

    public VLPoint minus(VLPoint point2) {
        return new VLPoint(x - point2.x, y - point2.y);
    }

    public VLPoint times(VLPoint point2) {
        return new VLPoint(x * point2.x, y * point2.y);
    }

    public VLPoint times(double scalar) {
        return new VLPoint(scalar * x, scalar * y);
    }

    public double cross(VLPoint point2) {

        // The area of the parallelogram created by the Points viewed as vectors.
        return x * point2.y - point2.x * y;
    }

    public double distance(VLPoint point2) {
        return Math.sqrt(Math.pow(x - point2.x, 2) + Math.pow(y - point2.y, 2));
    }

    public double distance(LineSegment line_segment_temp) {
        return distance(projection_onto(line_segment_temp));
    }

    public double distance(Ray ray_temp) {
        return distance(projection_onto(ray_temp));
    }

    public double distance(Polyline polyline_temp) {

        double running_min = distance(polyline_temp.get(0));
        double distance_temp;
        for (int i = 0; i < polyline_temp.size() - 1; i++) {
            distance_temp = distance(new LineSegment(polyline_temp.get(i), polyline_temp.get(i + 1)));
            if (distance_temp < running_min)
                running_min = distance_temp;
        }
        return running_min;
    }

    public double boundary_distance(VLPolygon polygon_temp) {

        double running_min = distance(polygon_temp.get(0));
        double distance_temp;
        for (int i = 0; i <= polygon_temp.n(); i++) {
            distance_temp = distance(new LineSegment(polygon_temp.get(i), polygon_temp.get(i + 1)));
            if (distance_temp < running_min)
                running_min = distance_temp;
        }
        return running_min;
    }

    public double boundary_distance(Environment environment_temp) {
        double running_min = distance(environment_temp.get(0).get(0));
        double distance_temp;
        for (int i = 0; i <= environment_temp.h(); i++) {
            distance_temp = boundary_distance(environment_temp.get(i));
            if (distance_temp < running_min)
                running_min = distance_temp;
        }
        return running_min;
    }

    public String toString() {
        return "\n" + x + ", " + y;
    }

    public VLPoint clone() {
        return new VLPoint(x, y);
    }

    public int hashCode() {
        return new Double(x).hashCode() + new Double(y).hashCode();
    }

}