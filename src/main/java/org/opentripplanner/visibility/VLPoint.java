package org.opentripplanner.visibility;

import java.lang.Math;

/**
 Ported by David Turner from Visilibity, by Karl J. Obermeyer


 This port undoubtedly introduced a number of bugs (and removed some features).

 Bug reports should be directed to the OpenTripPlanner project, unless they
 can be reproduced in the original VisiLibity
 */
public class VLPoint implements Comparable<VLPoint>, Cloneable {

    public double x, y;

    public VLPoint(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public boolean equals(Object o) {
        if (!(o instanceof VLPoint)) {
            return false;
        }
        VLPoint point2 = (VLPoint) o;
        return x == point2.x && y == point2.y;
    }

    public int compareTo(VLPoint point2) {

        if (x < point2.x) { return -1; }
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