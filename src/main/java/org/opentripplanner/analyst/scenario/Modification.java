package org.opentripplanner.analyst.scenario;

import org.opentripplanner.profile.RaptorWorkerData;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * A Modification is a single change that can be applied non-destructively to RaptorWorkerData.
 */
public abstract class Modification implements Serializable {

    /** Distinguish between modification types when a list of Modifications are serialized out as JSON. */
    public abstract String getType();

    public final Set<String> warnings = new HashSet<String>();
}
