package org.opentripplanner.gtfs;

import java.io.File;
import java.io.IOException;
import javax.annotation.Nullable;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.module.GtfsFeedId;
import org.opentripplanner.graph_builder.module.ValidateAndInterpolateStopTimesForEachTrip;
import org.opentripplanner.graph_builder.module.geometry.GeometryProcessor;
import org.opentripplanner.gtfs.graphbuilder.GtfsModule;
import org.opentripplanner.gtfs.mapping.GTFSToOtpTransitServiceMapper;
import org.opentripplanner.model.OtpTransitService;
import org.opentripplanner.model.calendar.CalendarService;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.model.calendar.impl.CalendarServiceImpl;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.site.StopTransferPriority;
import org.opentripplanner.transit.service.SiteRepository;

/**
 * This class helps building GtfsContext and post process the GtfsDao by repairing
 * StopTimes(optional) and generating TripPatterns(optional). This done in the {@link GtfsModule} in
 * the production code.
 */
public class GtfsContextBuilder {

  private final GtfsFeedId feedId;

  private final OtpTransitServiceBuilder transitBuilder;
  private CalendarService calendarService = null;
  private DataImportIssueStore issueStore = null;
  private Deduplicator deduplicator;

  public GtfsContextBuilder(GtfsFeedId feedId, OtpTransitServiceBuilder transitBuilder) {
    this.feedId = feedId;
    this.transitBuilder = transitBuilder;
  }

  public static GtfsContextBuilder contextBuilder(File file) throws IOException {
    return contextBuilder(null, file);
  }

  public static GtfsContextBuilder contextBuilder(@Nullable String defaultFeedId, File path)
    throws IOException {
    var transitBuilder = new OtpTransitServiceBuilder(
      new SiteRepository(),
      DataImportIssueStore.NOOP
    );
    GtfsImport gtfsImport = gtfsImport(defaultFeedId, path);
    GtfsFeedId feedId = gtfsImport.getFeedId();
    var mapper = new GTFSToOtpTransitServiceMapper(
      transitBuilder,
      feedId.getId(),
      DataImportIssueStore.NOOP,
      false,
      gtfsImport.getDao(),
      StopTransferPriority.ALLOWED
    );
    mapper.mapStopTripAndRouteDataIntoBuilder();
    mapper.mapAndAddTransfersToBuilder();
    return new GtfsContextBuilder(feedId, transitBuilder).withDataImportIssueStore(
      DataImportIssueStore.NOOP
    );
  }

  public OtpTransitServiceBuilder getTransitBuilder() {
    return transitBuilder;
  }

  public GtfsContextBuilder withIssueStoreAndDeduplicator(Graph graph) {
    return withIssueStoreAndDeduplicator(graph, DataImportIssueStore.NOOP);
  }

  public GtfsContextBuilder withIssueStoreAndDeduplicator(
    Graph graph,
    DataImportIssueStore issueStore
  ) {
    return withDataImportIssueStore(issueStore).withDeduplicator(graph.deduplicator);
  }

  public GtfsContextBuilder withDataImportIssueStore(DataImportIssueStore issueStore) {
    this.issueStore = issueStore;
    return this;
  }

  private GtfsContextBuilder withDeduplicator(Deduplicator deduplicator) {
    this.deduplicator = deduplicator;
    return this;
  }

  /**
   * This method will:
   * <ol>
   *     <li>generate TripPatterns (if enabled)</li>
   *     <li>create a new context</li>
   * </ol>
   */
  public GtfsContext build() {
    repairStopTimesAndGenerateTripPatterns();
    return new GtfsContextImpl(feedId, transitBuilder);
  }

  /**
   * By default this method is part of the {@link #build()} method. But in cases where you want to
   * change the dao after building the context, and these changes will affect the TripPatterns
   * generation, you should do the following:
   *
   * <pre>
   * GtfsContextBuilder contextBuilder = &lt;create context builder>;
   *
   * // turn off TripPatterns generation before building
   * context = contextBuilder
   *     .turnOffRepairStopTimesAndTripPatternsGeneration()
   *     .build();
   *
   * // Do your changes
   * applyChanges(context.getDao());
   *
   * // Repair StopTimes and generate TripPatterns
   * contextBuilder.repairStopTimesAndGenerateTripPatterns();
   * </pre>
   */
  public void repairStopTimesAndGenerateTripPatterns() {
    repairStopTimesForEachTrip();
    generateTripPatterns();
  }

  /* private stuff */

  private static GtfsImport gtfsImport(String defaultFeedId, File file) throws IOException {
    return new GtfsImport(defaultFeedId, file);
  }

  private void repairStopTimesForEachTrip() {
    new ValidateAndInterpolateStopTimesForEachTrip(
      transitBuilder.getStopTimesSortedByTrip(),
      true,
      true,
      issueStore
    ).run();
  }

  private void generateTripPatterns() {
    new GenerateTripPatternsOperation(
      transitBuilder,
      issueStore,
      deduplicator(),
      calendarService().getServiceIds(),
      new GeometryProcessor(transitBuilder, 150, issueStore)
    ).run();
  }

  private CalendarService calendarService() {
    if (calendarService == null) {
      calendarService = new CalendarServiceImpl(transitBuilder.buildCalendarServiceData());
    }
    return calendarService;
  }

  private Deduplicator deduplicator() {
    if (deduplicator == null) {
      deduplicator = new Deduplicator();
    }
    return deduplicator;
  }

  private static class GtfsContextImpl implements GtfsContext {

    private final GtfsFeedId feedId;
    private final OtpTransitService transitService;
    private final CalendarServiceData calendarServiceData;

    private GtfsContextImpl(GtfsFeedId feedId, OtpTransitServiceBuilder builder) {
      this.feedId = feedId;
      this.calendarServiceData = builder.buildCalendarServiceData();
      this.transitService = builder.build();
    }

    @Override
    public GtfsFeedId getFeedId() {
      return feedId;
    }

    @Override
    public OtpTransitService getTransitService() {
      return transitService;
    }

    @Override
    public CalendarServiceData getCalendarServiceData() {
      return calendarServiceData;
    }
  }
}
