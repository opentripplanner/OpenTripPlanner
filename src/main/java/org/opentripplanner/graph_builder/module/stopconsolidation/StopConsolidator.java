package org.opentripplanner.graph_builder.module.stopconsolidation;

import static org.opentripplanner.transit.model.framework.FeedScopedId.parseId;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.TransitModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StopConsolidator implements GraphBuilderModule {

  private static final Logger LOG = LoggerFactory.getLogger(TripPattern.class);
  private static final IdPair TO_REPLACE = new IdPair(
    parseId("kcm:82718"),
    parseId("commtrans:1010")
  );

  private final TransitModel transitModel;

  public StopConsolidator(TransitModel transitModel) {
    this.transitModel = transitModel;
  }

  @Override
  public void buildGraph() {
    var replacements = Stream
      .of(TO_REPLACE)
      .map(r -> {
        var primaryStop = transitModel.getStopModel().getStopLocation(r.primary());
        Objects.requireNonNull(primaryStop, "No stop with id %s".formatted(r.child()));
        return new StopReplacement(primaryStop, r.child);
      })
      .toList();

    var stopsToReplace = replacements.stream().map(StopReplacement::child).toList();

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
    replacements.forEach(r -> updatedStopPattern.replaceStop(r.child, r.primary));
    return pattern.copy().withStopPattern(updatedStopPattern.build()).build();
  }

  record IdPair(FeedScopedId primary, FeedScopedId child) {
    public IdPair {
      Objects.requireNonNull(primary);
      Objects.requireNonNull(child);
    }
  }

  record StopReplacement(StopLocation primary, FeedScopedId child) {}
}
