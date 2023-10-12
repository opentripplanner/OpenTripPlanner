package org.opentripplanner.ext.stopconsolidation;

import static org.opentripplanner.ext.stopconsolidation.TestStopConsolidationModel.PATTERN;
import static org.opentripplanner.ext.stopconsolidation.TestStopConsolidationModel.STOP_B;
import static org.opentripplanner.ext.stopconsolidation.TestStopConsolidationModel.STOP_D;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.stopconsolidation.internal.DefaultStopConsolidationRepository;
import org.opentripplanner.ext.stopconsolidation.model.ConsolidatedStopGroup;

class StopConsolidationModuleTest {

  @Test
  void replacePatterns() {
    var groups = List.of(new ConsolidatedStopGroup(STOP_D.getId(), List.of(STOP_B.getId())));

    var transitModel = TestStopConsolidationModel.buildTransitModel();
    transitModel.addTripPattern(PATTERN.getId(), PATTERN);
    var repo = new DefaultStopConsolidationRepository();
    var module = new StopConsolidationModule(transitModel, repo, groups);
    module.buildGraph();
  }
}
