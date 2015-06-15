package org.opentripplanner.graph_builder.module;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.graph_builder.linking.SimpleStreetSplitter;
import org.opentripplanner.graph_builder.module.osm.DefaultWayPropertySetSource;
import org.opentripplanner.graph_builder.module.osm.OpenStreetMapModule;
import org.opentripplanner.openstreetmap.impl.AnyFileBasedOpenStreetMapProviderImpl;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.TransitStop;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;

/**
 * Get fake graphs.
 */
public class FakeGraph {
    /** Build a graph in Columbus, OH with no transit */
    public static Graph buildGraphNoTransit () throws UnsupportedEncodingException {
        Graph gg = new Graph();

        OpenStreetMapModule loader = new OpenStreetMapModule();
        loader.setDefaultWayPropertySetSource(new DefaultWayPropertySetSource());
        AnyFileBasedOpenStreetMapProviderImpl provider = new AnyFileBasedOpenStreetMapProviderImpl();

        File file = new File(
                URLDecoder.decode(FakeGraph.class.getResource("columbus.osm.pbf").getFile(), "UTF-8"));

        provider.setPath(file);
        loader.setProvider(provider);

        loader.buildGraph(gg, new HashMap<Class<?>, Object>());
        return gg;
    }

    /** Add a regular grid of stops to the graph */
    public static void addRegularStopGrid(Graph g) {
        int count = 0;
        for (double lat = 39.9058; lat < 40.0281; lat += 0.005) {
            for (double lon = -83.1341; lon < -82.8646; lon += 0.005) {
                String id = "" + count++;
                AgencyAndId aid = new AgencyAndId("TEST", id);
                Stop stop = new Stop();
                stop.setLat(lat);
                stop.setLon(lon);
                stop.setName(id);
                stop.setCode(id);
                stop.setId(aid);

                new TransitStop(g, stop);
                count++;
            }
        }
    }

    /** add some extra stops to the graph */
    public static void addExtraStops (Graph g) {
        int count = 0;
        double lon = -83;
        for (double lat = 40; lat < 40.01; lat += 0.005) {
            String id = "EXTRA_" + count++;
            AgencyAndId aid = new AgencyAndId("EXTRA", id);
            Stop stop = new Stop();
            stop.setLat(lat);
            stop.setLon(lon);
            stop.setName(id);
            stop.setCode(id);
            stop.setId(aid);

            new TransitStop(g, stop);
            count++;
        }

        // add some duplicate stops
        lon = -83.1341 + 0.1;

        for (double lat = 39.9058; lat < 40.0281; lat += 0.005) {
            String id = "" + count++;
            AgencyAndId aid = new AgencyAndId("EXTRA", id);
            Stop stop = new Stop();
            stop.setLat(lat);
            stop.setLon(lon);
            stop.setName(id);
            stop.setCode(id);
            stop.setId(aid);

            new TransitStop(g, stop);
            count++;
        }

        // add some almost duplicate stops
        lon = -83.1341 + 0.15;

        for (double lat = 39.9059; lat < 40.0281; lat += 0.005) {
            String id = "" + count++;
            AgencyAndId aid = new AgencyAndId("EXTRA", id);
            Stop stop = new Stop();
            stop.setLat(lat);
            stop.setLon(lon);
            stop.setName(id);
            stop.setCode(id);
            stop.setId(aid);

            new TransitStop(g, stop);
            count++;
        }
    }

    /** link the stops in the graph */
    public static void link (Graph g) {
        SimpleStreetSplitter linker = new SimpleStreetSplitter(g);
        linker.link();
    }

}
