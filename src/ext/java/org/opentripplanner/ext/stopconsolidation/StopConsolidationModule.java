package org.opentripplanner.ext.stopconsolidation;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.ext.stopconsolidation.internal.DefaultStopConsolidationService;
import org.opentripplanner.ext.stopconsolidation.model.ConsolidatedStopGroup;
import org.opentripplanner.ext.stopconsolidation.model.StopReplacement;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.service.TransitModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A graph build module that takes a list of "consolidated" stops (stops from several feeds
 * that represent the same stop place) and swaps the "secondary" stops in patterns with their
 * "primary" equivalent.
 * <p>
 * NOTE: This will make real-time trip updates for a modified pattern a lot harder. For Arcadis'
 * initial implementation this is acceptable and will serve as encouragement for the data producers to
 * produce a consolidated transit feed rather than relying on this feature.
 */
public class StopConsolidationModule implements GraphBuilderModule {

  private static final Logger LOG = LoggerFactory.getLogger(TripPattern.class);

  private final StopConsolidationRepository repository;
  private final TransitModel transitModel;
  private final Collection<ConsolidatedStopGroup> groups;

  public StopConsolidationModule(
    TransitModel transitModel,
    StopConsolidationRepository repository,
    Collection<ConsolidatedStopGroup> groups
  ) {
    this.transitModel = Objects.requireNonNull(transitModel);
    this.repository = Objects.requireNonNull(repository);
    this.groups = Objects.requireNonNull(groups);
  }

  @Override
  public void buildGraph() {
    repository.addGroups(groups);

    var service = new DefaultStopConsolidationService(repository, transitModel);

    var stopsToReplace = service.secondaryStops();
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
    var updatedStopPattern = pattern.copyPlannedStopPattern();
    replacements.forEach(r -> updatedStopPattern.replaceStop(r.secondary(), r.primary()));
    return pattern.copy().withStopPattern(updatedStopPattern.build()).build();
  }

  public static StopConsolidationModule of(
    TransitModel transitModel,
    StopConsolidationRepository repo,
    DataSource ds
  ) {
    LOG.info("Reading stop consolidation information from '{}'", ds);
    try (var inputStream = ds.asInputStream()) {
      var groups = StopConsolidationParser.parseGroups(inputStream);
      return new StopConsolidationModule(transitModel, repo, groups);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
