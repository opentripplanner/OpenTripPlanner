package org.opentripplanner.graph_builder.module.stopconsolidation;

import java.util.List;
import java.util.stream.Stream;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.TransitModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for making sure, that all trips have arrival and departure times for
 * all stops. It also removes all stop times for trips, which have invalid stop times.
 */
public class StopConsolidator implements GraphBuilderModule {

  private static final Logger LOG = LoggerFactory.getLogger(StopConsolidator.class);
  private static final IdPair TO_REPLACE = new IdPair(new FeedScopedId("commtrans", "123"), new FeedScopedId("foo", "bar"));

  private final TransitModel transitModel;
  private final List<StopReplacement> replacements;

  public StopConsolidator(TransitModel transitModel) {
    this.transitModel = transitModel;

    this.replacements = Stream.of(TO_REPLACE).map(r -> {
      var stop = transitModel.getStopModel().getStopLocation(r.child());
      return new StopReplacement(r.primary, stop);
    }).toList();
  }


  @Override
  public void buildGraph() {
    transitModel
      .getAllTripPatterns()
      .forEach(pattern -> {
        var updatedStopPattern = pattern.getStopPattern().mutate().replaceStop(null, null).build();
        var updatedTripPattern = TripPattern
          .of(pattern.getId())
          .withStopPattern(updatedStopPattern)
          .withRoute(pattern.getRoute())
          .withNetexSubmode(pattern.getNetexSubmode())
          .build();

        transitModel.addTripPattern(updatedTripPattern.getId(), updatedTripPattern);
      });
  }

  record IdPair(FeedScopedId primary, FeedScopedId child) {}
  record StopReplacement(FeedScopedId primary, StopLocation child) {}
}
