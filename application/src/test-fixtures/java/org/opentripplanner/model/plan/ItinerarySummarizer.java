package org.opentripplanner.model.plan;

import java.util.List;
import org.opentripplanner.model.plan.leg.StreetLeg;

public class ItinerarySummarizer {

  private final Itinerary itinerary;

  public ItinerarySummarizer(Itinerary itinerary) {
    this.itinerary = itinerary;
  }

  public List<String> summarizeLegs() {
    return itinerary.legs().stream().map(ItinerarySummarizer::mapLeg).toList();
  }

  private static String mapLeg(Leg leg) {
    return switch (leg) {
      case StreetLeg sl -> mapStreetLeg(sl);
      default -> throw new IllegalStateException("Unexpected value: " + leg);
    };
  }

  private static String mapStreetLeg(StreetLeg leg) {
    var buf = new StringBuilder();
    buf.append("[");
    buf.append(leg.startTime().toOffsetDateTime());
    buf.append(" ");
    buf.append(leg.from().toStringShort());
    buf.append("] → [");
    buf.append(leg.endTime().toOffsetDateTime());
    buf.append(" ");
    buf.append(leg.to().toStringShort());
    buf.append("]");
    return buf.toString();
  }
}
