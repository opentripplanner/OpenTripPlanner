package org.opentripplanner.netex.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.netex.index.api.HMapValidationRule.Status.DISCARD;
import static org.opentripplanner.netex.index.api.HMapValidationRule.Status.OK;
import static org.rutebanken.netex.model.StopUseEnumeration.ACCESS;
import static org.rutebanken.netex.model.StopUseEnumeration.PASSTHROUGH;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.netex.index.NetexEntityIndex;
import org.opentripplanner.netex.mapping.MappingSupport;
import org.rutebanken.netex.model.EntityStructure;
import org.rutebanken.netex.model.JourneyPatternRefStructure;
import org.rutebanken.netex.model.PointInJourneyPatternRefStructure;
import org.rutebanken.netex.model.PointInLinkSequence_VersionedChildStructure;
import org.rutebanken.netex.model.PointsInJourneyPattern_RelStructure;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.ServiceJourneyPattern;
import org.rutebanken.netex.model.StopPointInJourneyPattern;
import org.rutebanken.netex.model.StopUseEnumeration;
import org.rutebanken.netex.model.TimetabledPassingTime;
import org.rutebanken.netex.model.TimetabledPassingTimes_RelStructure;

class JourneyPatternSJMismatchTest {

  private static final String PATTERN_ID = "pattern";
  private static final String JOURNEY_ID = "journey";

  @Test
  void patternAndJourneyMatch() {
    var pattern = new ServiceJourneyPatternBuilder(PATTERN_ID)
      .withPointsInSequence(1, 2, 3)
      .build();

    var index = new NetexEntityIndex();
    index.journeyPatternsById.add(pattern);

    var journey = new ServiceJourneyBuilder(JOURNEY_ID)
      .withPatternId(PATTERN_ID)
      .withPassingTimes(pattern.getPointsInSequence())
      .build();

    var rule = new JourneyPatternSJMismatch();
    rule.setup(index.readOnlyView());

    assertEquals(OK, rule.validate(journey));
  }

  @Test
  void differentNumberOfPassingTimes() {
    var pattern = new ServiceJourneyPatternBuilder(PATTERN_ID)
      .withPointsInSequence(1, 2, 3)
      .build();

    var index = new NetexEntityIndex();
    index.journeyPatternsById.add(pattern);

    var journey = new ServiceJourneyBuilder(JOURNEY_ID)
      .withPatternId(PATTERN_ID)
      .withPassingTimes(List.of("P-1", "P-2"))
      .build();

    var rule = new JourneyPatternSJMismatch();
    rule.setup(index.readOnlyView());

    assertEquals(DISCARD, rule.validate(journey));
  }

  @Test
  void passThrough() {
    var pattern = new ServiceJourneyPatternBuilder(PATTERN_ID)
      .addStopPointInSequence(1, ACCESS)
      .addStopPointInSequence(2, PASSTHROUGH)
      .addStopPointInSequence(3, ACCESS)
      .build();

    var index = new NetexEntityIndex();
    index.journeyPatternsById.add(pattern);

    var journey = new ServiceJourneyBuilder(JOURNEY_ID)
      .withPatternId(PATTERN_ID)
      .withPassingTimes(List.of("P-1", "P-3"))
      .build();

    var rule = new JourneyPatternSJMismatch();
    rule.setup(index.readOnlyView());

    assertEquals(OK, rule.validate(journey));
  }

  static class ServiceJourneyPatternBuilder {

    private final ServiceJourneyPattern pattern = new ServiceJourneyPattern();
    private final PointsInJourneyPattern_RelStructure points =
      new PointsInJourneyPattern_RelStructure();

    ServiceJourneyPatternBuilder(String id) {
      pattern.setId(id);
      pattern.setPointsInSequence(points);
    }

    ServiceJourneyPatternBuilder withPointsInSequence(int... orders) {
      var items = Arrays.stream(orders).mapToObj(order -> pointInPattern(order, ACCESS)).toList();
      points.withPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern(
        items
      );
      return this;
    }

    ServiceJourneyPatternBuilder addStopPointInSequence(int order, StopUseEnumeration stopUse) {
      var point = pointInPattern(order, stopUse);
      points
        .getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern()
        .add(point);
      return this;
    }

    ServiceJourneyPattern build() {
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

  static class ServiceJourneyBuilder {

    private final ServiceJourney journey = new ServiceJourney();

    ServiceJourneyBuilder(String id) {
      journey.setId(id);
    }

    ServiceJourneyBuilder withPatternId(String id) {
      var ref = MappingSupport.createWrappedRef(id, JourneyPatternRefStructure.class);
      journey.withJourneyPatternRef(ref);
      return this;
    }

    ServiceJourneyBuilder withPassingTimes(PointsInJourneyPattern_RelStructure pointsInSequence) {
      return withPassingTimes(
        pointsInSequence
          .getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern()
          .stream()
          .map(EntityStructure::getId)
          .toList()
      );
    }

    ServiceJourneyBuilder withPassingTimes(Collection<String> ids) {
      var passingTimes = new TimetabledPassingTimes_RelStructure();
      passingTimes.withTimetabledPassingTime(
        ids.stream().map(ServiceJourneyBuilder::timetabledPassingTime).toList()
      );
      journey.withPassingTimes(passingTimes);
      return this;
    }

    ServiceJourney build() {
      return journey;
    }

    private static TimetabledPassingTime timetabledPassingTime(String pointInPatternRef) {
      var passingTime = new TimetabledPassingTime();
      passingTime.withPointInJourneyPatternRef(
        MappingSupport.createWrappedRef(pointInPatternRef, PointInJourneyPatternRefStructure.class)
      );
      return passingTime;
    }
  }
}
