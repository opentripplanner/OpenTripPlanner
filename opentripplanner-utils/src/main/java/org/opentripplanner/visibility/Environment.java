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

/**
 * \brief environment represented by simple polygonal outer boundary with simple polygonal holes
 * 
 * \remarks For methods to work correctly, the outer boundary vertices must be listed ccw and the
 * hole vertices cw
 */
public class Environment {

    VLPolygon outer_boundary;

    ArrayList<VLPolygon> holes = new ArrayList<VLPolygon>();

    ArrayList<pair<Integer, Integer>> flattened_index_key = new ArrayList<pair<Integer, Integer>>();

    public Environment(List<VLPolygon> polygons) {
        outer_boundary = polygons.get(0);
        for (int i = 1; i < polygons.size(); i++)
            holes.add(polygons.get(i));
        update_flattened_index_key();
    }

    public Environment(VLPolygon polygon_temp) {
        outer_boundary = polygon_temp;
        update_flattened_index_key();
    }

    VLPoint kth_point(int k) {
        pair<Integer, Integer> ij = flattened_index_key.get(k);
        return get(ij.first()).get(ij.second());
    }

    int n() {
        int n_count = 0;
        n_count = outer_boundary.n();
        for (int i = 0; i < h(); i++)
            n_count += holes.get(i).n();
        return n_count;
    }

    int r() {
        int r_count = 0;
        r_count = outer_boundary.r();
        for (int i = 0; i < h(); i++) {
            VLPolygon polygon_temp = holes.get(i);
            r_count += polygon_temp.n() - polygon_temp.r();
        }
        return r_count;
    }

    int h() {
        return holes.size();
    }

    boolean is_in_standard_form() {
        if (outer_boundary.is_in_standard_form() == false || outer_boundary.area() < 0)
            return false;
        for (int i = 0; i < holes.size(); i++)
            if (holes.get(i).is_in_standard_form() == false || holes.get(i).area() > 0)
                return false;
        return true;
    }

    public boolean is_valid(double epsilon) {
        if (n() <= 2)
            return false;

        // Check all Polygons are simple.
        if (!outer_boundary.is_simple(epsilon)) {
            /*
             * std::cerr << std::endl << "\x1b[31m" << "The outer boundary is not simple." <<
             * "\x1b[0m" << std::endl;
             */
            return false;
        }
        for (int i = 0; i < h(); i++)
            if (!holes.get(i).is_simple(epsilon)) {
                /*
                 * std::cerr << std::endl << "\x1b[31m" << "Hole " << i << " is not simple." <<
                 * "\x1b[0m" << std::endl;
                 */
                return false;
            }

        // Check none of the Polygons' boundaries intersect w/in epsilon.
        for (int i = 0; i < h(); i++)
            if (outer_boundary.boundary_distance(holes.get(i)) <= epsilon) {
                /*
                 * std::cerr << std::endl << "\x1b[31m" <<
                 * "The outer boundary intersects the boundary of hole " << i << "." << "\x1b[0m" <<
                 * std::endl;
                 */
                return false;
            }
        for (int i = 0; i < h(); i++)
            for (int j = i + 1; j < h(); j++)
                if (holes.get(i).boundary_distance(holes.get(j)) <= epsilon) {
                    /*
                     * std::cerr << std::endl << "\x1b[31m" << "The boundary of hole " << i <<
                     * " intersects the boundary of hole " << j << "." << "\x1b[0m" << std::endl;
                     */
                    return false;
                }

        // Check that the vertices of each hole are in the outside_boundary
        // and not in any other holes.
        // Loop over holes.
        for (int i = 0; i < h(); i++) {
            // Loop over vertices of a hole
            for (int j = 0; j < holes.get(i).n(); j++) {
                if (!holes.get(i).get(j).in(outer_boundary, epsilon)) {
                    /*
                     * std::cerr << std::endl << "\x1b[31m" << "Vertex " << j << " of hole " << i <<
                     * " is not within the outer boundary." << "\x1b[0m" << std::endl;
                     */
                    return false;
                }
                // Second loop over holes.
                for (int k = 0; k < h(); k++)
                    if (i != k && holes.get(i).get(j).in(holes.get(k), epsilon)) {
                        /*
                         * std::cerr << std::endl << "\x1b[31m" << "Vertex " << j << " of hole " <<
                         * i << " is in hole " << k << "." << "\x1b[0m" << std::endl;
                         */
                        return false;
                    }
            }
        }

        // Check outer_boundary is ccw and holes are cw.
        if (outer_boundary.area() <= 0) {
            /*
             * std::cerr << std::endl << "\x1b[31m" <<
             * "The outer boundary vertices are not listed ccw." << "\x1b[0m" << std::endl;
             */
            return false;
        }
        for (int i = 0; i < h(); i++)
            if (holes.get(i).area() >= 0) {
                /*
                 * std::cerr << std::endl << "\x1b[31m" << "The vertices of hole " << i <<
                 * " are not listed cw." << "\x1b[0m" << std::endl;
                 */
                return false;
            }

        return true;
    }

    double boundary_length() {
        // Precondition: nonempty Environment.
        assert (outer_boundary.n() > 0);

        double length_temp = outer_boundary.boundary_length();
        for (int i = 0; i < h(); i++)
            length_temp += holes.get(i).boundary_length();
        return length_temp;
    }

    double area() {
        double area_temp = outer_boundary.area();
        for (int i = 0; i < h(); i++)
            area_temp += holes.get(i).area();
        return area_temp;
    }

    ArrayList<VLPoint> random_points(int count, double epsilon) {
        assert (area() > 0);

        BoundingBox bounding_box = bbox();
        ArrayList<VLPoint> pts_in_environment = new ArrayList<VLPoint>(count);
        VLPoint pt_temp = new VLPoint(
                Util.uniform_random_sample(bounding_box.x_min, bounding_box.x_max),
                Util.uniform_random_sample(bounding_box.y_min, bounding_box.y_max));
        while (pts_in_environment.size() < count) {
            while (!pt_temp.in(this, epsilon)) {
                pt_temp.set_x(Util.uniform_random_sample(bounding_box.x_min, bounding_box.x_max));
                pt_temp.set_y(Util.uniform_random_sample(bounding_box.y_min, bounding_box.y_max));
            }
            pts_in_environment.add(pt_temp);
            pt_temp.set_x(Util.uniform_random_sample(bounding_box.x_min, bounding_box.x_max));
            pt_temp.set_y(Util.uniform_random_sample(bounding_box.y_min, bounding_box.y_max));
        }
        return pts_in_environment;
    }

    
    BoundingBox bbox() {
        return outer_boundary.bbox();
    }

    public VLPolygon get(int i) {
        if (i == 0) {
            return outer_boundary;
        } else {
            return holes.get(i - 1);
        }
    }

    public void enforce_standard_form() {
        if (outer_boundary.area() < 0)
            outer_boundary.reverse();
        outer_boundary.enforce_standard_form();
        for (int i = 0; i < h(); i++) {
            if (holes.get(i).area() > 0)
                holes.get(i).reverse();
            holes.get(i).enforce_standard_form();
        }
    }

    void eliminate_redundant_vertices(double epsilon) {
        outer_boundary.eliminate_redundant_vertices(epsilon);
        for (int i = 0; i < holes.size(); i++)
            holes.get(i).eliminate_redundant_vertices(epsilon);

        update_flattened_index_key();
    }

    void reverse_holes() {
        for (int i = 0; i < holes.size(); i++)
            holes.get(i).reverse();
    }

    void update_flattened_index_key() {
        flattened_index_key.clear();

        for (int i = 0; i <= h(); i++) {
            for (int j = 0; j < get(i).n(); j++) {
                pair<Integer, Integer> pair_temp = new pair<Integer, Integer>(i, j);
                flattened_index_key.add(pair_temp);
            }
        }
    }

    pair<Integer, Integer> one_to_two(int k) {
        pair<Integer, Integer> two = new pair<Integer, Integer>(0, 0);
        // Strategy: add up vertex count of each Polygon (outer boundary +
        // holes) until greater than k
        int current_polygon_index = 0;
        int vertex_count_up_to_current_polygon = get(0).n();
        int vertex_count_up_to_last_polygon = 0;

        while (k >= vertex_count_up_to_current_polygon && current_polygon_index < h()) {
            current_polygon_index++;
            two.first = two.first + 1;
            vertex_count_up_to_last_polygon = vertex_count_up_to_current_polygon;
            vertex_count_up_to_current_polygon += get(current_polygon_index).n();
        }
        two.second = k - vertex_count_up_to_last_polygon;

        return two;
    }

    public String toString() {
        String outs = "//Environment Model\n";
        outs += "//Outer Boundary\n" + get(0);
        for (int i = 1; i <= h(); i++) {
            outs += "//Hole\n " + get(i);
        }
        // outs << "//EOF marker";
        return outs;
    }

    double boundary_distance(VLPoint point_temp) {
        return point_temp.boundary_distance(this);
    }

}