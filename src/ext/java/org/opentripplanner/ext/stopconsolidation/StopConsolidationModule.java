package org.opentripplanner.ext.stopconsolidation;

import jakarta.inject.Inject;
import java.util.List;
import javax.annotation.Nonnull;
import org.opentripplanner.ext.stopconsolidation.internal.DefaultStopConsolidationService;
import org.opentripplanner.ext.stopconsolidation.model.StopReplacement;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.service.TransitModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StopConsolidationModule implements GraphBuilderModule {

  private static final Logger LOG = LoggerFactory.getLogger(TripPattern.class);

  private final StopConsolidationRepository repository;
  private final TransitModel transitModel;

  @Inject
  public StopConsolidationModule(
    TransitModel transitModel,
    StopConsolidationRepository repository
  ) {
    this.transitModel = transitModel;
    this.repository = repository;
  }

  @Override
  public void buildGraph() {
    var groups = StopConsolidationParser.parseGroups();
    repository.addGroups(groups);

    var service = new DefaultStopConsolidationService(repository, transitModel);

    var stopsToReplace = service.stopIdsToReplace();
    var replacements = service.replacements();

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
    List<StopReplacement> replacements
  ) {
    var updatedStopPattern = pattern.getStopPattern().mutate();
    replacements.forEach(r -> updatedStopPattern.replaceStop(r.secondary(), r.primary()));
    return pattern.copy().withStopPattern(updatedStopPattern.build()).build();
  }
}
