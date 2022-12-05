package org.opentripplanner.street.model;

import java.io.Serializable;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.search.TraverseModeSet;

public class TurnRestriction implements Serializable {

  public final TurnRestrictionType type;
  public final StreetEdge from;
  public final StreetEdge to;
  public final RepeatingTimePeriod time;
  public final TraverseModeSet modes;

  public TurnRestriction(
    StreetEdge from,
    StreetEdge to,
    TurnRestrictionType type,
    TraverseModeSet modes,
    RepeatingTimePeriod time
  ) {
    this.from = from;
    this.to = to;
    this.type = type;
    this.modes = modes;
    this.time = time;
  }

  /**
   * Return true if the turn restriction is in force at the time described by the long.
   */
  public boolean active(long time) {
    if (this.time != null) {
      return this.time.active(time);
    }
    return true;
  }

  @Override
  public String toString() {
    return type.name() + " from " + from + " to " + to + " (modes: " + modes + ")";
  }
}
