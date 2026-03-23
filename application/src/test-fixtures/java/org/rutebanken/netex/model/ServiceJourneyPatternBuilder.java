package org.rutebanken.netex.model;

import static org.rutebanken.netex.model.StopUseEnumeration.ACCESS;

import java.math.BigInteger;
import java.util.Arrays;

public class ServiceJourneyPatternBuilder {

  private final ServiceJourneyPattern pattern = new ServiceJourneyPattern();
  private final PointsInJourneyPattern_RelStructure points =
    new PointsInJourneyPattern_RelStructure();

  public ServiceJourneyPatternBuilder(String id) {
    pattern.setId(id);
  }

  /**
   * Generates point in sequence and replaces the existing ones.
   */
  public ServiceJourneyPatternBuilder withPointsInSequence(int... orders) {
    var items = Arrays.stream(orders)
      .mapToObj(order -> pointInPattern(order, ACCESS))
      .toList();
    points.withPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern(items);
    return this;
  }

  public ServiceJourneyPatternBuilder addStopPointInSequence(
    int order,
    StopUseEnumeration stopUse
  ) {
    var point = pointInPattern(order, stopUse);
    points
      .getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern()
      .add(point);
    return this;
  }

  public ServiceJourneyPattern build() {
    pattern.setPointsInSequence(points);
    return pattern;
  }

  private static PointInLinkSequence_VersionedChildStructure pointInPattern(
    int order,
    StopUseEnumeration stopUse
  ) {
    var p = new StopPointInJourneyPattern();
    p.setId("P-%s".formatted(order));
    p.setOrder(BigInteger.valueOf(order));
    p.setStopUse(stopUse);
    return p;
  }
}
