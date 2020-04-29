package org.opentripplanner.gtfs;

import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.graph_builder.module.GtfsFeedId;
import org.opentripplanner.graph_builder.module.GtfsModule;
import org.opentripplanner.model.OtpTransitService;
import org.opentripplanner.model.calendar.CalendarService;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.model.calendar.impl.CalendarServiceImpl;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.trippattern.Deduplicator;

import java.io.File;
import java.io.IOException;

import static org.opentripplanner.gtfs.mapping.GTFSToOtpTransitServiceMapper.mapGtfsDaoToInternalTransitServiceBuilder;

/**
 * This class helps building GtfsContext and post process
 * the GtfsDao by repairing StopTimes(optional) and generating TripPatterns(optional).
 * This done in the {@link GtfsModule} in the production code.
 */
public class GtfsContextBuilder {

    private final GtfsFeedId feedId;

    private final OtpTransitServiceBuilder transitBuilder;

    private CalendarService calendarService = null;

    private DataImportIssueStore issueStore = null;

    private Deduplicator deduplicator;

    private boolean repairStopTimesAndGenerateTripPatterns = true;

    public static GtfsContextBuilder contextBuilder(String path) throws IOException {
        return contextBuilder(null, path);
    }

    public static GtfsContextBuilder contextBuilder(String defaultFeedId, String path) throws IOException {
        GtfsImport gtfsImport = gtfsImport(defaultFeedId, path);
        GtfsFeedId feedId = gtfsImport.getFeedId();
        OtpTransitServiceBuilder transitBuilder = mapGtfsDaoToInternalTransitServiceBuilder(
                gtfsImport.getDao(), feedId.getId(), new DataImportIssueStore(false)
        );
        return new GtfsContextBuilder(
                feedId,
                transitBuilder).withDataImportIssueStore(new DataImportIssueStore(false)
        );
    }

    public GtfsContextBuilder(GtfsFeedId feedId, OtpTransitServiceBuilder transitBuilder) {
        this.feedId = feedId;
        this.transitBuilder = transitBuilder;
    }

    public GtfsFeedId getFeedId() {
        return feedId;
    }

    public OtpTransitServiceBuilder getTransitBuilder() {
        return transitBuilder;
    }

    public GtfsContextBuilder withIssueStoreAndDeduplicator(
            Graph graph
    ) {
        return withIssueStoreAndDeduplicator(
                graph,
                new DataImportIssueStore(false)
        );
    }

    public GtfsContextBuilder withIssueStoreAndDeduplicator(
            Graph graph,
            DataImportIssueStore issueStore
    ) {
        return withDataImportIssueStore(issueStore)
                .withDeduplicator(graph.deduplicator);
    }

    public GtfsContextBuilder withDataImportIssueStore(DataImportIssueStore issueStore) {
        this.issueStore = issueStore;
        return this;
    }

    public GtfsContextBuilder withDeduplicator(Deduplicator deduplicator) {
        this.deduplicator = deduplicator;
        return this;
    }

    /**
     * The {@link org.opentripplanner.graph_builder.module.GtfsModule} is responsible for repairing
     * StopTimes for all trips and trip patterns generation, so turn this feature <b>off</b>
     * when using GtfsModule to load data.
     *
     * This feature is turned <b>on</b> by <em>default</em>.
     */
    public GtfsContextBuilder turnOffRepairStopTimesAndTripPatternsGeneration() {
        this.repairStopTimesAndGenerateTripPatterns = false;
        return this;
    }

    /**
     * This method will:
     * <ol>
     *     <li>repair stop-times (if enabled)</li>
     *     <li>generate TripPatterns (if enabled)</li>
     *     <li>create a new context</li>
     * </ol>
     */
    public GtfsContext build() {
        if(repairStopTimesAndGenerateTripPatterns) {
            repairStopTimesAndGenerateTripPatterns();
        }
        return new GtfsContextImpl(feedId, transitBuilder);
    }

    /**
     * By default this method is part of the {@link #build()} method.
     * But in cases where you want to change the dao after building the
     * context, and these changes will affect the TripPatterns generation,
     * you should do the following:
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

    private void repairStopTimesForEachTrip() {
        new RepairStopTimesForEachTripOperation(
                transitBuilder.getStopTimesSortedByTrip(), issueStore
        ).run();
    }

    private void generateTripPatterns() {
        new GenerateTripPatternsOperation(
                transitBuilder, issueStore,
                deduplicator(),
                calendarService().getServiceIds()
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

    private static GtfsImport gtfsImport(String defaultFeedId, String path) throws IOException {
        return new GtfsImport(defaultFeedId, new File(path));
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

        @Override public OtpTransitService getTransitService() {
            return transitService;
        }

        @Override public CalendarServiceData getCalendarServiceData() {
            return calendarServiceData;
        }
    }
}
