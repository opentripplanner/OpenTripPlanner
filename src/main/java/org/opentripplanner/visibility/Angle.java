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

class Angle implements Comparable<Angle>, Cloneable {
    double angle_radians;

    public Angle clone() {
        Angle angle = new Angle(angle_radians);
        angle.angle_radians = angle_radians; // handle 2pi case
        return angle;
    }

    public void set_to_2pi() {
        angle_radians = 2 * Math.PI;
    }

    public double get() {
        return angle_radians;
    }

    public Angle(double data_temp) {
        if (data_temp >= 0)
            angle_radians = data_temp % (2 * Math.PI);
        else {
            angle_radians = 2 * Math.PI + data_temp % -(2 * Math.PI);
            if (angle_radians == 2 * Math.PI)
                angle_radians = 0;
        }
    }

    public Angle(double rise_temp, double run_temp) {
        // First calculate 4 quadrant inverse tangent into [-pi,+pi].
        angle_radians = Math.atan2(rise_temp, run_temp);
        // Correct so angles specified in [0, 2*PI).
        if (angle_radians < 0)
            angle_radians = 2 * Math.PI + angle_radians;
    }

    void set(double data_temp) {
        angle_radians = data_temp;
    }

    void randomize() {
        angle_radians = Util.uniform_random_sample(0, 2 * Math.PI) % (2 * Math.PI);
    }

    public boolean equals(Object other) {
        if (!(other instanceof Angle)) {
            return false;
        }
        Angle angle2 = (Angle) other;
        return (get() == angle2.get());
    }

    public int compareTo(Angle angle2) {
        return (int) Math.signum(get() - angle2.get());
    }

    Angle plus(Angle angle2) {
        return new Angle(get() + angle2.get());
    }

    Angle minus(Angle angle2) {
        return new Angle(get() - angle2.get());
    }

    double geodesic_distance(Angle angle2) {
        double distance1 = Math.abs(get() - angle2.get());
        double distance2 = 2 * Math.PI - distance1;
        if (distance1 < distance2)
            return distance1;
        return distance2;
    }

    double geodesic_direction(Angle angle2) {

        double distance1 = Math.abs(get() - angle2.get());
        double distance2 = 2 * Math.PI - distance1;
        if (get() <= angle2.get()) {
            if (distance1 < distance2)
                return 1.0;
            return -1.0;
        }
        // Otherwise angle1 > angle2.
        if (distance1 < distance2)
            return -1.0;
        return 1.0;
    }

    public String toString() {

        return "" + angle_radians;
    }

    public int hashCode() {
        return new Double(angle_radians).hashCode();
    }
}