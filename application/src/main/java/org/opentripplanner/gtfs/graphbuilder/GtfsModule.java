package org.opentripplanner.gtfs.graphbuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opentripplanner.core.model.deduplicator.DeduplicatorService;
import org.opentripplanner.core.model.time.LocalDateRange;
import org.opentripplanner.ext.fares.service.gtfs.v1.GtfsFareServiceFactory;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.graph_builder.module.AddTransitEntitiesToGraph;
import org.opentripplanner.graph_builder.module.AddTransitEntitiesToTimetable;
import org.opentripplanner.graph_builder.module.TransitWithFutureDateValidator;
import org.opentripplanner.graph_builder.module.geometry.GeometryProcessor;
import org.opentripplanner.gtfs.GenerateTripPatternsOperation;
import org.opentripplanner.gtfs.interlining.InterlineProcessor;
import org.opentripplanner.gtfs.mapping.GTFSToTransitDataImportMapper;
import org.opentripplanner.model.TransitDataImport;
import org.opentripplanner.model.impl.TransitDataImportBuilder;
import org.opentripplanner.model.impl.TransitDataImportBuilder.StopTimeValidation;
import org.opentripplanner.model.impl.TripPatternGenerator;
import org.opentripplanner.routing.fares.FareServiceFactory;
import org.opentripplanner.service.streetdetails.StreetDetailsRepository;
import org.opentripplanner.service.streetdetails.internal.DefaultStreetDetailsRepository;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.transit.dataimport.TimetableDataImport;
import org.opentripplanner.transit.model.calendar.TripCalendars;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.service.TransitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GtfsModule implements GraphBuilderModule {

  private static final Logger LOG = LoggerFactory.getLogger(GtfsModule.class);
  /**
   * @see BuildConfig#transitServiceStart
   * @see BuildConfig#transitServiceEnd
   */
  private final LocalDateRange transitPeriodLimit;
  private final List<GtfsBundle> gtfsBundles;
  private final FareServiceFactory fareServiceFactory;

  private final TransitRepository transitRepository;
  private final TimetableDataImport timetableDataImport;
  private final StreetDetailsRepository streetDetailsRepository;
  private final Graph graph;
  private final DataImportIssueStore issueStore;
  private final DeduplicatorService deduplicator;

  private final double maxStopToShapeSnapDistance;
  private final double transitShapeSimplificationToleranceMeters;
  private final int subwayAccessTime_s;

  public GtfsModule(
    List<GtfsBundle> bundles,
    TransitRepository transitRepository,
    TimetableDataImport timetableDataImport,
    StreetDetailsRepository streetDetailsRepository,
    Graph graph,
    DeduplicatorService deduplicator,
    DataImportIssueStore issueStore,
    LocalDateRange transitPeriodLimit,
    FareServiceFactory fareServiceFactory,
    double maxStopToShapeSnapDistance,
    double transitShapeSimplificationToleranceMeters,
    int subwayAccessTime_s
  ) {
    this.gtfsBundles = bundles;
    this.transitRepository = transitRepository;
    this.timetableDataImport = timetableDataImport;
    this.streetDetailsRepository = streetDetailsRepository;
    this.graph = graph;
    this.deduplicator = deduplicator;
    this.issueStore = issueStore;
    this.transitPeriodLimit = transitPeriodLimit;
    this.fareServiceFactory = fareServiceFactory;
    this.maxStopToShapeSnapDistance = maxStopToShapeSnapDistance;
    this.transitShapeSimplificationToleranceMeters = transitShapeSimplificationToleranceMeters;
    this.subwayAccessTime_s = subwayAccessTime_s;
  }

  /**
   * Create a new instance for unit-testing.
   */
  public static GtfsModule forTest(
    List<GtfsBundle> bundles,
    TransitRepository transitRepository,
    TimetableDataImport timetableDataImport,
    Graph graph,
    LocalDateRange transitPeriodLimit
  ) {
    return new GtfsModule(
      bundles,
      transitRepository,
      timetableDataImport,
      new DefaultStreetDetailsRepository(),
      graph,
      new Deduplicator(),
      DataImportIssueStore.NOOP,
      transitPeriodLimit,
      new GtfsFareServiceFactory(),
      150.0,
      0.0,
      120
    );
  }

  @Override
  public void buildGraph() {
    // TODO - Cleanup the mapping and use of service-id. A GTFS feed service-id is NOT the same as
    //        the OTP domain model service-id.
    //        See https://github.com/opentripplanner/OpenTripPlanner/issues/7972.
    //        This applies to the body of this method and related code, e.g. fares and
    //        InterlineProcessor.

    TripCalendars tripCalendars = TripCalendars.empty();

    Map<String, GtfsBundle> feedIdsEncountered = new HashMap<>();

    try {
      for (GtfsBundle gtfsBundle : gtfsBundles) {
        var gtfsDao = gtfsBundle.loadDao();

        var feedId = gtfsBundle.getFeedId();
        verifyUniqueFeedId(gtfsBundle, feedIdsEncountered, feedId);

        feedIdsEncountered.put(feedId, gtfsBundle);

        var tripPatternGenerator = new GtfsTripPatternGenerator(
          deduplicator,
          maxStopToShapeSnapDistance,
          transitShapeSimplificationToleranceMeters
        );
        GTFSToTransitDataImportMapper mapper = new GTFSToTransitDataImportMapper(
          new TransitDataImportBuilder(
            transitRepository.getSiteRepository(),
            issueStore,
            transitPeriodLimit,
            StopTimeValidation.INTERPOLATE,
            tripPatternGenerator
          ),
          feedId,
          issueStore,
          gtfsBundle.parameters().discardMinTransferTimes(),
          gtfsBundle.parameters().stationTransferPreference()
        );
        mapper.mapStopTripAndRouteDataIntoBuilder(gtfsDao);

        TransitDataImportBuilder builder = mapper.getBuilder();
        var fareRulesData = mapper.fareRulesData();

        builder
          .tripCalendars()
          .listServiceIds()
          .forEach(sId ->
            fareRulesData.putServiceIds(sId, builder.tripCalendars().listServiceDates(sId))
          );

        mapper.mapAndAddTransfersToBuilder(gtfsDao);

        // NB! This call has side effects - it prunes/validates the builder's state, resolves
        // pending transfers and generates trip patterns, in that fixed order - see
        // TransitDataImportBuilder#build().
        TransitDataImport dataImport = builder.build();

        addTransitRepositoryToGraph(graph, transitRepository, streetDetailsRepository, dataImport);

        if (gtfsBundle.parameters().blockBasedInterlining()) {
          new InterlineProcessor(
            transitRepository.getConstrainedTransferService(),
            builder.getStaySeatedNotAllowed(),
            gtfsBundle.parameters().maxInterlineDistance(),
            issueStore,
            dataImport.getTripCalendars()
          ).run(dataImport.getTripPatterns());
        }

        fareServiceFactory.processGtfs(fareRulesData);

        tripCalendars = tripCalendars.merge(dataImport.getTripCalendars());
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    timetableDataImport.mergeTripCalendars(tripCalendars);
    TransitWithFutureDateValidator.validate(
      tripCalendars,
      issueStore,
      transitRepository.getTimeZone()
    );
  }

  /**
   * Verifies that a feed id is not assigned twice.
   * <p>
   * Duplicates can happen in the following cases:
   * <pre>
   *  - the feed id is configured twice in build-config.json
   *  - two GTFS feeds have the same feed_info.feed_id
   *  - a GTFS feed defines a feed_info.feed_id like '3' that collides with an auto-generated one
   * </pre>
   * Debugging these cases is very confusing, so we prevent it from happening.
   */
  private static void verifyUniqueFeedId(
    GtfsBundle gtfsBundle,
    Map<String, GtfsBundle> feedIdsEncountered,
    String feedId
  ) {
    if (feedIdsEncountered.containsKey(feedId)) {
      LOG.error(
        "Feed id '{}' has been used for {} but it was already assigned to {}.",
        feedId,
        gtfsBundle,
        feedIdsEncountered.get(feedId)
      );
      throw new IllegalArgumentException("Duplicate feed id: '%s'".formatted(feedId));
    }
  }

  @Override
  public void checkInputs() {
    for (GtfsBundle bundle : gtfsBundles) {
      bundle.checkInputs();
    }
  }

  /* Private Methods */

  private void addTransitRepositoryToGraph(
    Graph graph,
    TransitRepository transitRepository,
    StreetDetailsRepository streetDetailsRepository,
    TransitDataImport dataImport
  ) {
    AddTransitEntitiesToTimetable.addToTimetable(
      dataImport,
      transitRepository,
      timetableDataImport
    );
    AddTransitEntitiesToGraph.addToGraph(
      dataImport,
      subwayAccessTime_s,
      graph,
      streetDetailsRepository
    );
  }

  /**
   * The GTFS {@link TripPatternGenerator}.
   * <p>
   * This wraps {@link GenerateTripPatternsOperation} rather than having it implement {@link TripPatternGenerator}
   * directly, because its {@code calendarServiceIds} constructor argument can only be known once the builder has data
   * - but this generator is itself constructed and handed to {@link TransitDataImportBuilder}'s constructor before any
   * mapping has happened, so it must derive {@code builder.tripCalendars().listServiceIds()} at {@link #generate} time,
   * not upfront. Folding that derivation into {@code GenerateTripPatternsOperation} itself would remove the ability its
   * own unit tests rely on to inject an arbitrary {@code calendarServiceIds} independent of any real calendar data.
   */
  private static class GtfsTripPatternGenerator implements TripPatternGenerator {

    private final DeduplicatorService deduplicator;
    private final double maxStopToShapeSnapDistance;
    private final double transitShapeSimplificationToleranceMeters;

    private GtfsTripPatternGenerator(
      DeduplicatorService deduplicator,
      double maxStopToShapeSnapDistance,
      double transitShapeSimplificationToleranceMeters
    ) {
      this.deduplicator = deduplicator;
      this.maxStopToShapeSnapDistance = maxStopToShapeSnapDistance;
      this.transitShapeSimplificationToleranceMeters = transitShapeSimplificationToleranceMeters;
    }

    @Override
    public void generate(TransitDataImportBuilder builder, DataImportIssueStore issueStore) {
      // NB! run() has side effects: it adds trip patterns to builder.
      new GenerateTripPatternsOperation(
        builder,
        issueStore,
        deduplicator,
        builder.tripCalendars().listServiceIds(),
        new GeometryProcessor(
          builder,
          maxStopToShapeSnapDistance,
          transitShapeSimplificationToleranceMeters,
          issueStore
        )
      ).run();
    }
  }
}
