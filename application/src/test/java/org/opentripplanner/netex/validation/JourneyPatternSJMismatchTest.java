package org.opentripplanner.netex.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.netex.index.api.HMapValidationRule.Status.DISCARD;
import static org.opentripplanner.netex.index.api.HMapValidationRule.Status.OK;
import static org.rutebanken.netex.model.StopUseEnumeration.ACCESS;
import static org.rutebanken.netex.model.StopUseEnumeration.PASSTHROUGH;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.netex.index.NetexEntityIndex;
import org.rutebanken.netex.model.ServiceJourneyBuilder;
import org.rutebanken.netex.model.ServiceJourneyPatternBuilder;

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
}
