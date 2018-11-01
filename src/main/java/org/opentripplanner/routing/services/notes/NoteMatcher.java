package org.opentripplanner.routing.services.notes;

import java.io.Serializable;

import org.opentripplanner.routing.core.State;

/**
 * A note matcher will determine if a note is applicable to a given state, based on condition such
 * as current traverse mode, wheelchair access, etc...
 * 
 * @author laurent
 */
public interface NoteMatcher extends Serializable {
    public boolean matches(State state);
}
