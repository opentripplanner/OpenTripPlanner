package org.opentripplanner.api.parameter;

import com.google.common.collect.Sets;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraverseMode;

import java.io.Serializable;
import java.security.InvalidParameterException;
import java.util.Set;

public class QualifiedMode implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public final TraverseMode mode;
    public final Set<Qualifier> qualifiers = Sets.newHashSet();

    public QualifiedMode(String qMode) {
        String[] elements = qMode.split("_");
        mode = TraverseMode.valueOf(elements[0].trim());
        if (mode == null) {
            throw new InvalidParameterException();
        }
        for (int i = 1; i < elements.length; i++) {
            Qualifier q = Qualifier.valueOf(elements[i].trim());
            if (q == null) {
                throw new InvalidParameterException();
            } else {
                qualifiers.add(q);
            }
        }
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(mode);
        for (Qualifier qualifier : qualifiers) {
            sb.append("_");
            sb.append(qualifier);
        }
        return sb.toString();
    }

    public void applyToRoutingRequest (RoutingRequest req, boolean usingTransit) {
        req.modes.setMode(this.mode, true);
        if (this.mode == TraverseMode.BICYCLE) {
            if (this.qualifiers.contains(Qualifier.RENT)) {
                req.modes.setMode(TraverseMode.WALK, true); // turn on WALK for bike rental mode
                req.allowBikeRental = true;
            }
            if (usingTransit) {
                req.bikeParkAndRide = this.qualifiers.contains(Qualifier.PARK);
            }
        }
        if (usingTransit && this.mode == TraverseMode.CAR) {
            if (this.qualifiers.contains(Qualifier.PARK)) {
                req.parkAndRide = true;
            } else {
                req.kissAndRide = true;
            }
            req.modes.setWalk(true); // need to walk after dropping the car off
        }
    }

    @Override
    public int hashCode() {
        return mode.hashCode() * qualifiers.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof QualifiedMode) {
            QualifiedMode qmOther = (QualifiedMode) other;
            return qmOther.mode.equals(this.mode) && qmOther.qualifiers.equals(this.qualifiers);
        }
        return false;
    }

}

enum Qualifier {
    RENT, HAVE, PARK, KEEP
}