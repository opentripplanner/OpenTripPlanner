package org.opentripplanner.visibility;

/**
 Ported by David Turner from Visilibity, by Karl J. Obermeyer
   
 
 This port undoubtedly introduced a number of bugs (and removed some features).
 
 Bug reports should be directed to the OpenTripPlanner project, unless they 
 can be reproduced in the original VisiLibity
 */

import java.util.ArrayList;
import java.util.List;

/**
 Ported by David Turner from Visilibity, by Karl J. Obermeyer


 This port undoubtedly introduced a number of bugs (and removed some features).

 Bug reports should be directed to the OpenTripPlanner project, unless they
 can be reproduced in the original VisiLibity
 */
public class VLPolygon {

    public ArrayList<VLPoint> vertices;

    public VLPolygon(List<VLPoint> vertices_temp) {
        vertices = new ArrayList<VLPoint>(vertices_temp);
    }

    public int n() {
        return vertices.size();
    }

    public boolean is_in_standard_form() {
        // if more than one point in the polygon.
        if (vertices.size() > 1) {
            for (int i = 1; i < vertices.size(); i++) {
                if (vertices.get(0).compareTo(vertices.get(i)) > 0) {
                    return false;
                }
            }
        }
        return true;
    }

    public double area() {
        double area_temp = 0;
        if (n() == 0) { return 0; }
        for (int i = 0; i <= n() - 1; i++) {
            area_temp += get(i).x * get(i + 1).y - get(i + 1).x * get(i).y;
        }
        return area_temp / 2.0;
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
        if (n() != polygon2.n() || n() == 0 || polygon2.n() == 0) {
            return false;
        }
        for (int i = 0; i < n(); i++) {
            if (!get(i).equals(polygon2.get(i))) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        return vertices.hashCode() + 1;
    }

    public VLPoint get(int i) {
        return vertices.get(i % vertices.size());
    }

    public String toString() {
        String outs = "";
        for (int i = 0; i < n(); i++)
            outs += get(i) + "\n";
        return outs;
    }
}