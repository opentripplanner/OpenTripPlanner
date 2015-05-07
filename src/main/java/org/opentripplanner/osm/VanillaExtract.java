package org.opentripplanner.osm;

import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.server.*;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.mapdb.Fun;
import org.mapdb.Fun.Tuple3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.BindException;
import java.util.SortedSet;
import java.util.zip.GZIPOutputStream;

/**
 * Load OSM data into MapDB and perform bounding box extracts.
 *
 * Some useful tools:
 * http://boundingbox.klokantech.com
 * http://www.maptiler.org/google-maps-coordinates-tile-bounds-projection/
 */
public class VanillaExtract {

    private static final Logger LOG = LoggerFactory.getLogger(VanillaExtract.class);

    private static final int PORT = 9001;

    private static final String BIND_ADDRESS = "0.0.0.0";

    public static void main(String[] args) {

        VexPbfParser parser = new VexPbfParser(args[0]);
        parser.parse(args[1]);

        LOG.info("Starting VEX HTTP server on port {} of interface {}", PORT, BIND_ADDRESS);
        HttpServer httpServer = new HttpServer();
        httpServer.addListener(new NetworkListener("vanilla_extract", BIND_ADDRESS, PORT));
        // Bypass Jersey etc. and add a low-level Grizzly handler.
        // As in servlets, * is needed in base path to identify the "rest" of the path.
        httpServer.getServerConfiguration().addHttpHandler(new VexHttpHandler(parser), "/*");
        try {
            httpServer.start();
            LOG.info("VEX server running.");
            Thread.currentThread().join();
        } catch (BindException be) {
            LOG.error("Cannot bind to port {}. Is it already in use?", PORT);
        } catch (IOException ioe) {
            LOG.error("IO exception while starting server.");
        } catch (InterruptedException ie) {
            LOG.info("Interrupted, shutting down.");
        }
        httpServer.shutdown();

    }

    private static class VexHttpHandler extends HttpHandler {

        private static VexPbfParser parser;

        public VexHttpHandler(VexPbfParser parser) {
            this.parser = parser;
        }

        @Override
        public void service(Request request, Response response) throws Exception {
            OutputStream out = response.getOutputStream();
            String uri = request.getDecodedRequestURI();
            response.setContentType("application/gzip");
            try {
                String[] coords = uri.split("/")[1].split("[,;]");
                double minLat = Double.parseDouble(coords[0]);
                double minLon = Double.parseDouble(coords[1]);
                double maxLat = Double.parseDouble(coords[2]);
                double maxLon = Double.parseDouble(coords[3]);
                if (minLat >= maxLat || minLon >= maxLon) {
                    throw new IllegalArgumentException();
                }
                /* Respond to head requests to let the client know the server is alive and the request is valid. */
                if (request.getMethod() == Method.HEAD) {
                    response.setStatus(HttpStatus.OK_200);
                    return;
                }
                /* TODO filter out buildings on the server side. */
                boolean buildings = coords.length > 4 && "buildings".equalsIgnoreCase(coords[4]);
                VexPbfParser.WebMercatorTile minTile = new VexPbfParser.WebMercatorTile(minLat, minLon);
                VexPbfParser.WebMercatorTile maxTile = new VexPbfParser.WebMercatorTile(maxLat, maxLon);

                // Note that y tile numbers are increasing in the opposite direction of latitude (from north to south)
                int minX = minTile.xtile;
                int minY = maxTile.ytile;
                int maxX = maxTile.xtile;
                int maxY = minTile.ytile;

                OutputStream zipOut = new GZIPOutputStream(out);
                OSMTextOutput tout = new OSMTextOutput(zipOut, parser.osm);

                // SortedSet provides one-dimensional ordering and iteration. Tuple3 gives an odometer-like ordering.
                // Therefore we must vary one of the dimensions "manually". Consider a set containing all the integers
                // from 00 to 99 at 2-tuples. The range from (1,1) to (2,2) does not contain the four
                // elements (1,1) (1,2) (2,1) (2,2). It contains the elements (1,1) (1,2) (1,3) (1,4) ... (2,2).
                for (int x = minX; x <= maxX; x++) {
                    SortedSet<Tuple3<Integer, Integer, Long>> xSubset = parser.osm.index.subSet(
                        new Tuple3(x, minY, null  ), true, // inclusive lower bound, null tests lower than anything
                        new Tuple3(x, maxY, Fun.HI), true  // inclusive upper bound, HI tests higher than anything
                    );
                    for (Tuple3<Integer, Integer, Long> item : xSubset) {
                        long wayId = item.c;
                        tout.printWay(wayId);
                    }
                }
                zipOut.close(); // necessary to avoid corrupted gzip file
                response.setStatus(HttpStatus.OK_200);
            } catch (Exception ex) {
                response.setStatus(HttpStatus.BAD_REQUEST_400);
                out.write("URI format: /min_lat,min_lon,max_lat,max_lon (all in decimal degrees)\n".getBytes());
                ex.printStackTrace();
            } finally {
                out.close();
            }
        }

    }
}
