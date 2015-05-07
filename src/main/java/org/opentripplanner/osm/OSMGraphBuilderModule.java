package org.opentripplanner.osm;

import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.util.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;

public abstract class OSMGraphBuilderModule implements GraphBuilderModule {

    private static final Logger LOG = LoggerFactory.getLogger(OSMGraphBuilderModule.class);

    protected OSM osm;

    public static void main (String[] args) {
        Graph graph = new Graph();
        VexServerModule module = new VexServerModule("localhost", 45.506055,-122.602763,45.518,-122.586004);
        module.checkInputs();
        module.buildGraph(graph, new HashMap<Class<?>, Object>());
    }

    public static class PBFModule extends OSMGraphBuilderModule {

        String pbfFilePath;

        public PBFModule(String path) {
            pbfFilePath = path;
        }

        @Override
        public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {
            osm = OSM.fromPBF(pbfFilePath);
        }

        @Override
        public void checkInputs() {
            File pbfFile = new File(pbfFilePath);
            if ( ! pbfFile.isFile()) {
                throw new RuntimeException("The specified PBF file is not a 'normal file'.");
            }
            if ( ! new File(pbfFilePath).canRead()) {
                throw new RuntimeException("The specified PBF file is not readable.");
            }
        }
    }

    public static class VexServerModule extends OSMGraphBuilderModule {

        public final String host;
        public final double minLat;
        public final double minLon;
        public final double maxLat;
        public final double maxLon;
        URL url;

        public VexServerModule(String host, double minLat, double minLon, double maxLat, double maxLon) {
            this.host = host;
            this.minLat = minLat;
            this.minLon = minLon;
            this.maxLat = maxLat;
            this.maxLon = maxLon;
            try {
                String docPath = String.format(Locale.US, "/%f,%f,%f,%f", minLat, minLon, maxLat, maxLon);
                url = new URL("http", host, 9001, docPath);
            } catch (MalformedURLException e) {
                throw new RuntimeException("Malformed VEX server URL.");
            }
        }

        @Override
        public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {
            try {
                InputStream vexInputStream = url.openStream();
                osm = new OSM (null); // temporary file DB
                osm.loadFromVexStream(vexInputStream);
                vexInputStream.close();
            } catch (IOException e) {
                LOG.error("IO exception reading from VEX server.");
                e.printStackTrace();
            }
        }

        @Override
        public void checkInputs() {
            try {
                HttpUtils.testUrl(url.toExternalForm());
                LOG.info("VEX server responded to HEAD request.");
            } catch (IOException e) {
                throw new RuntimeException("IO exception when connecting to VEX server.");
            }
        }
    }

}
