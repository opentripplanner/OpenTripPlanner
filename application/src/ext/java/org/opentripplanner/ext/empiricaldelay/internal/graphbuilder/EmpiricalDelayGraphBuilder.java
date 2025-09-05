package org.opentripplanner.ext.empiricaldelay.internal.graphbuilder;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.opentripplanner.ext.empiricaldelay.EmpiricalDelayRepository;
import org.opentripplanner.ext.empiricaldelay.internal.csvinput.EmpiricalDelayCsvDataReader;
import org.opentripplanner.ext.empiricaldelay.internal.model.TripDelaysDto;
import org.opentripplanner.ext.empiricaldelay.parameters.EmpiricalDelayFeedParameters;
import org.opentripplanner.ext.empiricaldelay.parameters.EmpiricalDelayParameters;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.model.ConfiguredCompositeDataSource;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.TimetableRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class allows updating the graph with emissions data from external emissions data files.
 */
public class EmpiricalDelayGraphBuilder implements GraphBuilderModule {

  private static final Logger LOG = LoggerFactory.getLogger(EmpiricalDelayGraphBuilder.class);

  private final Iterable<ConfiguredCompositeDataSource<EmpiricalDelayFeedParameters>> dataSources;
  private final Deduplicator deduplicator;
  private final DataImportIssueStore issueStore;
  private final EmpiricalDelayParameters parameters;
  private final EmpiricalDelayRepository repository;
  private final TimetableRepository timetableRepository;

  public EmpiricalDelayGraphBuilder(
    Iterable<ConfiguredCompositeDataSource<EmpiricalDelayFeedParameters>> dataSources,
    Deduplicator deduplicator,
    DataImportIssueStore issueStore,
    EmpiricalDelayParameters parameters,
    EmpiricalDelayRepository repository,
    TimetableRepository timetableRepository
  ) {
    this.dataSources = Objects.requireNonNull(dataSources);
    this.deduplicator = Objects.requireNonNull(deduplicator);
    this.issueStore = Objects.requireNonNull(issueStore);
    this.parameters = Objects.requireNonNull(parameters);
    this.repository = Objects.requireNonNull(repository);
    this.timetableRepository = Objects.requireNonNull(timetableRepository);
  }

  public void buildGraph() {
    // This method does not have unit tests - so delegate as much as posible

    if (parameters == null) {
      return;
    }
    var mapper = new TripDelaysMapper(
      createStopIdsByTripIdMap(timetableRepository.getAllTripPatterns()),
      issueStore,
      deduplicator
    );

    for (var data : dataSources) {
      var reader = new EmpiricalDelayCsvDataReader(issueStore);
      reader.read(data.dataSource(), data.config().feedId());

      // Add calendar data
      repository.addEmpiricalDelayServiceCalendar(data.config().feedId(), reader.calendar());

      // Validate and add trip delays
      for (TripDelaysDto trip : reader.trips()) {
        mapper.map(trip).ifPresent(it -> repository.addTripDelays(it));
      }
    }
    LOG.info(repository.summary());
  }

  /** Package local so we can test it */
  static Map<FeedScopedId, List<FeedScopedId>> createStopIdsByTripIdMap(
    Collection<TripPattern> tripPatterns
  ) {
    var map = new HashMap<FeedScopedId, List<FeedScopedId>>();
    for (var pattern : tripPatterns) {
      pattern
        .scheduledTripsAsStream()
        .forEach(it ->
          map.put(it.getId(), pattern.getStops().stream().map(StopLocation::getId).toList())
        );
    }
    return map;
  }
}
