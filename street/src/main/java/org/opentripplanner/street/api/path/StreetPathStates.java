package org.opentripplanner.street.api.path;

import java.util.List;
import org.opentripplanner.street.search.state.State;

/**
 * Access to the states of a {@link StreetPath}, which are deliberately not part of its API.
 * <p>
 * This only exists for the carpooling sandbox, which still needs the states until it is
 * migrated. Do not use it in new code.
 */
public final class StreetPathStates {

  private StreetPathStates() {}

  /**
   * The states of the path in chronological order.
   */
  public static List<State> states(StreetPath path) {
    return ((DefaultStreetPath) path).states();
  }
}
