package org.opentripplanner.gtfs;

import org.opentripplanner.graph_builder.annotation.GraphBuilderAnnotation;
import org.opentripplanner.graph_builder.module.GtfsFeedId;
import org.opentripplanner.graph_builder.module.GtfsModule;
import org.opentripplanner.model.CalendarService;
import org.opentripplanner.model.FareAttribute;
import org.opentripplanner.model.Pathway;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.ServiceCalendar;
import org.opentripplanner.model.ServiceCalendarDate;
import org.opentripplanner.model.ShapePoint;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.routing.graph.AddBuilderAnnotation;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.trippattern.Deduplicator;

import java.io.File;
import java.io.IOException;

import static org.opentripplanner.calendar.impl.CalendarServiceDataFactoryImpl.createCalendarService;
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

    private AddBuilderAnnotation addBuilderAnnotation = null;

    private Deduplicator deduplicator;

    private boolean repairStopTimesAndGenerateTripPatterns = true;

    private boolean setAgencyToFeedIdForAllElements = false;

    public static GtfsContextBuilder contextBuilder(String path) throws IOException {
        return contextBuilder(null, path);
    }

    public static GtfsContextBuilder contextBuilder(String defaultFeedId, String path) throws IOException {
        GtfsImport gtfsImport = gtfsImport(defaultFeedId, path);
        GtfsFeedId feedId = gtfsImport.getFeedId();
        OtpTransitServiceBuilder transitBuilder = mapGtfsDaoToInternalTransitServiceBuilder(gtfsImport.getDao());
        return new GtfsContextBuilder(feedId, transitBuilder);
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

    public GtfsContextBuilder withGraphBuilderAnnotationsAndDeduplicator(Graph graph) {
        return withAddBuilderAnnotation(graph)
                .withDeduplicator(graph.deduplicator);
    }

    public GtfsContextBuilder withAddBuilderAnnotation(AddBuilderAnnotation addBuilderAnnotation) {
        this.addBuilderAnnotation = addBuilderAnnotation;
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

    public GtfsContextBuilder turnOnSetAgencyToFeedIdForAllElements() {
        this.setAgencyToFeedIdForAllElements = true;
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
        if (setAgencyToFeedIdForAllElements) {
            setAgencyToFeedIdForAllElements();
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

    private void setAgencyToFeedIdForAllElements() {

        for (ShapePoint shapePoint : transitBuilder.getShapePoints()) {
            shapePoint.getShapeId().setAgencyId(this.feedId.getId());
        }
        for (Route route : transitBuilder.getRoutes().values()) {
            route.getId().setAgencyId(this.feedId.getId());
        }
        for (Stop stop : transitBuilder.getStops().values()) {
            stop.getId().setAgencyId(this.feedId.getId());
        }

        for (Trip trip : transitBuilder.getTripsById().values()) {
            trip.getId().setAgencyId(this.feedId.getId());
        }

        for (ServiceCalendar serviceCalendar : transitBuilder.getCalendars()) {
            serviceCalendar.getServiceId().setAgencyId(this.feedId.getId());
        }
        for (ServiceCalendarDate serviceCalendarDate : transitBuilder.getCalendarDates()) {
            serviceCalendarDate.getServiceId().setAgencyId(this.feedId.getId());
        }

        for (FareAttribute fareAttribute : transitBuilder.getFareAttributes()) {
            fareAttribute.getId().setAgencyId(this.feedId.getId());
        }

        for (Pathway pathway : transitBuilder.getPathways()) {
            pathway.getId().setAgencyId(this.feedId.getId());
        }

        transitBuilder.regenerateIndexes();
    }

    private void repairStopTimesForEachTrip() {
        new RepairStopTimesForEachTripOperation(
                transitBuilder.getStopTimesSortedByTrip(),
                addBuilderAnnotation()
        ).run();
    }

    private void generateTripPatterns() {
        new GenerateTripPatternsOperation(
                transitBuilder, addBuilderAnnotation(), deduplicator(), calendarService()
        ).run();
    }

    private CalendarService calendarService() {
        if (calendarService == null) {
            calendarService = createCalendarService(transitBuilder);
        }
        return calendarService;
    }

    private AddBuilderAnnotation addBuilderAnnotation() {
        if (addBuilderAnnotation == null) {
            addBuilderAnnotation = GraphBuilderAnnotation::getMessage;
        }
        return addBuilderAnnotation;
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

        private final OtpTransitServiceBuilder transitBuilder;

        private GtfsContextImpl(GtfsFeedId feedId, OtpTransitServiceBuilder transitBuilder) {
            this.feedId = feedId;
            this.transitBuilder = transitBuilder;
        }

        @Override
        public GtfsFeedId getFeedId() {
            return feedId;
        }

        @Override
        public OtpTransitServiceBuilder getTransitBuilder() {
            return transitBuilder;
        }
    }

}
