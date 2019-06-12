package org.opentripplanner.graph_builder.module;

import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.netex.loader.NetexBundle;
import org.opentripplanner.netex.loader.NetexLoader;
import org.opentripplanner.routing.edgetype.factory.PatternHopFactory;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.DefaultFareServiceFactory;
import org.opentripplanner.routing.services.FareServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;

// TODO OTP2 - logging?

/**
 * This module is used for importing the NeTEx CEN Technical Standard for exchanging Public Transport schedules and
 * related data (<a href="http://netex-cen.eu/">http://netex-cen.eu/</a>). Currently it only supports the Norwegian
 * profile (<a href="https://enturas.atlassian.net/wiki/spaces/PUBLIC/">
 * https://enturas.atlassian.net/wiki/spaces/PUBLIC/</a>), but it is intended to be updated later to support other
 * profiles.
 */
public class NetexModule implements GraphBuilderModule {

    private static final Logger LOG = LoggerFactory.getLogger(NetexModule.class);

    private List<NetexBundle> netexBundles;

    private FareServiceFactory fareServiceFactory = new DefaultFareServiceFactory();

    public NetexModule(List<NetexBundle> netexBundles) {
        this.netexBundles = netexBundles;
    }

    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {

        graph.clearTimeZone();
        CalendarServiceData calendarServiceData = new CalendarServiceData();

        // TODO OTP2 - Stops set inside the hf.run. The next line appered after merging
        // TODO OTP2 - dev-2.x and netex_inport, It does not compile due to the deletion of the
        // TODO OTP2 - GtfsStopContext in dex-2.x - Verify that the code still is OK, and remove
        // TODO OTP2 -  this and the "//hf.setStopContext(stopContext);" below (line 67).
        //GtfsStopContext stopContext = new GtfsStopContext();

        try {
            for (NetexBundle netexBundle : netexBundles) {
                OtpTransitServiceBuilder transitBuilder = new NetexLoader(netexBundle).loadBundle();
                calendarServiceData.add(transitBuilder.buildCalendarServiceData());

                PatternHopFactory hf = new PatternHopFactory(
                        new GtfsFeedId.Builder()
                                .id(netexBundle.netexParameters.netexFeedId)
                                .build(),
                        transitBuilder.build(),
                        fareServiceFactory,
                        netexBundle.getMaxStopToShapeSnapDistance(),
                        netexBundle.subwayAccessTime,
                        netexBundle.maxInterlineDistance
                );
                //hf.setStopContext(stopContext);
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