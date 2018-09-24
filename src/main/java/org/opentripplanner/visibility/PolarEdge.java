package org.opentripplanner.visibility;

/**
 Ported by David Turner from Visilibity, by Karl J. Obermeyer
   
 
 This port undoubtedly introduced a number of bugs (and removed some features).
 
 Bug reports should be directed to the OpenTripPlanner project, unless they 
 can be reproduced in the original VisiLibity
 */
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