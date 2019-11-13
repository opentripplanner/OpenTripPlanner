package org.opentripplanner.netex;

import org.opentripplanner.graph_builder.BuilderAnnotationStore;
import org.opentripplanner.graph_builder.module.AddTransitModelEntitiesToGraph;
import org.opentripplanner.graph_builder.module.GtfsFeedId;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.model.OtpTransitService;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.netex.loader.NetexBundle;
import org.opentripplanner.graph_builder.module.geometry.GeometryAndBlockProcessor;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.DefaultFareServiceFactory;
import org.opentripplanner.routing.services.FareServiceFactory;

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
    private final static double MAX_STOP_TO_SHAPE_SNAP_DISTANCE = 150;

    private final boolean linkStopsToParentStations;
    private final boolean parentStationTransfers;
    private final int subwayAccessTime;
    private final int maxInterlineDistance;
    private final String netexFeedId;


    private List<NetexBundle> netexBundles;

    private FareServiceFactory fareServiceFactory = new DefaultFareServiceFactory();

    public NetexModule(
            String netexFeedId,
            boolean linkStopsToParentStations,
            boolean parentStationTransfers,
            int subwayAccessTime,
            int maxInterlineDistance,
            List<NetexBundle> netexBundles
    ) {
        this.netexFeedId = netexFeedId;
        this.linkStopsToParentStations = linkStopsToParentStations;
        this.parentStationTransfers = parentStationTransfers;
        this.subwayAccessTime = subwayAccessTime;
        this.maxInterlineDistance = maxInterlineDistance;
        this.netexBundles = netexBundles;
    }

    @Override
    public void buildGraph(
            Graph graph,
            HashMap<Class<?>, Object> extra,
            BuilderAnnotationStore annotationStore
    ) {

        graph.clearTimeZone();
        CalendarServiceData calendarServiceData = new CalendarServiceData();

        try {
            for (NetexBundle netexBundle : netexBundles) {
                netexBundle.checkInputs();

                OtpTransitServiceBuilder transitBuilder =
                        netexBundle.loadBundle(graph.deduplicator, graph);
                calendarServiceData.add(transitBuilder.buildCalendarServiceData());

                OtpTransitService otpService = transitBuilder.build();

                graph.getOperators().addAll(otpService.getAllOperators());
                graph.addNoticeAssignments(otpService.getNoticeAssignments());

                GtfsFeedId feedId = new GtfsFeedId.Builder().id(netexFeedId).build();

                AddTransitModelEntitiesToGraph.addToGraph(
                        feedId, otpService, subwayAccessTime, graph
                );

                new GeometryAndBlockProcessor(
                        otpService,
                        fareServiceFactory,
                        MAX_STOP_TO_SHAPE_SNAP_DISTANCE,
                        maxInterlineDistance
                ).run(graph);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        graph.putService(CalendarServiceData.class, calendarServiceData);
        graph.updateTransitFeedValidity(calendarServiceData);

        graph.hasTransit = true;
        graph.calculateTransitCenter();
    }

    @Override
    public void checkInputs() {
        netexBundles.forEach(NetexBundle::checkInputs);
    }

}