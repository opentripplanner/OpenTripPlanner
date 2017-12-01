package org.opentripplanner.graph_builder.module;

import org.opentripplanner.calendar.impl.MultiCalendarServiceImpl;
import org.opentripplanner.netex.loader.NetexBundle;
import org.opentripplanner.netex.loader.NetexLoader;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.routing.edgetype.factory.GtfsStopContext;
import org.opentripplanner.routing.edgetype.factory.PatternHopFactory;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.DefaultFareServiceFactory;
import org.opentripplanner.routing.services.FareServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;

public class NetexModule implements GraphBuilderModule {

    private static final Logger LOG = LoggerFactory.getLogger(NetexModule.class);

    private List<NetexBundle> netexBundles;

    private FareServiceFactory _fareServiceFactory = new DefaultFareServiceFactory();

    public NetexModule(List<NetexBundle> netexBundles) {
        this.netexBundles = netexBundles;
    }

    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {

        graph.clearTimeZone();
        MultiCalendarServiceImpl calendarService = new MultiCalendarServiceImpl();
        GtfsStopContext stopContext = new GtfsStopContext();

        try {
            for (NetexBundle netexBundle : netexBundles) {
                OtpTransitServiceBuilder daoBuilder = new NetexLoader(netexBundle).loadBundle();

                calendarService.addData(daoBuilder);

                PatternHopFactory hf = new PatternHopFactory(
                        new GtfsFeedId.Builder().id(netexBundle.netexParameters.netexFeedId)
                                .build(), daoBuilder.build(), _fareServiceFactory,
                        netexBundle.getMaxStopToShapeSnapDistance(), netexBundle.subwayAccessTime,
                        netexBundle.maxInterlineDistance);
                hf.setStopContext(stopContext);
                hf.run(graph);

                if (netexBundle.linkStopsToParentStations) {
                    hf.linkStopsToParentStations(graph);
                }
                if (netexBundle.parentStationTransfers) {
                    hf.createParentStationTransfers();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        CalendarServiceData data = calendarService.getData();
        graph.putService(CalendarServiceData.class, data);
        graph.updateTransitFeedValidity(data);

        graph.hasTransit = true;
        graph.calculateTransitCenter();
    }

    @Override
    public void checkInputs() {
        netexBundles.forEach(NetexBundle::checkInputs);
    }
}