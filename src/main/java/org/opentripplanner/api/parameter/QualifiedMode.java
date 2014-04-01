package org.opentripplanner.api.parameter;

import java.security.InvalidParameterException;
import java.util.Set;

import org.opentripplanner.routing.core.TraverseMode;

import com.beust.jcommander.internal.Sets;

public class QualifiedMode {

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

}

enum Qualifier {
    RENT, HAVE, PARK, KEEP
}