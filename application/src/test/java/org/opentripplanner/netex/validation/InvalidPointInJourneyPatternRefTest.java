package org.opentripplanner.netex.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.netex.index.api.HMapValidationRule.Status.DISCARD;
import static org.opentripplanner.netex.index.api.HMapValidationRule.Status.OK;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.netex.index.NetexEntityIndex;
import org.rutebanken.netex.model.ServiceJourneyBuilder;
import org.rutebanken.netex.model.ServiceJourneyPatternBuilder;

class InvalidPointInJourneyPatternRefTest {

  private static final String PATTERN_ID = "p1";
  private static final String JOURNEY_ID = "j1";

  @Test
  void validRef() {
    var pattern = new ServiceJourneyPatternBuilder(PATTERN_ID)
      .withPointsInSequence(1, 2, 3)
      .build();

    var index = new NetexEntityIndex();
    index.journeyPatternsById.add(pattern);

    var journey = new ServiceJourneyBuilder(JOURNEY_ID)
      .withPatternId(PATTERN_ID)
      .withPassingTimes(List.of("P-1", "P-2", "P-3"))
      .build();

    var rule = new InvalidPointInJourneyPatternRef();
    rule.setup(index.readOnlyView());

    assertEquals(OK, rule.validate(journey));
  }

  @Test
  void invalidRef() {
    var pattern = new ServiceJourneyPatternBuilder(PATTERN_ID)
      .withPointsInSequence(1, 2, 3)
      .build();

    var index = new NetexEntityIndex();
    index.journeyPatternsById.add(pattern);

    var journey = new ServiceJourneyBuilder(JOURNEY_ID)
      .withPatternId(PATTERN_ID)
      .withPassingTimes(List.of("P-1", "P-2", "P-4"))
      .build();

    var rule = new InvalidPointInJourneyPatternRef();
    rule.setup(index.readOnlyView());

    assertEquals(DISCARD, rule.validate(journey));
  }
}
