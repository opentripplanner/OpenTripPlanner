/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.api.resource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opentripplanner.api.common.RoutingResource;
import org.opentripplanner.routing.algorithm.EarliestArrivalSearch;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.edgetype.StreetTransitLink;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.standalone.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.internal.Maps;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.io.Files;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;

/**
 * This class provides a vector isochrone generator for places without good OSM street connectivity,
 * or for graphs that are too large-scale to bother with modeling streets (e.g. all of China). It
 * assumes constant speed through both streets and "free space" around vertices, yielding isochrones
 * that are the union of many circles.
 * 
 * The origin snapping option used here (connect to all stations within M meters of a lat/lon)
 * should be made into a standard query option in OTP. Instead of specifying an origin station
 * search radius, it could be more coherent to use all stations within (maxIsochroneTime * speed)
 * meters, and apply a travel time to reach them.
 * 
 * The GET methods both use makeContours which in turn uses makePoints (where range checking
 * occurs). Putting "@Setter" on all the parameters would allow resource classes to be used outside
 * Jersey.
 */
@Path("/routers/{routerId}/simpleIsochrone")
public class SimpleIsochrone extends RoutingResource {
    
    private static final Logger LOG = LoggerFactory.getLogger(SimpleIsochrone.class);

    private static final EarliestArrivalSearch sptService = new EarliestArrivalSearch();

    /* Parameters shared between all methods. */
    @QueryParam("requestSpacingMinutes") @DefaultValue("30") 
    int requestSpacingMinutes; 
    @QueryParam("requestTimespanHours") @DefaultValue("20") 
    int requestTimespanHours;
    @QueryParam("stopsOnly") @DefaultValue("true") 
    boolean stopsOnly;
    @QueryParam("radiusMeters") @DefaultValue("5000") 
    double radiusMeters; 
    @QueryParam("nContours") @DefaultValue("4") 
    int nContours;
    @QueryParam("contourSpacingMinutes") @DefaultValue("30") 
    int contourSpacingMinutes;
    
    /** Whether the results should be left in the indicated CRS or de-projected back to WGS84. */
    @QueryParam("resultsProjected") @DefaultValue("false") boolean resultsProjected;

    /** The coordinate reference system in which buffering should be performed. 
        Defaults to the Hong Kong 1980 Grid System. */
    // We are not using CoordinateReferenceSystem StringReaderProvider here because 
    // Enunciate chokes on it (see #1319).
    @QueryParam("crs") @DefaultValue("EPSG:2326") String crsCode;

    /** What to name the output file. */
    @QueryParam("shpName") String shpName;

    private RoutingRequest request;

    
    /* Static feature schemas */
    
    private static final SimpleFeatureType pointSchema = makePointSchema();
    
    private static final SimpleFeatureType contourSchema = makeContourSchema();
    
    static SimpleFeatureType makePointSchema() {
        SimpleFeatureTypeBuilder tbuilder = new SimpleFeatureTypeBuilder();
        tbuilder.setName("points");
        tbuilder.setCRS(DefaultGeographicCRS.WGS84);
        tbuilder.add("Geometry", Point.class);
        tbuilder.add("Time", Integer.class); 
        return tbuilder.buildFeatureType();
    }

    static SimpleFeatureType makeContourSchema() {
        /* Create the output feature schema. */
        SimpleFeatureTypeBuilder tbuilder = new SimpleFeatureTypeBuilder();
        tbuilder.setName("contours");
        tbuilder.setCRS(DefaultGeographicCRS.WGS84);
        tbuilder.add("Geometry", MultiPolygon.class);
        tbuilder.add("Time", Integer.class); 
        return tbuilder.buildFeatureType();
    }

    private void rangeCheckParameters () {
        
    }
    
    /** @return a map from each vertex to minimum travel time over the course of the day. */
    private Map<Vertex, Double> makePoints () throws Exception {
        rangeCheckParameters();
        request = buildRequest();
        Router router = otpServer.getRouter(routerId);
        Graph graph = router.graph;
        //double speed = request.getWalkSpeed();
        Coordinate originCoord = request.from.getCoordinate();
        if (originCoord == null) return null;
        List<TransitStop> stops = graph.streetIndex.getNearbyTransitStops(originCoord, radiusMeters);
        if (stops.isEmpty()) {
            LOG.error("No stops found within {} meters.", radiusMeters);
            return null;
        }
        if (shpName == null)
            shpName = stops.get(0).getName().split(" ")[0];   
        StreetVertex origin = new IntersectionVertex(graph, "iso_temp", originCoord.x, originCoord.y);
        for (TransitStop stop : stops) {
            new StreetTransitLink(origin, stop, false);
            LOG.debug("linked to stop {}", stop.getName());
        }
        request.setRoutingContext(graph, origin, null);
        
        /* Make one request every M minutes over H hours */
        int nRequests = (requestTimespanHours * 60) / requestSpacingMinutes;
        request.clampInitialWait = (requestSpacingMinutes * 60);
        Date date = request.getDateTime();
        MinMap<Vertex, Double> points = new MinMap<Vertex, Double>();
        for (int r = 0; r < nRequests; r++) {
            request.dateTime = date.getTime() / 1000 + r * requestSpacingMinutes * 60;
            LOG.debug("date: {} {}", new Date(request.dateTime), request.dateTime);
            ShortestPathTree spt = sptService.getShortestPathTree(request, 10);
            /* This could even be a good use for a generic SPT merging function */
            for (State s : spt.getAllStates()) {
                if ( stopsOnly && ! (s.getVertex() instanceof TransitStop)) continue;
                points.putMin(s.getVertex(), (double)(s.getActiveTime()));
            }
        }
        graph.removeVertexAndEdges(origin);
        return points;
    }
    
    /** @return a map from contour (isochrone) thresholds in seconds to geometries. */
    private Map<Integer, Geometry> makeContours() throws Exception { 
        
        Map<Vertex, Double> points = makePoints();
        if (points == null || points.isEmpty()) {
            LOG.error("No destinations were reachable.");
            return null;
        }
        
        /* Set up transforms into projected coordinates (necessary for buffering in meters) */
        /* We could avoid projection by equirectangular approximation */
        CoordinateReferenceSystem wgs = DefaultGeographicCRS.WGS84;
        CoordinateReferenceSystem crs = CRS.decode(crsCode);
        MathTransform toMeters   = CRS.findMathTransform(wgs, crs, false);
        MathTransform fromMeters = CRS.findMathTransform(crs, wgs, false); 
        GeometryFactory geomf = JTSFactoryFinder.getGeometryFactory();
                
        /* One list of geometries for each contour */
        Multimap<Integer, Geometry> bufferLists = ArrayListMultimap.create();
        for (int c = 0; c < nContours; ++c) {
            int thresholdSeconds = (c + 1) * contourSpacingMinutes * 60;
            for (Map.Entry<Vertex, Double> vertexSeconds : points.entrySet()) {
                double remainingSeconds = thresholdSeconds - vertexSeconds.getValue();
                if (remainingSeconds > 60) { // avoid degenerate geometries
                    double remainingMeters = remainingSeconds * request.walkSpeed;
                    Geometry point = geomf.createPoint(vertexSeconds.getKey().getCoordinate());
                    point = JTS.transform(point, toMeters);
                    Geometry buffer = point.buffer(remainingMeters);
                    bufferLists.put(thresholdSeconds, buffer);
                }
            }
        }

        /* Union the geometries at each contour threshold. */
        Map<Integer, Geometry> contours = Maps.newHashMap();
        for (Integer threshold : bufferLists.keys()) {
            Collection<Geometry> buffers = bufferLists.get(threshold);
            Geometry geom = geomf.buildGeometry(buffers); // make geometry collection
            geom = geom.union(); // combine all individual buffers in this contour into one
            if ( ! resultsProjected) geom = JTS.transform(geom, fromMeters);
            contours.put(threshold, geom);
        }
        return contours;
    }

    private SimpleFeatureCollection makePointFeatures() throws Exception {
        Map<Vertex, Double> points = makePoints();
        /* Stage the point features in memory */
        DefaultFeatureCollection featureCollection = new DefaultFeatureCollection(null, pointSchema);
        SimpleFeatureBuilder fbuilder = new SimpleFeatureBuilder(pointSchema);
        GeometryFactory gf = new GeometryFactory();
        for (Map.Entry<Vertex, Double> entry : points.entrySet()) {
            Vertex vertex = entry.getKey();
            Double travelTime = entry.getValue();
            fbuilder.add(gf.createPoint(vertex.getCoordinate()));
            fbuilder.add(travelTime);
            featureCollection.add(fbuilder.buildFeature(null));
        }
        return featureCollection;
    }

    private SimpleFeatureCollection makeContourFeatures() throws Exception {
        Map<Integer, Geometry> contours = makeContours();
        /* Stage the features in memory, in order from bottom to top, biggest to smallest */
        DefaultFeatureCollection featureCollection = 
                new DefaultFeatureCollection(null, contourSchema);
        SimpleFeatureBuilder fbuilder = new SimpleFeatureBuilder(contourSchema);
        List<Integer> thresholds = new ArrayList<Integer>(contours.keySet());
        Collections.sort(thresholds);
        //Collections.reverse(thresholds);
        for (Integer threshold : thresholds) {
            Geometry contour = contours.get(threshold);
            fbuilder.add(contour);
            fbuilder.add(threshold);
            featureCollection.add(fbuilder.buildFeature(null));
        }
        return featureCollection;
    }

    /** @return Evenly spaced travel time contours (isochrones) as GeoJSON. */
    @GET @Produces("application/json")
    public Response geoJsonGet() throws Exception { 
        /* QGIS seems to want multi-features rather than multi-geometries. */
        SimpleFeatureCollection contourFeatures = makeContourFeatures();
        /* Output the staged features to JSON */
        StringWriter writer = new StringWriter();
        FeatureJSON fj = new FeatureJSON();
        fj.writeFeatureCollection(contourFeatures, writer);
        return Response.ok().entity(writer.toString()).build();
    }
        
    /** @return Evenly spaced travel time contours (isochrones) as a zipped shapefile. */
    @GET @Produces("application/x-zip-compressed")
    public Response zippedShapefileGet(
            @QueryParam("stream") @DefaultValue("true") boolean stream) 
            throws Exception {
        SimpleFeatureCollection contourFeatures = makeContourFeatures(); 
        /* Output the staged features to Shapefile */
        final File shapeDir = Files.createTempDir();
        File shapeFile = new File(shapeDir, shpName + ".shp");
        LOG.debug("writing out shapefile {}", shapeFile);
        ShapefileDataStore outStore = new ShapefileDataStore(shapeFile.toURI().toURL());
        outStore.createSchema(contourSchema);
        /* "FeatureSource is used to read features, the subclass FeatureStore is used for 
         * read/write access. The way to tell if a File can be written to in GeoTools is to use an 
         * instanceof check. */
        SimpleFeatureStore featureStore = (SimpleFeatureStore) outStore.getFeatureSource();
        featureStore.addFeatures(contourFeatures);
        // close?
        shapeDir.deleteOnExit(); // Note: the order is important
        for (File f : shapeDir.listFiles())
            f.deleteOnExit();
        /* Zip up the shapefile components */  
        StreamingOutput output = new DirectoryZipper(shapeDir);
        if (stream) {
            return Response.ok().entity(output).build();
        } else {
            File zipFile = new File(shapeDir, shpName + ".zip");
            OutputStream fos = new FileOutputStream(zipFile);
            output.write(fos);
            zipFile.deleteOnExit();
            return Response.ok().entity(zipFile).build();
        }
    }   
    
    private static class DirectoryZipper implements StreamingOutput {
        private File directory;
        
        DirectoryZipper(File directory){
        	this.directory = directory;
        }
        
        @Override
        public void write(OutputStream outStream) throws IOException {
            ZipOutputStream zip = new ZipOutputStream(outStream);
            for (File f : directory.listFiles()) {
                zip.putNextEntry(new ZipEntry(f.getName()));
                Files.copy(f, zip);
                zip.closeEntry();
                zip.flush();
            }
            zip.close();
        }
    }

    /**
     * A HashMap that has been extended to track the greatest or smallest value for each key.
     * Note that this does not change the meaning of the 'put' method. It adds two new methods that add the min/max
     * behavior.
     */
    public static class MinMap<K, V extends Comparable<V>> extends HashMap<K, V> {
        private static final long serialVersionUID = -23L;

        /** 
         * Put the given key-value pair in the map if the map does not yet contain the key, or if
         * the value is less than the existing value for the same key. 
         * @return whether the key-value pair was inserted in the map.
         */
        public boolean putMin(K key, V value) {
            V oldValue = this.get(key);
            if (oldValue == null || value.compareTo(oldValue) < 0) {
                this.put(key, value);
                return true;
            }
            return false;
        }
        
        /** 
         * Put the given key-value pair in the map if the map does not yet contain the key, or if
         * the value is greater than the existing value for the same key. 
         * @return whether the key-value pair was inserted in the map.
         */
        public boolean putMax(K key, V value) {
            V oldValue = this.get(key);
            if (oldValue == null || value.compareTo(oldValue) > 0) {
                this.put(key, value);
                return true;
            }
            return false;
        }
        
    }

}