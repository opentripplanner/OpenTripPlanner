package org.opentripplanner.gtfs.graphbuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.onebusaway.gtfs.impl.GtfsRelationalDaoImpl;
import org.onebusaway.gtfs.model.Area;
import org.onebusaway.gtfs.model.FareLegRule;
import org.onebusaway.gtfs.model.FareMedium;
import org.onebusaway.gtfs.model.FareProduct;
import org.onebusaway.gtfs.model.FareTransferRule;
import org.onebusaway.gtfs.model.RiderCategory;
import org.onebusaway.gtfs.model.RouteNetworkAssignment;
import org.onebusaway.gtfs.model.StopAreaElement;
import org.onebusaway.gtfs.serialization.GtfsReader;
import org.onebusaway.gtfs.services.GtfsRelationalDao;
import org.opentripplanner.ext.fares.impl.gtfs.DefaultFareServiceFactory;
import org.opentripplanner.ext.flex.FlexTripsMapper;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.graph_builder.module.AddTransitEntitiesToGraph;
import org.opentripplanner.graph_builder.module.AddTransitEntitiesToTimetable;
import org.opentripplanner.graph_builder.module.ValidateAndInterpolateStopTimesForEachTrip;
import org.opentripplanner.graph_builder.module.geometry.GeometryProcessor;
import org.opentripplanner.gtfs.GenerateTripPatternsOperation;
import org.opentripplanner.gtfs.interlining.InterlineProcessor;
import org.opentripplanner.gtfs.mapping.GTFSToOtpTransitServiceMapper;
import org.opentripplanner.model.OtpTransitService;
import org.opentripplanner.model.TripStopTimes;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.model.calendar.ServiceDateInterval;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.routing.fares.FareServiceFactory;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.DeduplicatorService;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.service.TimetableRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GtfsModule implements GraphBuilderModule {

  public static final Set<Class<?>> FARES_V2_CLASSES = Set.of(
    Area.class,
    FareProduct.class,
    FareLegRule.class,
    FareMedium.class,
    FareTransferRule.class,
    RiderCategory.class,
    RouteNetworkAssignment.class,
    StopAreaElement.class
  );

  private static final Logger LOG = LoggerFactory.getLogger(GtfsModule.class);
  /**
   * @see BuildConfig#transitServiceStart
   * @see BuildConfig#transitServiceEnd
   */
  private final ServiceDateInterval transitPeriodLimit;
  private final List<GtfsBundle> gtfsBundles;
  private final FareServiceFactory fareServiceFactory;

  private final TimetableRepository timetableRepository;
  private final Graph graph;
  private final DataImportIssueStore issueStore;
  private final DeduplicatorService deduplicator;

  private final double maxStopToShapeSnapDistance;
  private final int subwayAccessTime_s;

  public GtfsModule(
    List<GtfsBundle> bundles,
    TimetableRepository timetableRepository,
    Graph graph,
    DeduplicatorService deduplicator,
    DataImportIssueStore issueStore,
    ServiceDateInterval transitPeriodLimit,
    FareServiceFactory fareServiceFactory,
    double maxStopToShapeSnapDistance,
    int subwayAccessTime_s
  ) {
    this.gtfsBundles = bundles;
    this.timetableRepository = timetableRepository;
    this.graph = graph;
    this.deduplicator = deduplicator;
    this.issueStore = issueStore;
    this.transitPeriodLimit = transitPeriodLimit;
    this.fareServiceFactory = fareServiceFactory;
    this.maxStopToShapeSnapDistance = maxStopToShapeSnapDistance;
    this.subwayAccessTime_s = subwayAccessTime_s;
  }

  /**
   * Create a new instance for unit-testing.
   */
  public static GtfsModule forTest(
    List<GtfsBundle> bundles,
    TimetableRepository timetableRepository,
    Graph graph,
    ServiceDateInterval transitPeriodLimit
  ) {
    return new GtfsModule(
      bundles,
      timetableRepository,
      graph,
      new Deduplicator(),
      DataImportIssueStore.NOOP,
      transitPeriodLimit,
      new DefaultFareServiceFactory(),
      150.0,
      120
    );
  }

  @Override
  public void buildGraph() {
    CalendarServiceData calendarServiceData = new CalendarServiceData();

    boolean hasTransit = false;

    Map<String, GtfsBundle> feedIdsEncountered = new HashMap<>();

    try {
      for (GtfsBundle gtfsBundle : gtfsBundles) {
        var gtfsDao = loadBundle(gtfsBundle);

        var feedId = gtfsBundle.getFeedId();
        verifyUniqueFeedId(gtfsBundle, feedIdsEncountered, feedId);

        feedIdsEncountered.put(feedId, gtfsBundle);

        GTFSToOtpTransitServiceMapper mapper = new GTFSToOtpTransitServiceMapper(
          new OtpTransitServiceBuilder(timetableRepository.getSiteRepository(), issueStore),
          feedId,
          issueStore,
          gtfsBundle.parameters().discardMinTransferTimes(),
          gtfsBundle.parameters().stationTransferPreference()
        );
        mapper.mapStopTripAndRouteDataIntoBuilder(gtfsDao);

        OtpTransitServiceBuilder builder = mapper.getBuilder();
        var fareRulesData = mapper.fareRulesData();

        builder.limitServiceDays(transitPeriodLimit);

        calendarServiceData.add(builder.buildCalendarServiceData());

        if (OTPFeature.FlexRouting.isOn()) {
          builder.getFlexTripsById().addAll(FlexTripsMapper.createFlexTrips(builder, issueStore));
        }

        validateAndInterpolateStopTimesForEachTrip(builder.getStopTimesSortedByTrip(), issueStore);

        // We need to run this after the cleaning of the data, as stop indices might have changed
        mapper.mapAndAddTransfersToBuilder(gtfsDao);

        GeometryProcessor geometryProcessor = new GeometryProcessor(
          builder,
          maxStopToShapeSnapDistance,
          issueStore
        );

        // NB! The calls below have side effects - the builder state is updated!
        createTripPatterns(
          deduplicator,
          timetableRepository,
          builder,
          calendarServiceData.getServiceIds(),
          geometryProcessor,
          issueStore
        );

        OtpTransitService otpTransitService = builder.build();

        // if this or previously processed gtfs bundle has transit that has not been filtered out
        hasTransit = hasTransit || otpTransitService.hasActiveTransit();

        addTimetableRepositoryToGraph(graph, timetableRepository, otpTransitService);

        if (gtfsBundle.parameters().blockBasedInterlining()) {
          new InterlineProcessor(
            timetableRepository.getTransferService(),
            builder.getStaySeatedNotAllowed(),
            gtfsBundle.parameters().maxInterlineDistance(),
            issueStore,
            calendarServiceData
          ).run(otpTransitService.getTripPatterns());
        }

        fareServiceFactory.processGtfs(fareRulesData, otpTransitService);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    timetableRepository.validateTimeZones();

    timetableRepository.updateCalendarServiceData(hasTransit, calendarServiceData, issueStore);
  }

  /**
   * Verifies that a feed id is not assigned twice.
   * <p>
   * Duplicates can happen in the following cases:
   *  - the feed id is configured twice in build-config.json
   *  - two GTFS feeds have the same feed_info.feed_id
   *  - a GTFS feed defines a feed_info.feed_id like '3' that collides with an auto-generated one
   * <p>
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

  /**
   * This method has side effects, the {@code stopTimesByTrip} is updated.
   */
  private void validateAndInterpolateStopTimesForEachTrip(
    TripStopTimes stopTimesByTrip,
    DataImportIssueStore issueStore
  ) {
    new ValidateAndInterpolateStopTimesForEachTrip(stopTimesByTrip, true, issueStore).run();
  }

  /**
   * This method has side effects, the {@code builder} is updated with new TripPatterns.
   */
  private void createTripPatterns(
    DeduplicatorService deduplicator,
    TimetableRepository timetableRepository,
    OtpTransitServiceBuilder builder,
    Set<FeedScopedId> calServiceIds,
    GeometryProcessor geometryProcessor,
    DataImportIssueStore issueStore
  ) {
    GenerateTripPatternsOperation buildTPOp = new GenerateTripPatternsOperation(
      builder,
      issueStore,
      deduplicator,
      calServiceIds,
      geometryProcessor
    );
    buildTPOp.run();
    timetableRepository.setHasFrequencyService(
      timetableRepository.hasFrequencyService() || buildTPOp.hasFrequencyBasedTrips()
    );
    timetableRepository.setHasScheduledService(
      timetableRepository.hasScheduledService() || buildTPOp.hasScheduledTrips()
    );
  }

  private void addTimetableRepositoryToGraph(
    Graph graph,
    TimetableRepository timetableRepository,
    OtpTransitService otpTransitService
  ) {
    AddTransitEntitiesToTimetable.addToTimetable(otpTransitService, timetableRepository);
    AddTransitEntitiesToGraph.addToGraph(otpTransitService, subwayAccessTime_s, graph);
  }

  private GtfsRelationalDao loadBundle(GtfsBundle gtfsBundle) throws IOException {
    var dao = new GtfsRelationalDaoImpl();
    dao.setPackShapePoints(true);
    LOG.info("reading {}", gtfsBundle.feedInfo());

    String gtfsFeedId = gtfsBundle.getFeedId();

    GtfsReader reader = new GtfsReader();
    reader.setInputSource(gtfsBundle.getCsvInputSource());
    reader.setEntityStore(dao);
    reader.setInternStrings(true);
    reader.setDefaultAgencyId(gtfsFeedId);

    dao.open();
    for (Class<?> entityClass : reader.getEntityClasses()) {
      if (skipEntityClass(entityClass)) {
        LOG.info("Skipping entity: {}", entityClass.getName());
        continue;
      }
      LOG.info("Reading entity: {}", entityClass.getName());
      reader.readEntities(entityClass);
    }

    dao.close();
    return dao;
  }

  /**
   * Since GTFS Fares V2 is a very new, constantly evolving standard there might be a lot of errors
   * in the data. We only want to try to parse them when the feature flag is explicitly enabled as
   * it can easily lead to graph build failures.
   */
  private boolean skipEntityClass(Class<?> entityClass) {
    return OTPFeature.FaresV2.isOff() && FARES_V2_CLASSES.contains(entityClass);
  }
}
