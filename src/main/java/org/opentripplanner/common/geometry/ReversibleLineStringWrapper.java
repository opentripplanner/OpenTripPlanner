package org.opentripplanner.common.geometry;

import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.LineString;

public class ReversibleLineStringWrapper {

    LineString ls;

    public ReversibleLineStringWrapper(LineString ls) {
        this.ls = ls;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ReversibleLineStringWrapper))
            return false;
        ReversibleLineStringWrapper that = (ReversibleLineStringWrapper) other;
        CoordinateSequence cs0 = this.ls.getCoordinateSequence();
        CoordinateSequence cs1 = that.ls.getCoordinateSequence();
        if (cs0.equals(cs1))
            return true;
        return matchesBackward(cs0, cs1);
    }

    public boolean matchesBackward(CoordinateSequence cs0, CoordinateSequence cs1) {
        if (cs0.size() != cs1.size())
            return false;
        int maxIdx = cs0.size() - 1;
        for (int i = 0; i <= maxIdx; i++)
            if (!cs0.getCoordinate(i).equals(cs1.getCoordinate(maxIdx - i)))
                return false;
        return true;
    }

    @Override
    public int hashCode() {
        CoordinateSequence cs = ls.getCoordinateSequence();
        int maxIdx = cs.size() - 1;
        int x = (int) (cs.getX(0) * 1000000) + (int) (cs.getX(maxIdx) * 1000000);
        int y = (int) (cs.getY(0) * 1000000) + (int) (cs.getY(maxIdx) * 1000000);
        return x + y * 101149 + maxIdx * 7883;
    }

}
