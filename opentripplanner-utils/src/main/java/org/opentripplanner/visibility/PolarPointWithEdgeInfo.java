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

class PolarPointWithEdgeInfo extends PolarPoint implements Cloneable {

    // this was originally an iterator, bafflingly -DMT
    PolarEdge incident_edge;

    boolean is_first; // True iff polar_point is the first_point of the

    // PolarEdge pointed to by
    // incident_edge.

    PolarPointWithEdgeInfo() {
        super();
    }

    void set_polar_point(PolarPoint ppoint_temp) {
        set_polar_origin(ppoint_temp.polar_origin);
        set_x(ppoint_temp.x);
        set_y(ppoint_temp.y);
        set_range(ppoint_temp.range);
        set_bearing(ppoint_temp.bearing);
    }

    void set(PolarPointWithEdgeInfo ppoint_temp) {
        set_polar_origin(ppoint_temp.polar_origin);
        set_x(ppoint_temp.x);
        set_y(ppoint_temp.y);
        set_range(ppoint_temp.range);
        set_bearing(ppoint_temp.bearing);
        incident_edge = ppoint_temp.incident_edge;
        is_first = ppoint_temp.is_first;
    }

    // The operator < is the same as for PolarPoint with one
    // exception. If two vertices have equal coordinates, but one is
    // the first point of its respective edge and the other is the
    // second point of its respective edge, then the vertex which is
    // the second point of its respective edge is considered
    // lexicographically smaller.
    public int compareTo(VLPoint point2) {
        if (!(point2 instanceof PolarPointWithEdgeInfo)) {
            return super.compareTo(point2);
        }

        PolarPointWithEdgeInfo polar_point2 = (PolarPointWithEdgeInfo) point2;
        int bearingComp = bearing.compareTo(polar_point2.bearing);
        if (bearingComp < 0)
            return -1;
        else if (bearingComp == 0) {
            if (range < polar_point2.range) {
                return -1;
            } else if (range > polar_point2.range) {
                return 1;
            } else {
                if (!is_first && polar_point2.is_first) {
                    return -1;
                } else if (is_first && !polar_point2.is_first) {
                    return 1;
                }
                return 0;
            }
        }
        return 1;

    }

    public PolarPointWithEdgeInfo clone() {
        PolarPointWithEdgeInfo temp = new PolarPointWithEdgeInfo();
        temp.set(this);
        return temp;
    }

}
