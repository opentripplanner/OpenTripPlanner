package org.opentripplanner.transit;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vividsolutions.jts.geom.Envelope;
import gnu.trove.set.TIntSet;
import org.glassfish.grizzly.http.server.*;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.opentripplanner.streets.EdgeStore;
import org.opentripplanner.streets.VertexStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.BindException;

import static org.opentripplanner.streets.VertexStore.floatingDegreesToFixed;

/**
 * Simple Web-based visualizer for transport networks.
 */
public class TransportNetworkVisualizer {
    private static final Logger LOG = LoggerFactory.getLogger(TransportNetworkVisualizer.class);

    public static final String INTERFACE = "0.0.0.0";
    public static final int PORT = 9007;

    public static void main (String... args) throws Exception {
        LOG.info("Starting transport network visualizer");

        TransportNetwork network;
        if (args.length == 1) {
            LOG.info("Reading serialized transport network");

            // load serialized transportnetwork
            File in = new File(args[0]);
            FileInputStream fis = new FileInputStream(in);
            network = TransportNetwork.read(fis);
            fis.close();

            LOG.info("Done reading serialized transport network");
        }
        else if (args.length == 2) {
            LOG.info("Building transport network");
            network = TransportNetwork.fromFiles(args[0], args[1]);
            LOG.info("Done building transport network");
        }
        else {
            LOG.info("usage:");
            LOG.info(" TransportNetworkVisualizer serialized_transport_network");
            LOG.info(" TransportNetworkVisualizer osm_file gtfs_file");
            return;
        }

        network.streetLayer.indexStreets();

        // network has been built
        // Start HTTP server
        LOG.info("Starting server on {}:{}", INTERFACE, PORT);
        HttpServer server = new HttpServer();
        server.addListener(new NetworkListener("transport_network_visualizer", INTERFACE, PORT));
        server.getServerConfiguration().addHttpHandler(new TransportNetworkHandler(network), "/api/*");
        server.getServerConfiguration().addHttpHandler(new CLStaticHttpHandler(ClassLoader.getSystemClassLoader(), "/org/opentripplanner/transit/TransitNetworkVisualizer/"));
        try {
            server.start();
            LOG.info("VEX server running.");
            Thread.currentThread().join();
        } catch (BindException be) {
            LOG.error("Cannot bind to port {}. Is it already in use?", PORT);
        } catch (IOException ioe) {
            LOG.error("IO exception while starting server.");
        } catch (InterruptedException ie) {
            LOG.info("Interrupted, shutting down.");
        }
        server.shutdown();

    }

    public static class TransportNetworkHandler extends
            org.glassfish.grizzly.http.server.HttpHandler {

        private final TransportNetwork network;

        public TransportNetworkHandler(TransportNetwork network) {
            this.network = network;
        }

        @Override public void service(Request req, Response res) throws Exception {
            try {
                String layer = req.getPathInfo().substring(1);
                double north = Double.parseDouble(req.getParameter("n"));
                double south = Double.parseDouble(req.getParameter("s"));
                double east = Double.parseDouble(req.getParameter("e"));
                double west = Double.parseDouble(req.getParameter("w"));

                Envelope env = new Envelope(floatingDegreesToFixed(east), floatingDegreesToFixed(west),
                        floatingDegreesToFixed(south), floatingDegreesToFixed(north));

                if ("streetEdges".equals(layer)) {
                    TIntSet streets = network.streetLayer.spatialIndex.query(env);

                    if (streets.size() > 10_000) {
                        LOG.warn("Refusing to include more than 10000 edges in result");
                        res.sendError(400, "Request area too large");
                        return;
                    }

                    // write geojson to response
                    ObjectMapper mapper = new ObjectMapper();
                    JsonFactory factory = mapper.getFactory();
                    OutputStream os = new ByteArrayOutputStream();
                    JsonGenerator gen = factory.createGenerator(os);

                    // geojson header
                    gen.writeStartObject();
                    gen.writeStringField("type", "FeatureCollection");

                    gen.writeArrayFieldStart("features");

                    EdgeStore.Edge cursor = network.streetLayer.edgeStore.getCursor();
                    VertexStore.Vertex vcursor = network.streetLayer.vertexStore.getCursor();

                    streets.forEach(s -> {
                        try {
                            cursor.seek(s);

                            gen.writeStartObject();

                            gen.writeObjectFieldStart("properties");
                            gen.writeEndObject();

                            gen.writeStringField("type", "Feature");

                            gen.writeObjectFieldStart("geometry");
                            gen.writeStringField("type", "LineString");
                            gen.writeArrayFieldStart("coordinates");

                            gen.writeStartArray();
                            vcursor.seek(cursor.getFromVertex());
                            gen.writeNumber(vcursor.getLon());
                            gen.writeNumber(vcursor.getLat());
                            gen.writeEndArray();

                            gen.writeStartArray();
                            vcursor.seek(cursor.getToVertex());
                            gen.writeNumber(vcursor.getLon());
                            gen.writeNumber(vcursor.getLat());
                            gen.writeEndArray();

                            gen.writeEndArray();

                            gen.writeEndObject();
                            gen.writeEndObject();
                            return true;
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });

                    gen.writeEndArray();
                    gen.writeEndObject();

                    gen.flush();
                    gen.close();
                    os.close();

                    String json = os.toString();

                    res.setStatus(HttpStatus.OK_200);
                    res.setContentType("application/json");
                    res.setContentLength(json.length());
                    res.getWriter().write(json);
                }
            } catch (Exception e) {
                LOG.error("Error servicing request", e);
            }
        }
    }
}
