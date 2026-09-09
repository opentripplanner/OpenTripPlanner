package org.opentripplanner.gtfs;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.module.ValidateAndInterpolateStopTimesForEachTrip;
import org.opentripplanner.graph_builder.module.geometry.GeometryProcessor;
import org.opentripplanner.gtfs.graphbuilder.GtfsModule;
import org.opentripplanner.gtfs.mapping.GTFSToTransitDataImportMapper;
import org.opentripplanner.model.impl.TransitDataImportBuilder;
import org.opentripplanner.transit.model.calendar.TripCalendars;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.site.StopTransferPriority;
import org.opentripplanner.transit.service.SiteRepository;

/**
 * This class helps building GtfsContext and post process the GtfsDao by repairing
 * StopTimes(optional) and generating TripPatterns(optional). This done in the {@link GtfsModule} in
 * the production code.
 */
public class GtfsContextBuilder {

  private final String feedId;

  private final TransitDataImportBuilder transitBuilder;
  private Set<FeedScopedId> serviceIds = null;
  private DataImportIssueStore issueStore = null;
  private Deduplicator deduplicator;

  public GtfsContextBuilder(String feedId, TransitDataImportBuilder transitBuilder) {
    this.feedId = feedId;
    this.transitBuilder = transitBuilder;
  }

  public static GtfsContextBuilder contextBuilder(File file) throws IOException {
    return contextBuilder(null, file);
  }

  public static GtfsContextBuilder contextBuilder(@Nullable String defaultFeedId, File path)
    throws IOException {
    var transitBuilder = new TransitDataImportBuilder(
      new SiteRepository(),
      DataImportIssueStore.NOOP
    );
    GtfsImport gtfsImport = gtfsImport(defaultFeedId, path);
    String feedId = gtfsImport.getFeedId();
    var mapper = new GTFSToTransitDataImportMapper(
      transitBuilder,
      feedId,
      DataImportIssueStore.NOOP,
      false,
      StopTransferPriority.ALLOWED
    );
    mapper.mapStopTripAndRouteDataIntoBuilder(gtfsImport.getDao());
    mapper.mapAndAddTransfersToBuilder(gtfsImport.getDao());
    return new GtfsContextBuilder(feedId, transitBuilder).withDataImportIssueStore(
      DataImportIssueStore.NOOP
    );
  }

  public TransitDataImportBuilder getTransitBuilder() {
    return transitBuilder;
  }

  public GtfsContextBuilder withDataImportIssueStore(DataImportIssueStore issueStore) {
    this.issueStore = issueStore;
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
    var tripCalendars = transitBuilder.tripCalendars().build();
    return new GtfsContextImpl(feedId, tripCalendars);
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
      issueStore
    ).run();
  }

  private void generateTripPatterns() {
    new GenerateTripPatternsOperation(
      transitBuilder,
      issueStore,
      deduplicator(),
      serviceIds(),
      new GeometryProcessor(transitBuilder, 150, issueStore)
    ).run();
  }

  private Set<FeedScopedId> serviceIds() {
    if (serviceIds == null) {
      serviceIds = transitBuilder.tripCalendars().listServiceIds();
    }
    return serviceIds;
  }

  private Deduplicator deduplicator() {
    if (deduplicator == null) {
      deduplicator = new Deduplicator();
    }
    return deduplicator;
  }

  private static class GtfsContextImpl implements GtfsContext {

    private final String feedId;
    private final TripCalendars tripCalendars;

    private GtfsContextImpl(String feedId, TripCalendars tripCalendars) {
      this.feedId = feedId;
      this.tripCalendars = tripCalendars;
    }

    @Override
    public String getFeedId() {
      return feedId;
    }

    @Override
    public TripCalendars getTripCalendars() {
      return tripCalendars;
    }
  }
}
