package org.opentripplanner.netex.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.netex.index.api.HMapValidationRule.Status.DISCARD;
import static org.opentripplanner.netex.index.api.HMapValidationRule.Status.OK;

import java.math.BigInteger;
import org.junit.jupiter.api.Test;
import org.opentripplanner.netex.index.NetexEntityIndex;
import org.opentripplanner.netex.mapping.MappingSupport;
import org.rutebanken.netex.model.JourneyPatternRefStructure;
import org.rutebanken.netex.model.PointsInJourneyPattern_RelStructure;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.ServiceJourneyPattern;
import org.rutebanken.netex.model.StopPointInJourneyPattern;

class JourneyPatternDuplicateStopPointsTest {

  private static final String PATTERN_ID = "pattern";
  private static final String JOURNEY_ID = "journey";

  @Test
  void noDuplicates() {
    var pattern = createPattern("P-1", "P-2", "P-3");

    var index = new NetexEntityIndex();
    index.journeyPatternsById.add(pattern);

    var journey = createJourney(PATTERN_ID);

    var rule = new JourneyPatternDuplicateStopPoints();
    rule.setup(index.readOnlyView());

    assertEquals(OK, rule.validate(journey));
  }

  @Test
  void duplicateStopPoints() {
    var pattern = createPattern("P-1", "P-2", "P-2");

    var index = new NetexEntityIndex();
    index.journeyPatternsById.add(pattern);

    var journey = createJourney(PATTERN_ID);

    var rule = new JourneyPatternDuplicateStopPoints();
    rule.setup(index.readOnlyView());

    assertEquals(DISCARD, rule.validate(journey));
  }

  @Test
  void duplicateStopPointsAtDifferentPositions() {
    var pattern = createPattern("P-1", "P-2", "P-3", "P-1");

    var index = new NetexEntityIndex();
    index.journeyPatternsById.add(pattern);

    var journey = createJourney(PATTERN_ID);

    var rule = new JourneyPatternDuplicateStopPoints();
    rule.setup(index.readOnlyView());

    assertEquals(DISCARD, rule.validate(journey));
  }

  private ServiceJourneyPattern createPattern(String... pointIds) {
    var pattern = new ServiceJourneyPattern();
    pattern.setId(PATTERN_ID);

    var points = new PointsInJourneyPattern_RelStructure();
    int order = 1;
    for (String pointId : pointIds) {
      var point = new StopPointInJourneyPattern();
      point.setId(pointId);
      point.setOrder(BigInteger.valueOf(order++));
      points
        .getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern()
        .add(point);
    }

    pattern.setPointsInSequence(points);
    return pattern;
  }

  private ServiceJourney createJourney(String patternId) {
    var journey = new ServiceJourney();
    journey.setId(JOURNEY_ID);
    var ref = MappingSupport.createWrappedRef(patternId, JourneyPatternRefStructure.class);
    journey.withJourneyPatternRef(ref);
    return journey;
  }
}
