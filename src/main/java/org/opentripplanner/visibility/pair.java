package org.opentripplanner.visibility;

/**
 Ported by David Turner from Visilibity, by Karl J. Obermeyer


 This port undoubtedly introduced a number of bugs (and removed some features).

 Bug reports should be directed to the OpenTripPlanner project, unless they
 can be reproduced in the original VisiLibity.
//c++'s std::pair -- it's only used a few places, so
//we might as well just copy it
*/
class pair<T, U> {
    public T first;

    public U second;

    public pair() {
    }

    public pair(T one, U two) {
        first = one;
        second = two;
    }

    public T first() {
        return first;
    }

    public U second() {
        return second;
    }
}