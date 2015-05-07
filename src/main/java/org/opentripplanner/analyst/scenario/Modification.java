package org.opentripplanner.analyst.scenario;

import java.util.HashSet;
import java.util.Set;

/**
 * A Modification is a single change that can be applied non-destructively to an OTP graph.
 */
public abstract class Modification {

    /** Distinguish between modification types when a list of Modifications are serialized out as JSON. */
    public String getType() {
        return this.getClass().getSimpleName();
    }

    public final Set<String> warnings = new HashSet<String>();

    /**
     * Applies this non-destructively to a scenario-specific snapshot of the graph.
     */
    public void applyToGraph() {  }

}
