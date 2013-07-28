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

class PolarEdge {
    PolarPoint first;

    PolarPoint second;

    PolarEdge() {
    }

    PolarEdge(PolarPoint ppoint1, PolarPoint ppoint2) {
        first = ppoint1.clone();
        second = ppoint2.clone();
    }

    public String toString() {
        return "PolarEdge(" + first + ", " + second + ")";
    }

    public boolean equals(Object o) {
        if (!(o instanceof PolarEdge)) {
            return false;
        }
        PolarEdge other = (PolarEdge) o;
        return first.equals(other.first) && second.equals(other.second);
    }

    public int hashCode() {
        return 31 * first.hashCode() + second.hashCode();
    }
}