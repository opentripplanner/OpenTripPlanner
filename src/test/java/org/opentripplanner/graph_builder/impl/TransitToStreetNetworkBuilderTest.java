/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opentripplanner.graph_builder.impl;

import org.opentripplanner.util.StreetType;
import com.csvreader.CsvWriter;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.nio.charset.Charset;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import junit.framework.TestCase;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import static org.junit.Assert.assertNotNull;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.graph_builder.annotation.GraphBuilderAnnotation;
import org.opentripplanner.graph_builder.annotation.StopUnlinked;
import org.opentripplanner.graph_builder.impl.osm.DebugNamer;
import org.opentripplanner.graph_builder.module.osm.DefaultWayPropertySetSource;
import org.opentripplanner.graph_builder.module.osm.OpenStreetMapModule;
import org.opentripplanner.graph_builder.model.GtfsBundle;
import org.opentripplanner.graph_builder.module.GtfsModule;
import org.opentripplanner.graph_builder.module.PruneFloatingIslands;
import org.opentripplanner.graph_builder.module.TransitToStreetNetworkModule;
import org.opentripplanner.graph_builder.module.TransitToTaggedStopsModule;
import org.opentripplanner.model.json_serialization.GeoJSONSerializer;
import org.opentripplanner.model.json_serialization.SerializerUtils;
import org.opentripplanner.openstreetmap.impl.AnyFileBasedOpenStreetMapProviderImpl;
import org.opentripplanner.openstreetmap.impl.FileBasedOpenStreetMapProviderImpl;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTransitLink;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.DefaultStreetVertexIndexFactory;
import org.opentripplanner.routing.impl.StreetVertexIndexServiceImpl;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.util.TransitStopConnToWantedEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mabu
 */
public class TransitToStreetNetworkBuilderTest {
    private HashMap<Class<?>, Object> extra;
    StreetVertexIndexServiceImpl index;
    private Envelope extent;
    private double searchRadiusM = 10;
    private double searchRadiusLat = SphericalDistanceLibrary.metersToDegrees(searchRadiusM);    
    private static final Logger LOG = LoggerFactory.getLogger(TransitToStreetNetworkBuilderTest.class);
    private static final boolean transit_stats = false;
    
    @Before
    public void setUp() {
        extra = new HashMap<Class<?>, Object>();
    }
    
    /**
     * Creates graph from OSM and GTFS data and runs {@link org.opentripplanner.graph_builder.module.TransitToTaggedStopsModule} and {@link org.opentripplanner.graph_builder.module.TransitToStreetNetworkModule}.
     * @param osm_filename filename for OSM (in resource folder of class)
     * @param gtfs_filename filename for GTFS (in resource folder of class)
     * @param taggedBuilder should {@link org.opentripplanner.graph_builder.module.TransitToTaggedStopsModule} be run
     * @param normalBuilder should {@link org.opentripplanner.graph_builder.module.TransitToStreetNetworkModule} be run
     * @return Loaded graph
     * @throws Exception 
     */
    private Graph loadGraph(String osm_filename, String gtfs_filename,
            boolean taggedBuilder, boolean normalBuilder) throws Exception{

        Graph gg = new Graph();

        OpenStreetMapModule loader = new OpenStreetMapModule();
        //names streets based on osm ids (osm:way:osmid)
        loader.customNamer = new DebugNamer();
        loader.setDefaultWayPropertySetSource(new DefaultWayPropertySetSource());
        AnyFileBasedOpenStreetMapProviderImpl provider = new AnyFileBasedOpenStreetMapProviderImpl();
        loader.skipVisibility = true;
        loader.staticParkAndRide = true;
        
        PruneFloatingIslands pfi = new PruneFloatingIslands();

        File file = new File(getClass().getResource(osm_filename).getFile());
        
        File gtfs_file = new File(getClass().getResource(gtfs_filename).getFile());
        List<GtfsBundle> gtfsBundles = Lists.newArrayList();
        
        GtfsBundle gtfsBundle = new GtfsBundle(gtfs_file);
        gtfsBundle.setTransfersTxtDefinesStationPaths(false);
        gtfsBundles.add(gtfsBundle);
        
        GtfsModule gtfsBuilder = new GtfsModule(gtfsBundles);

        provider.setPath(file);
        loader.setProvider(provider);

        loader.buildGraph(gg, extra);
        
        extent = gg.getExtent();
                
        //pfi.buildGraph(gg, extra);
        
        index = new StreetVertexIndexServiceImpl(gg);
        
        gtfsBuilder.buildGraph(gg, extra);
        
        if (taggedBuilder) {
            TransitToTaggedStopsModule transitToTaggedStopsGraphBuilderImpl = new TransitToTaggedStopsModule();
            transitToTaggedStopsGraphBuilderImpl.buildGraph(gg, extra);
        }
        
        if (normalBuilder) {
            TransitToStreetNetworkModule transitToStreetNetworkGraphBuilderImpl = new TransitToStreetNetworkModule();
            transitToStreetNetworkGraphBuilderImpl.buildGraph(gg, extra);
        }
        
        return gg;
    }
    
    /**
     * Finds useful streets to link to
     * 
     * It is used for bootstrapping correct transitStop to StreetLink.
     * It prefers non-drivable streets and service ways.
     * Idea is to visually check all this connections in vizgui to have a list of
     * correct connections for testing
     * @param ts TransitStop vertex
     * @param stl Link between this vertex and street made with transitToStopLinker (used only to add parameter to {@link TransitToStreetConnection})
     * @param neighbourType Which neighbour to search
     * @param transitConnections Found neighbours are added to this list
     * @return true if neighbour is found false otherwise
     */
    public boolean findNeighbours(Vertex ts, StreetTransitLink stl, org.opentripplanner.util.StreetType neighbourType, List<TransitToStreetConnection> transitConnections) {
        Envelope envelope = new Envelope(ts.getCoordinate());
        double xscale = Math.cos(ts.getCoordinate().y * Math.PI / 180);
        envelope.expandBy(searchRadiusLat / xscale, searchRadiusLat);
        Geometry poly = GeometryUtils.getGeometryFactory().toGeometry(envelope);
        Collection<Edge> edges = index.getEdgesForEnvelope(envelope);
        List<T2<StreetEdge, Double>> candidateStreets = new ArrayList<>(edges.size());
        boolean hasAWalkPathNear = false;
        System.out.println(neighbourType);
        for (Edge e : edges) {
            StreetEdge se = (StreetEdge) e;
            if (se.isBack() || se.isStairs()) {
                continue;
            }
            //Checks if street truly intersects this envelope
            if(!poly.intersects(e.getGeometry())) {
                continue;
            }
            Double distance = SphericalDistanceLibrary.fastDistance(ts.getCoordinate(), se.getGeometry());
            System.out.println("  " + distance + ", " + e);
            //Returns true if a path that can be walked/biked and not driven is near
            if ((neighbourType == StreetType.WALK_BIKE && ((se.getPermission().allows(StreetTraversalPermission.PEDESTRIAN)
                    || (se.getPermission().allows(StreetTraversalPermission.BICYCLE)))
                    && !(se.getPermission().allows(StreetTraversalPermission.CAR)))) 
                || (neighbourType == StreetType.SERVICE 
                    && (se.getName().equals("service road")))) {
                candidateStreets.add(new T2<>(se, distance));
                //TODO: some service roads aren't added in the graph because of acces:no
                if (!hasAWalkPathNear) {
                    System.out.println("  YES");
                    hasAWalkPathNear = true;
                }
            }
            
        }
        if (hasAWalkPathNear) {
            T2<StreetEdge, Double> closestRoad = Collections.min(candidateStreets, new Comparator<T2<StreetEdge, Double>>() {

                @Override
                public int compare(T2<StreetEdge, Double> o1, T2<StreetEdge, Double> o2) {
                    return o1.second.compareTo(o2.second);
                }
            });
            
            if(transitConnections != null) {
                transitConnections.add(new TransitToStreetConnection((TransitStop)ts, stl, closestRoad.first, neighbourType));
            }
        
        } else {
            System.out.println("  NO");
        }
        return hasAWalkPathNear;
    }
    
    /**
     * Reads saved transitStop -> streetEdge connections and compares them with 
     * connections in generated Graph.
     * 
     * Graph is generated with {@link #loadGraph(java.lang.String, java.lang.String, boolean, boolean) } function
     * 
     * 
     * @param osm_filename filename for OSM (in resource folder of class)
     * @param gtfs_filename filename for GTFS (in resource folder of class)
     * @param wanted_con_filename filename for saved connections (in resource folder of class)
     * @throws Exception 
     */
    private void testTransitStreetConnections(String osm_filename, String gtfs_filename, String wanted_con_filename, String name) throws Exception {
        Graph gg = loadGraph(osm_filename, gtfs_filename, true, true);
        assertNotNull(gg);
        CsvWriter writer = null;
        PrintWriter pw = new PrintWriter("diffs/" + name + ".txt");
        if (transit_stats) {
            writer = new CsvWriter("diffs/transit_stats_" + name + ".csv", ':', Charset.forName("UTF8"));
            writer.writeRecord(new String[]{"stop_modes", "distance", "street_type", "street_modes", "stop_id", "stop_name"});
        }
        //Reads saved correct transit stop -> Street edge connections
        FileInputStream fis = new FileInputStream(getClass().getResource(wanted_con_filename).getFile());
        ObjectInputStream ois = new ObjectInputStream(fis);
        List<TransitStopConnToWantedEdge> outList = (List<TransitStopConnToWantedEdge>) ois.readObject();
        Map<String, TransitStopConnToWantedEdge> stop_id_toEdge = new HashMap<>(outList.size());
        for (TransitStopConnToWantedEdge stop_edge_con: outList) {
            if (transit_stats) {
                String mode = stop_edge_con.getTransitStop().getModes().toString();
                String street_modes = stop_edge_con.getStreetEdge().getPermission().toString();
                String dist = Double.toString(SphericalDistanceLibrary.fastDistance(stop_edge_con.getTransitStop().getCoordinate(), stop_edge_con.getStreetEdge().getGeometry()));
                String type = stop_edge_con.getStreetType().toString();
                writer.writeRecord(new String[]{mode, dist, type, street_modes, stop_edge_con.getStopID(), stop_edge_con.getTransitStop().getName()});
            }
            stop_id_toEdge.put(stop_edge_con.getStopID(), stop_edge_con);
        }
        if (transit_stats) {
            writer.close();
        }
        ois.close();
        fis.close();
	//Stops that are in tests but weren't connected to graph at all
        List<StreetFeature> missingStops = new ArrayList<>(stop_id_toEdge.size());

        List<TransitToStreetConnection> transitConnections = new ArrayList<>();
        //All found stops
        int allStops = 0;
        //Stops for which there is no correct connection
        int unknownStops = 0;
        //Stops which are linked to the same street as in correct connection
        int correctlyLinkedStops = 0;
        //For each transit stop in current graph check if transitStop is correctly connected
        for(TransitStop ts: Iterables.filter(gg.getVertices(), TransitStop.class)) {
	    //Used for checking if this stop has any connection
            int savedAllStops = allStops;
            //Each stop usually has 2 outgoing StreetTransit links each for a different direction
            for (StreetTransitLink e : Iterables.filter(ts.getOutgoing(), StreetTransitLink.class)) {
                allStops++;
                TransitStopConnToWantedEdge wanted_con = stop_id_toEdge.get(ts.getLabel());
                if (wanted_con == null) {
                    LOG.debug("Stop {} wasn't found in saved stops", ts.getLabel());
                    unknownStops++;
                } else {
                    Vertex connected_vertex = e.getToVertex();
                    String wantedEdgeLabel = wanted_con.getStreetEdge().getName();
                    StringBuilder sb = new StringBuilder();
                    boolean foundConnection = false;
                    //Look through all outgoing street edges on end vertex of StreetTransitLink
                    //and check if one street edge is correct one based on edge label
                    for (StreetEdge currentStreetEdge : Iterables.filter(connected_vertex.getOutgoingStreetEdges(), StreetEdge.class)) {
                        if (currentStreetEdge.getName().equals(wantedEdgeLabel)) {
                            //assertEquals(String.format("Transit stop %s connected correctly", ts.getLabel()), wantedEdgeLabel, se.getName());
                            collector.checkThat(currentStreetEdge.getName(), CoreMatchers.describedAs("TransitStop %0 connected correctly to %1", CoreMatchers.equalTo(wantedEdgeLabel), ts.getLabel(), wantedEdgeLabel));
                            transitConnections.add(new TransitToStreetConnection(wanted_con, (StreetTransitLink) e, true, currentStreetEdge));
                            foundConnection = true;
                            correctlyLinkedStops++;
                            break;
                        } else {
                            sb.append(currentStreetEdge.getName());
                            sb.append("|");
                        }
                    }
                    //If correct connection wasn't found we need to check incoming edges
                    //because sometimes connection is made right on OSM way split which means different labels.
                    if (!foundConnection) {
                        for (StreetEdge se : Iterables.filter(connected_vertex.getIncoming(), StreetEdge.class)) {
                            if (se.getName().equals(wantedEdgeLabel)) {
                                //assertEquals(String.format("Transit stop %s connected correctly", ts.getLabel()), wantedEdgeLabel, se.getName());
                                collector.checkThat(se.getName(), CoreMatchers.describedAs("TransitStop %0 connected correctly to %1", CoreMatchers.equalTo(wantedEdgeLabel), ts.getLabel(), wantedEdgeLabel));
                                transitConnections.add(new TransitToStreetConnection(wanted_con, (StreetTransitLink) e, true, se));
                                foundConnection = true;
                                correctlyLinkedStops++;
                                break;
                            } else {
                                sb.append(se.getName());
                                sb.append("|");
                            }

                        }
                    }
                    if (!foundConnection) {
                        pw.println(ts.getLabel() + " NOT CONNECTED");
                        //assertTrue(String.format("Transit stop %s connected wrongly", ts.getLabel()), foundConnection);
                        //collector.checkThat(sb.toString(), CoreMatchers.equalTo(wantedEdgeLabel));
                        collector.checkThat(sb.toString(), CoreMatchers.describedAs("TransitStop %0 should be connected to %1", CoreMatchers.equalTo(wantedEdgeLabel), ts.getLabel(), wantedEdgeLabel));
                        if (e.getToVertex().getOutgoingStreetEdges().size() > 0) {
                            transitConnections.add(new TransitToStreetConnection(wanted_con, (StreetTransitLink) e, false, (StreetEdge) e.getToVertex().getOutgoingStreetEdges().get(0)));
                        } else {
                            LOG.warn("To vertex has no outgoing edges");
                        }
                    }  else {
                        pw.println(ts.getLabel() + " CONNECTED");
                    }
                }
            }
            //no connections in this stop
            if (allStops == savedAllStops) {
                TransitStopConnToWantedEdge wanted_con = stop_id_toEdge.get(ts.getLabel());
                //This stop has wanted street link
                if (wanted_con != null) {
                    missingStops.addAll(TransitToStreetConnection.toStreetFeatureMissing(ts, wanted_con.getStreetEdge()));
                }
            }
        }
        
        LOG.info("Correctly linked {}/{} ({}%) stations for {}", correctlyLinkedStops, allStops-unknownStops, Math.round((double)correctlyLinkedStops/(double)(allStops-unknownStops)*100), osm_filename);
        LOG.info("Not checked: {} stations.", unknownStops);

        try {
            PrintWriter pw_readme = new PrintWriter(new FileWriter("diffs/readme.txt", true));
            pw_readme.println(String.format("Correctly linked %d/%d (%d%%) stations for %s", correctlyLinkedStops, allStops-unknownStops, Math.round((double)correctlyLinkedStops/(double)(allStops-unknownStops)*100), osm_filename));
            pw_readme.println(String.format("Not checked: %d stations.", unknownStops));
            pw_readme.println(String.format("All stops: %d", stop_id_toEdge.size()));
            pw_readme.close();
        } catch (IOException e) {
            LOG.error("Error:", e);
        }


        missingStops.addAll(TransitToStreetConnection.toFeatureCollection(transitConnections, TransitToStreetConnection.CollectionType.CORRECT_LINK).getFeatures());
        writeGeoJson("diffs/correct_" + name + ".geojson", new StreetFeatureCollection(missingStops));
        pw.close();
    }
    
    /**
     * Function is used to make size of test GTFS files by including only the necessary trips/routes/stops
     * 
     * Function outputs 5 CSV files in OTP home folder. With help of this files GTFS file is lowered.
     * OBA transformer was also tested, but size is lowered 2 times more this way.
     * 
     * @param osm_filename filename for OSM (in resource folder of class)
     * @param gtfs_filename filename for GTFS (in resource folder of class)
     * @param wanted_con_filename filename for saved connections (in resource folder of class)
     * @param name text name of this GTFS files
     * @throws Exception 
     */
    private void findNeededGTFSData(String osm_filename, String gtfs_filename, String wanted_con_filename, String name) throws Exception {
        Graph gg = loadGraph(osm_filename, gtfs_filename, true, true);
        assertNotNull(gg);
        gg.index(new DefaultStreetVertexIndexFactory());
        //Reads saved correct transit stop -> Street edge connections
        FileInputStream fis = new FileInputStream(getClass().getResource(wanted_con_filename).getFile());
        ObjectInputStream ois = new ObjectInputStream(fis);
        List<TransitStopConnToWantedEdge> outList = (List<TransitStopConnToWantedEdge>) ois.readObject();
        Set<String> stops = new HashSet<>(outList.size());
        Set<String> routes = new HashSet<>(outList.size());
        Set<String> trips = new HashSet<>(outList.size());
        Set<String> shapes = new HashSet<>(outList.size());
        Set<String> services = new HashSet<>(outList.size());

        for (TransitStopConnToWantedEdge stop_edge_con: outList) {
            Stop stop = stop_edge_con.getTransitStop().getStop();
            if (stops.contains(stop.getId().getId())) {
                continue;
            }
            Collection<TripPattern> patterns = gg.index.patternsForStop.get(stop);
            for (TripPattern pattern: patterns) {
                for (Trip trip: pattern.getTrips()) {
                    trips.add(trip.getId().getId());
                    //Shape ID is optional
                    try {
                        shapes.add(trip.getShapeId().getId());
                    } catch (NullPointerException nul) {}
                    services.add(trip.getServiceId().getId());
                    routes.add(trip.getRoute().getId().getId());
                }
            }
            stops.add(stop.getId().getId());
        }
        LOG.info("Stops: {}, routes: {}, trips: {}, shapes: {}, services: {}", stops.size(), routes.size(), trips.size(), shapes.size(), services.size());
        writeUsedGTFS(stops, name, "stop");
        writeUsedGTFS(routes, name, "route");
        writeUsedGTFS(trips, name, "trip");
        writeUsedGTFS(shapes, name, "shape");
        writeUsedGTFS(services, name, "service");
    }
    
    private void writeUsedGTFS(Set<String> ids, String name, String type) throws IOException {
        CsvWriter writer = new CsvWriter(type + "s_" + name + ".csv", ':', Charset.forName("UTF8"));
        writer.writeRecord(new String[]{type + "_ids"});
        for (String stop_id: ids) {
            writer.writeRecord(new String[]{stop_id});
        }
        writer.close();
    }

    @Rule
    public ErrorCollector collector = new ErrorCollector();
    
    @Test
    public void testMariborBus() throws Exception {
        testTransitStreetConnections("maribor_clean.osm.gz", "marprom_fake_gtfs_small.zip", "maribor.ser", "maribor");
        //findNeededGTFSData("maribor_clean.osm.gz", "marprom_fake_gtfs.zip", "maribor.ser", "maribor");
    }
    

    @Test
    public void testPortland() throws Exception {
        testTransitStreetConnections("portland_small.osm.pbf", "portland_small_gtfs.zip", "portland.ser", "portland");
        //findNeededGTFSData("washington_station.osm", "portland_gtfs.zip", "portland.ser", "portland");
    }
    
    @Test
    public void testMilano() throws Exception {
        testTransitStreetConnections("milan_italy_small.osm.pbf", "milano_small_gtfs.zip", "milano.ser", "milano");
        //findNeededGTFSData("milan_italy.osm.pbf", "Export_OpenDataTPL_Current.zip", "milano.ser", "milano");
    }
    
    
    
    //Creates wanted connections
    public void bootstrapWantedConnections() throws Exception {
        Graph gg = loadGraph("maribor_clean.osm.gz", "marprom_fake_gtfs.zip", true, true);
        //Graph gg = loadGraph("maribor_clean.osm.gz", "small.zip", true, true);
        gg.summarizeBuilderAnnotations();
        for (GraphBuilderAnnotation gba: gg.getBuilderAnnotations()) {
            if (gba instanceof StopUnlinked) {
                System.err.println(gba.getMessage());
            }
        }
        assertNotNull(gg);
        
        
        List<StreetTransitLink> streetTransitLinks = new ArrayList<>();
        //Which transit stop SHOULD BE connected to which edge
        List<TransitToStreetConnection> transitConnections = new ArrayList<>();
        //Otherwise all streetTransitLinks are duplicated. One for forward and one for backward
        Set<Integer> seenTransitStops = new HashSet<>();
        for(Vertex v: gg.getVertices()) {
            if (v instanceof TransitStop
                    && extent.contains(v.getCoordinate())) {
                
                for (Edge e : v.getOutgoing()) {
                    if (e instanceof StreetTransitLink
                            && !seenTransitStops.contains(v.getIndex())) {
                        streetTransitLinks.add((StreetTransitLink) e);
                        seenTransitStops.add(v.getIndex());
                        String edge_out = String.format("StreetTransitLink(%d, TransitStop{%d:%s} -> %s)", e.getId(), e.getFromVertex().getIndex(), e.getFromVertex().getName(), e.getToVertex());
                        System.out.println(edge_out);
                        boolean hasWalkNeighbours = findNeighbours(e.getFromVertex(), (StreetTransitLink)e, StreetType.WALK_BIKE, transitConnections);
                        if (!hasWalkNeighbours) {
                            
                            hasWalkNeighbours = findNeighbours(e.getFromVertex(), (StreetTransitLink)e, StreetType.SERVICE, transitConnections);
                            if (!hasWalkNeighbours) {
                                //HACK: Is street edge always the first in to vertex?
                                transitConnections.add(new TransitToStreetConnection((TransitStop)v, (StreetTransitLink)e, (StreetEdge)e.getToVertex().getOutgoingStreetEdges().get(0), StreetType.NORMAL));
                            }
                        }
                        
                    }
                }
            }
        }
        /*List<Double> distances = new ArrayList<>(streetTransitLinks.size());
        for (StreetTransitLink stl: Iterables.limit(streetTransitLinks, 20)) {
        //for (StreetTransitLink stl: streetTransitLinks) {
            Double distance = SphericalDistanceLibrary.getInstance().fastLength(stl.getGeometry());
            distances.add(distance);
            //System.out.println(String.format("%s;;%s", distance, stl));
            //findNeighbours(stl.getFromVertex());
        }*/
        

        /*System.err.println("Max distance:");
        T2<Double, Integer> maxVals = getMax(distances);
        StreetTransitLink maxLink = streetTransitLinks.get(maxVals.second);
        System.out.println(String.format("Dist:%s %s", maxVals.first, maxLink));
        
        System.err.println("Min distance:");
        System.err.println(Collections.min(distances));
        
        Collections.sort(distances);
        
        for (Double dist: distances) {
            System.err.println(dist);
        }
        */
        writeGeoJson("out_transit.geojson", TransitToStreetConnection.toFeatureCollection(transitConnections, TransitToStreetConnection.CollectionType.TRANSIT_LINK));
        writeGeoJson("out_wanted.geojson", TransitToStreetConnection.toFeatureCollection(transitConnections, TransitToStreetConnection.CollectionType.WANTED_LINK));
        writeSerial("wanted_transit.ser", TransitToStreetConnection.toSuper(transitConnections));
    }
    
    /**
     * Writes saved transit to street connections
     * @param filepath
     * @param transitToStreetConnections
     * @throws FileNotFoundException
     * @throws IOException 
     */
    private void writeSerial(String filepath,
        List<TransitStopConnToWantedEdge> transitToStreetConnections) throws FileNotFoundException, IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(filepath);
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
        objectOutputStream.writeObject(transitToStreetConnections);
        objectOutputStream.close();
        fileOutputStream.close();
        
    }
    
    /**
     * Writes Geojson of Features
     * @param filePath path where geojson is saved
     * @param streetFeatureCollection collection that is saved
     * @throws FileNotFoundException
     * @throws IOException 
     */
    private void writeGeoJson(String filePath,
        StreetFeatureCollection streetFeatureCollection) throws FileNotFoundException, IOException {

        FileOutputStream fileOutputStream = new FileOutputStream(filePath);
        PrintStream out = new PrintStream(fileOutputStream);
        ObjectMapper mapper = SerializerUtils.getMapper();

        SimpleModule module = new SimpleModule("GeoJSONSerializer", new Version(1, 0, 0, null, "com.fasterxml.jackson.module", "jackson-module-jaxb-annotations"));
        module.addSerializer(new GeoJSONSerializer(15));
        module.addSerializer(new StreetFeatureSerializer());
        module.addSerializer(new FeatureCollectionSerializer());

        mapper.registerModule(module);
        
        JsonGenerator jsonGenerator = mapper.getJsonFactory().createJsonGenerator(out);
        //jsonGenerator.setPrettyPrinter(new DefaultPrettyPrinter());
        mapper.writeValue(jsonGenerator, streetFeatureCollection);
    }   
    private static T2<Double, Integer> getMax(List<Double> list) {
        Double max = Double.MIN_VALUE;
        Integer max_index = null;
        for(int i=0; i < list.size(); i++) {
            Double cur = list.get(i);
            if (cur > max) {
                max = cur;
                max_index = i;
            }
        }
        
        return new T2(max, max_index);
        
    }
}
