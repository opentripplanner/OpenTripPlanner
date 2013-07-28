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

import java.util.ArrayList;
import java.util.List;

public class VLPolygon {

    public ArrayList<VLPoint> vertices;

    public VLPolygon() {

        vertices = new ArrayList<VLPoint>();
    }

    double boundary_distance(VLPoint point_temp) {
        return point_temp.boundary_distance(this);
    }

    double boundary_distance(LineSegment line_segment) {
        return line_segment.boundary_distance(this);
    }

    public VLPolygon(List<VLPoint> vertices_temp) {
        vertices = new ArrayList<VLPoint>(vertices_temp);
    }

    public int n() {
        return vertices.size();
    }

    public VLPolygon(VLPoint point0, VLPoint point1, VLPoint point2) {
        vertices = new ArrayList<VLPoint>();
        vertices.add(point0);
        vertices.add(point1);
        vertices.add(point2);
    }

    public int r() {
        int r_count = 0;
        if (vertices.size() > 1) {
            // Use cross product to count right turns.
            for (int i = 0; i <= n() - 1; i++)
                if ((get(i + 1).x - get(i).x) * (get(i + 2).y - get(i).y)

                - (get(i + 1).y - get(i).y) * (get(i + 2).x - get(i).x) < 0)
                    r_count++;
            if (area() < 0) {
                r_count = n() - r_count;
            }
        }
        return r_count;
    }

    public boolean is_simple(double epsilon) {

        if (n() == 0 || n() == 1 || n() == 2)
            return false;

        // Make sure adjacent edges only intersect at a single point.
        for (int i = 0; i <= n() - 1; i++)
            if (new LineSegment(get(i), get(i + 1)).intersection(
                    new LineSegment(get(i + 1), get(i + 2)), epsilon).size() > 1)
                return false;

        // Make sure nonadjacent edges do not intersect.
        for (int i = 0; i < n() - 2; i++)
            for (int j = i + 2; j <= n() - 1; j++)
                if (0 != (j + 1) % vertices.size()
                        && new LineSegment(get(i), get(i + 1)).distance(new LineSegment(get(j),
                                get(j + 1))) <= epsilon)
                    return false;

        return true;
    }

    public boolean is_in_standard_form() {
        if (vertices.size() > 1) // if more than one point in the polygon.
            for (int i = 1; i < vertices.size(); i++)
                if (vertices.get(0).compareTo(vertices.get(i)) > 0)
                    return false;
        return true;
    }

    public double boundary_length() {
        double length_temp = 0;
        if (n() == 0 || n() == 1)
            return 0;
        for (int i = 0; i < n() - 1; i++)
            length_temp += vertices.get(i).distance(vertices.get(i + 1));
        length_temp += vertices.get(n() - 1).distance(vertices.get(0));
        return length_temp;
    }

    public double area() {
        double area_temp = 0;
        if (n() == 0)
            return 0;
        for (int i = 0; i <= n() - 1; i++)
            area_temp += get(i).x * get(i + 1).y - get(i + 1).x * get(i).y;
        return area_temp / 2.0;
    }

    public VLPoint centroid() {
        assert (vertices.size() > 0);

        double area_temp = area();
        assert (area_temp != 0);
        double x_temp = 0;
        for (int i = 0; i <= n() - 1; i++)
            x_temp += (get(i).x + get(i + 1).x)
                    * (get(i).x * get(i + 1).y - get(i + 1).x * get(i).y);
        double y_temp = 0;
        for (int i = 0; i <= n() - 1; i++)
            y_temp += (get(i).y + get(i + 1).y)
                    * (get(i).x * get(i + 1).y - get(i + 1).x * get(i).y);
        return new VLPoint(x_temp / (6 * area_temp), y_temp / (6 * area_temp));
    }

    public double diameter() {
        // Precondition: nonempty Polygon.
        assert (n() > 0);

        double running_max = 0;
        for (int i = 0; i < n() - 1; i++) {
            for (int j = i + 1; j < n(); j++) {
                if (get(i).distance(get(j)) > running_max)
                    running_max = get(i).distance(get(j));
            }
        }
        return running_max;
    }

    public BoundingBox bbox() {
        // Precondition: nonempty Polygon.
        assert (vertices.size() > 0);

        BoundingBox bounding_box = new BoundingBox();
        double x_min = vertices.get(0).x, x_max = vertices.get(0).x, y_min = vertices.get(0).y, y_max = vertices
                .get(0).y;
        for (int i = 1; i < vertices.size(); i++) {
            if (x_min > vertices.get(i).x) {
                x_min = vertices.get(i).x;
            }
            if (x_max < vertices.get(i).x) {
                x_max = vertices.get(i).x;
            }
            if (y_min > vertices.get(i).y) {
                y_min = vertices.get(i).y;
            }
            if (y_max < vertices.get(i).y) {
                y_max = vertices.get(i).y;
            }
        }
        bounding_box.x_min = x_min;
        bounding_box.x_max = x_max;
        bounding_box.y_min = y_min;
        bounding_box.y_max = y_max;
        return bounding_box;
    }

    ArrayList<VLPoint> random_points(int count, double epsilon) {
        // Precondition: nonempty Polygon.
        assert (vertices.size() > 0);

        BoundingBox bounding_box = bbox();
        ArrayList<VLPoint> pts_in_polygon = new ArrayList<VLPoint>(count);
        VLPoint pt_temp = new VLPoint(
                Util.uniform_random_sample(bounding_box.x_min, bounding_box.x_max),
                Util.uniform_random_sample(bounding_box.y_min, bounding_box.y_max));
        while (pts_in_polygon.size() < count) {
            while (!pt_temp.in(this, epsilon)) {
                pt_temp.set_x(Util.uniform_random_sample(bounding_box.x_min, bounding_box.x_max));
                pt_temp.set_y(Util.uniform_random_sample(bounding_box.y_min, bounding_box.y_max));
            }
            pts_in_polygon.add(pt_temp);
            pt_temp.set_x(Util.uniform_random_sample(bounding_box.x_min, bounding_box.x_max));
            pt_temp.set_y(Util.uniform_random_sample(bounding_box.y_min, bounding_box.y_max));
        }
        return pts_in_polygon;
    }

    public void enforce_standard_form() {
        int point_count = vertices.size();
        if (point_count > 1) { // if more than one point in the polygon.
            ArrayList<VLPoint> vertices_temp = new ArrayList<VLPoint>(point_count);
            // Find index of lexicographically smallest point.
            int index_of_smallest = 0;
            int i; // counter.
            for (i = 1; i < point_count; i++)
                if (vertices.get(i).compareTo(vertices.get(index_of_smallest)) < 0)
                    index_of_smallest = i;
            // Fill vertices_temp starting with lex. smallest.
            for (i = index_of_smallest; i < point_count; i++)
                vertices_temp.add(vertices.get(i));
            for (i = 0; i < index_of_smallest; i++)
                vertices_temp.add(vertices.get(i));
            vertices = vertices_temp;
        }
    }

    public void eliminate_redundant_vertices(double epsilon) {
        // Degenerate case.
        if (vertices.size() < 4)
            return;

        // Store new minimal length list of vertices.
        ArrayList<VLPoint> vertices_temp = new ArrayList<VLPoint>(vertices.size());

        // Place holders.
        int first = 0;
        int second = 1;
        int third = 2;

        while (third <= vertices.size()) {
            // if second is redundant
            if (new LineSegment(get(first), get(third)).distance(get(second)) <= epsilon) {
                // =>skip it
                second = third;
                third++;
            }
            // else second not redundant
            else {
                // =>add it
                vertices_temp.add(get(second));
                first = second;
                second = third;
                third++;
            }
        }

        // decide whether to add original first point
        if (new LineSegment(vertices_temp.get(0), vertices_temp.get(vertices_temp.size() - 1))
                .distance(vertices.get(0)) > epsilon)
            vertices_temp.add(vertices.get(0));

        // Update list of vertices.
        vertices = vertices_temp;
    }

    public void reverse() {
        int n = n();
        if (n > 2) {
            // reverse, leaving the first point in its place
            for (int i = 1; i < (n+1) / 2; ++i) {
                VLPoint temp = vertices.get(i);
                vertices.set(i, vertices.get((n - i)));
                vertices.set((n - i), temp);
            }
        }
    }

    public boolean equals(Object o) {
        if (!(o instanceof VLPolygon)) {
            return false;
        }
        VLPolygon polygon2 = (VLPolygon) o;
        if (n() != polygon2.n() || n() == 0 || polygon2.n() == 0)
            return false;
        for (int i = 0; i < n(); i++)
            if (!get(i).equals(polygon2.get(i)))
                return false;
        return true;
    }

    public int hashCode() {
        return vertices.hashCode() + 1;
    }

    public VLPoint get(int i) {
        return vertices.get(i % vertices.size());
    }

    boolean equivalent(VLPolygon polygon2, double epsilon) {
        if (n() == 0 || polygon2.n() == 0)
            return false;
        if (n() != polygon2.n())
            return false;
        // Try all cyclic matches
        int n = n();// =polygon2.n()
        for (int offset = 0; offset < n; offset++) {
            boolean successful_match = true;
            for (int i = 0; i < n; i++) {
                if (get(i).distance(polygon2.get(i + offset)) > epsilon) {
                    successful_match = false;
                    break;
                }
            }
            if (successful_match)
                return true;
        }
        return false;
    }

    double boundary_distance(VLPolygon polygon2) {
        assert (n() > 0 && polygon2.n() > 0);

        // Handle single point degeneracy.
        if (n() == 1)
            return get(0).boundary_distance(polygon2);
        else if (polygon2.n() == 1)
            return polygon2.get(0).boundary_distance(this);
        // Handle cases where each polygon has at least 2 points.
        // Initialize to an upper bound.
        double running_min = get(0).boundary_distance(polygon2);
        double distance_temp;
        // Loop over all possible pairs of line segments.
        for (int i = 0; i <= n() - 1; i++) {
            for (int j = 0; j <= polygon2.n() - 1; j++) {
                distance_temp = new LineSegment(get(i), get(i + 1)).distance(new LineSegment(
                        polygon2.get(j), polygon2.get(j + 1)));
                if (distance_temp < running_min)
                    running_min = distance_temp;
            }
        }
        return running_min;
    }

    public String toString() {
        String outs = "";
        for (int i = 0; i < n(); i++)
            outs += get(i) + "\n";
        return outs;
    }

    public boolean hasPointInside(VLPolygon container) {
        for (VLPoint point : vertices) {
            if (point.in(container)) {
                return true;
            }
        }
        return false;
    }

}