package org.opentripplanner.ext.stopconsolidation;

import java.util.List;
import javax.annotation.Nonnull;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.service.TransitModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StopConsolidationModule implements GraphBuilderModule {

  private static final Logger LOG = LoggerFactory.getLogger(TripPattern.class);

  private final StopConsolidationModel model;
  private final TransitModel transitModel;

  public StopConsolidationModule(TransitModel transitModel) {
    this.transitModel = transitModel;
    this.model = new StopConsolidationModel(transitModel);
  }

  @Override
  public void buildGraph() {
    var stopsToReplace = model.stopIdsToReplace();
    var replacements = model.replacements();

    transitModel
      .getAllTripPatterns()
      .stream()
      .filter(pattern -> pattern.containsAnyStopId(stopsToReplace))
      .forEach(pattern -> {
        LOG.info("Replacing stop(s) in pattern {}", pattern);
        var modifiedPattern = modifyStopsInPattern(pattern, replacements);
        transitModel.addTripPattern(modifiedPattern.getId(), modifiedPattern);
      });
  }

  @Nonnull
  private TripPattern modifyStopsInPattern(
    TripPattern pattern,
    List<StopConsolidationModel.StopReplacement> replacements
  ) {
    var updatedStopPattern = pattern.getStopPattern().mutate();
    replacements.forEach(r -> updatedStopPattern.replaceStop(r.secondary(), r.primary()));
    return pattern.copy().withStopPattern(updatedStopPattern.build()).build();
  }
}
