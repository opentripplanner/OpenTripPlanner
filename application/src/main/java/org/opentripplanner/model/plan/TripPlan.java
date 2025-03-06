package org.opentripplanner.model.plan;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * A TripPlan is a set of ways to get from point A to point B at time T.
 */
public class TripPlan {

  /** The time and date of travel */
  public final Instant date;

  /** The origin */
  public final Place from;

  /** The destination */
  public final Place to;

  public final List<Itinerary> itineraries;

  public TripPlan(Place from, Place to, Instant date, Collection<Itinerary> itineraries) {
    this.from = from;
    this.to = to;
    this.date = date;
    this.itineraries = List.copyOf(itineraries);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(TripPlan.class)
      .addObj("date", date)
      .addObj("from", from)
      .addObj("to", to)
      .addObj("itineraries", itineraries)
      .toString();
  }
}
