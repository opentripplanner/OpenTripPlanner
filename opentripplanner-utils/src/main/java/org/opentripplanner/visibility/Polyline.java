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
import java.util.Collections;

class Polyline {

    ArrayList<VLPoint> vertices = new ArrayList<VLPoint>();

    VLPoint get(int i) {
        return vertices.get(i);
    }

    int size() {
        return vertices.size();
    }

    double distance(VLPoint point_temp) {
        return point_temp.distance(this);
    }

    double length() {
        double length_temp = 0;
        for (int i = 1; i <= vertices.size() - 1; i++)
            length_temp += vertices.get(i - 1).distance(vertices.get(i));
        return length_temp;
    }

    double diameter() {
        // Precondition: nonempty Polyline.
        assert (size() > 0);

        double running_max = 0;
        for (int i = 0; i < size() - 1; i++) {
            for (int j = i + 1; j < size(); j++) {
                if (get(i).distance(get(j)) > running_max)
                    running_max = get(i).distance(get(j));
            }
        }
        return running_max;
    }

    BoundingBox bbox() {
        // Precondition: nonempty Polyline.
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

    void eliminate_redundant_vertices(double epsilon) {
        // Trivial case
        if (vertices.size() < 3)
            return;

        // Store new minimal length list of vertices
        ArrayList<VLPoint> vertices_temp = new ArrayList<VLPoint>(vertices.size());

        // Place holders
        int first = 0;
        int second = 1;
        int third = 2;

        // Add first vertex
        vertices_temp.add(get(first));

        while (third < vertices.size()) {
            // if second redundant
            if (new LineSegment(get(first), get(third)).distance(get(second)) <= epsilon) {
                // =>skip it
                second = third;
                third++;
            }
            // else second not redundant
            else {
                // =>add it.
                vertices_temp.add(get(second));
                first = second;
                second = third;
                third++;
            }
        }

        // Add last vertex
        vertices_temp.add(vertices.get(vertices.size() - 1));

        // Update list of vertices
        vertices = vertices_temp;
    }

    void reverse() {
        Collections.reverse(vertices);
    }

    public String toString() {
        String outs = "";
        for (int i = 0; i < size(); i++)
            outs += get(i) + "\n";
        return outs;
    }

    void append(Polyline polyline) {
        vertices.ensureCapacity(vertices.size() + polyline.vertices.size());
        for (int i = 0; i < polyline.vertices.size(); i++) {
            vertices.add(polyline.vertices.get(i));
        }
    }

}