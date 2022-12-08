package org.opentripplanner.street.model.note;

import java.io.Serializable;
import org.opentripplanner.street.search.state.State;

/**
 * A note matcher will determine if a note is applicable to a given state, based on condition such
 * as current traverse mode, wheelchair access, etc...
 *
 * @author laurent
 */
public interface StreetNoteMatcher extends Serializable {
  boolean matches(State state);
}
