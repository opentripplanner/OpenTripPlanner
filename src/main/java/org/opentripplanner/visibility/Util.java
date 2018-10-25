package org.opentripplanner.visibility;

import java.util.Random;

/**
 Ported by David Turner from Visilibity, by Karl J. Obermeyer


 This port undoubtedly introduced a number of bugs (and removed some features).

 Bug reports should be directed to the OpenTripPlanner project, unless they
 can be reproduced in the original VisiLibity
 */
public class Util {
    static Random rng = new Random();

    public static double uniform_random_sample(double lower_bound, double upper_bound) {
        if (lower_bound == upper_bound)
            return lower_bound;
        double sample_point;
        double span = upper_bound - lower_bound;
        sample_point = lower_bound + span * rng.nextDouble();
        return sample_point;
    }
}