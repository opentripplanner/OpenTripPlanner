package org.opentripplanner.ext.stopconsolidation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.opentripplanner.ext.stopconsolidation.TestStopConsolidationModel.PATTERN;
import static org.opentripplanner.ext.stopconsolidation.TestStopConsolidationModel.STOP_A;
import static org.opentripplanner.ext.stopconsolidation.TestStopConsolidationModel.STOP_B;
import static org.opentripplanner.ext.stopconsolidation.TestStopConsolidationModel.STOP_C;
import static org.opentripplanner.ext.stopconsolidation.TestStopConsolidationModel.STOP_D;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.stopconsolidation.internal.DefaultStopConsolidationRepository;
import org.opentripplanner.ext.stopconsolidation.model.ConsolidatedStopGroup;
import org.opentripplanner.transit.model.network.TripPattern;

class StopConsolidationModuleTest {

  @Test
  void replacePatterns() {
    var groups = List.of(new ConsolidatedStopGroup(STOP_D.getId(), List.of(STOP_B.getId())));

    var transitModel = TestStopConsolidationModel.buildTransitModel();
    transitModel.addTripPattern(PATTERN.getId(), PATTERN);
    var repo = new DefaultStopConsolidationRepository();
    var module = new StopConsolidationModule(transitModel, repo, groups);
    module.buildGraph();

    var modifiedPattern = transitModel.getTripPatternForId(PATTERN.getId());
    assertFalse(modifiedPattern.getRoutingTripPattern().getPattern().sameAs(PATTERN));
    assertFalse(modifiedPattern.sameAs(PATTERN));

    var modifiedStop = modifiedPattern
      .getRoutingTripPattern()
      .getPattern()
      .getStopPattern()
      .getStop(1);
    assertEquals(modifiedStop, STOP_D);

    var patterns = List.copyOf(transitModel.getAllTripPatterns());

    var stops = patterns.stream().map(TripPattern::getStops).toList();
    assertEquals(List.of(List.of(STOP_A, STOP_D, STOP_C)), stops);
  }
}
