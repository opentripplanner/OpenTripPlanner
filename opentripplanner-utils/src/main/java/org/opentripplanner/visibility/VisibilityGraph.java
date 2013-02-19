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
import java.util.Collection;

public class VisibilityGraph {

    // the number of vertices in each Polygon of corresponding Environment
    ArrayList<Integer> vertex_counts;

    // n-by-n adjacency matrix data stored as 2D dynamic array
    boolean[][] adjacency_matrix;

    int n;

    public VisibilityGraph(Environment environment, double epsilon) {
        this(environment, epsilon, null);
    }

    public VisibilityGraph(Environment environment, double epsilon, Collection<VLPoint> origins) {
        n = environment.n();

        // fill vertex_counts
        vertex_counts = new ArrayList<Integer>(environment.h());
        for (int i = 0; i < environment.h(); i++)
            vertex_counts.add(environment.get(i).n());

        adjacency_matrix = new boolean[n][n];
        // fill adjacency matrix by checking for inclusion in the
        // visibility polygons
        for (int k1 = 0; k1 < n; k1++) {
            VLPoint point1 = environment.kth_point(k1);
            if (origins != null && !origins.contains(point1)) 
                    continue;
            VLPolygon polygon_temp = new VisibilityPolygon(point1, environment, epsilon);
            for (int k2 = 0; k2 < n; k2++) {
                if (k1 == k2)
                    adjacency_matrix[k1][k1] = true;
                else {
                    VLPoint point2 = environment.kth_point(k2);
                    if (origins == null || origins.contains(point2)) {
                        adjacency_matrix[k1][k2] = adjacency_matrix[k2][k1] = point2.in(
                                polygon_temp, epsilon);
                    }
                }
            }
        }
    }

    public VisibilityGraph(ArrayList<VLPoint> points, Environment environment, double epsilon) {
        vertex_counts = new ArrayList<Integer>(environment.h());
        n = points.size();

        // fill vertex_counts
        vertex_counts.add(n);

        adjacency_matrix = new boolean[n][n];

        // fill adjacency matrix by checking for inclusion in the
        // visibility polygons
        VLPolygon polygontemp;
        for (int k1 = 0; k1 < n; k1++) {
            polygontemp = new VisibilityPolygon(points.get(k1), environment, epsilon);
            for (int k2 = 0; k2 < n; k2++) {
                if (k1 == k2)
                    adjacency_matrix[k1][k1] = true;
                else
                    adjacency_matrix[k1][k2] = adjacency_matrix[k2][k1] = points.get(k2).in(
                            polygontemp, epsilon);
            }
        }
    }

    public boolean get(int polygon1, int vertex1, int polygon2, int vertex2) {
        return adjacency_matrix[get_vertex_index(polygon1, vertex1)][get_vertex_index(polygon2, vertex2)];
    }

    public boolean get(int k1, int k2) {
        return adjacency_matrix[k1][k2];
    }


    // original code called this two_to_one, incomprehensibly
    public int get_vertex_index(int polygon, int vertex) {
        int k = 0;

        for (int counter = 0; counter < polygon; counter++)
            k += vertex_counts.get(counter);
        k += vertex;

        return k;
    }

    public String toString() {
        String outs = "";
        for (int k1 = 0; k1 < n; k1++) {
            for (int k2 = 0; k2 < n; k2++) {
                outs += get(k1, k2) ? "1" : "0";
                if (k2 < n - 1)
                    outs += "  ";
                else
                    outs += "\n";
            }
        }

        return outs;
    }

}