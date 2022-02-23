package org.opentripplanner.netex;

import org.opentripplanner.ext.flex.FlexTripsMapper;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.graph_builder.module.AddTransitModelEntitiesToGraph;
import org.opentripplanner.graph_builder.module.GtfsFeedId;
import org.opentripplanner.graph_builder.module.geometry.GeometryAndBlockProcessor;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.model.OtpTransitService;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.model.calendar.ServiceDateInterval;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.fares.impl.DefaultFareServiceFactory;
import org.opentripplanner.routing.fares.FareServiceFactory;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.util.OTPFeature;

import java.util.HashMap;
import java.util.List;


/**
 * This module is used for importing the NeTEx CEN Technical Standard for exchanging
 * Public Transport schedules and related data
 * (<a href="http://netex-cen.eu/">http://netex-cen.eu/</a>). Currently it only supports the
 * Norwegian profile
 * (<a href="https://enturas.atlassian.net/wiki/spaces/PUBLIC/">https://enturas.atlassian.net/wiki/spaces/PUBLIC/</a>),
 * but it is intended to be updated later to support other profiles.
 */
public class NetexModule implements GraphBuilderModule {

    private final double maxStopToShapeSnapDistance;
    private final int subwayAccessTime;
    private final int maxInterlineDistance;
    private final String netexFeedId;

    /**
     * @see BuildConfig#transitServiceStart
     * @see BuildConfig#transitServiceEnd
     */
    private final ServiceDateInterval transitPeriodLimit;

    private final List<NetexBundle> netexBundles;

    private final FareServiceFactory fareServiceFactory = new DefaultFareServiceFactory();

    public NetexModule(
            String netexFeedId,
            int subwayAccessTime,
            int maxInterlineDistance,
            double maxStopToShapeSnapDistance,
            ServiceDateInterval transitPeriodLimit,
            List<NetexBundle> netexBundles
    ) {
        this.netexFeedId = netexFeedId;
        this.subwayAccessTime = subwayAccessTime;
        this.maxInterlineDistance = maxInterlineDistance;
        this.transitPeriodLimit = transitPeriodLimit;
        this.netexBundles = netexBundles;
        this.maxStopToShapeSnapDistance = maxStopToShapeSnapDistance;
    }

    @Override
    public void buildGraph(
            Graph graph,
            HashMap<Class<?>, Object> extra,
            DataImportIssueStore issueStore
    ) {

        graph.clearTimeZone();
        CalendarServiceData calendarServiceData = graph.getCalendarDataService();
        try {
            for (NetexBundle netexBundle : netexBundles) {
                netexBundle.checkInputs();

                OtpTransitServiceBuilder transitBuilder = netexBundle.loadBundle(
                        graph.deduplicator,
                        issueStore
                );
                transitBuilder.limitServiceDays(transitPeriodLimit);

                calendarServiceData.add(transitBuilder.buildCalendarServiceData());

                if (OTPFeature.FlexRouting.isOn()) {
                    transitBuilder.getFlexTripsById().addAll(
                            FlexTripsMapper.createFlexTrips(transitBuilder, issueStore)
                    );
                }

                OtpTransitService otpService = transitBuilder.build();


                // TODO OTP2 - Move this into the AddTransitModelEntitiesToGraph
                //           - and make sure thay also work with GTFS feeds - GTFS do no
                //           - have operators and notice assignments.
                graph.getOperators().addAll(otpService.getAllOperators());
                graph.addNoticeAssignments(otpService.getNoticeAssignments());

                GtfsFeedId feedId = new GtfsFeedId.Builder().id(netexFeedId).build();

                AddTransitModelEntitiesToGraph.addToGraph(
                        feedId, otpService, subwayAccessTime, graph
                );

                new GeometryAndBlockProcessor(
                        otpService,
                        fareServiceFactory,
                        maxStopToShapeSnapDistance,
                        maxInterlineDistance
                ).run(graph, issueStore);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        graph.clearCachedCalenderService();
        graph.putService(CalendarServiceData.class, calendarServiceData);
        graph.updateTransitFeedValidity(calendarServiceData, issueStore);

        graph.hasTransit = true;
        graph.calculateTransitCenter();
    }

    @Override
    public void checkInputs() {
        netexBundles.forEach(NetexBundle::checkInputs);
    }

}