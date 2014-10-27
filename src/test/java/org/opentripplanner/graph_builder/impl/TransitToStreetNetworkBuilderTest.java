/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opentripplanner.graph_builder.impl;

import org.opentripplanner.util.StreetType;
import com.bedatadriven.geojson.GeoJsonModule;
import com.fasterxml.jackson.core.JsonGenerator;
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
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.graph_builder.annotation.GraphBuilderAnnotation;
import org.opentripplanner.graph_builder.annotation.StopUnlinked;
import org.opentripplanner.graph_builder.impl.osm.DefaultWayPropertySetSource;
import org.opentripplanner.graph_builder.impl.osm.OpenStreetMapGraphBuilderImpl;
import org.opentripplanner.graph_builder.model.GtfsBundle;
import org.opentripplanner.model.json_serialization.SerializerUtils;
import org.opentripplanner.openstreetmap.impl.FileBasedOpenStreetMapProviderImpl;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTransitLink;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
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
    
    @Before
    public void setUp() {
        extra = new HashMap<Class<?>, Object>();
    }
    
    /**
     * Creates graph from OSM and GTFS data and runs {@link TransitToTaggedStopsGraphBuilderImpl} and {@link TransitToStreetNetworkGraphBuilderImpl}.
     * @param osm_filename filename for OSM (in resource folder of class)
     * @param gtfs_filename filename for GTFS (in resource folder of class)
     * @param wanted_con_filename filename for saved connections (in resource folder of class)
     * @throws Exception 
     */
    private Graph loadGraph(String osm_filename, String gtfs_filename,
            boolean taggedBuilder, boolean normalBuilder) throws Exception{

        Graph gg = new Graph();

        OpenStreetMapGraphBuilderImpl loader = new OpenStreetMapGraphBuilderImpl();
        loader.setDefaultWayPropertySetSource(new DefaultWayPropertySetSource());
        FileBasedOpenStreetMapProviderImpl provider = new FileBasedOpenStreetMapProviderImpl();
        loader.skipVisibility = true;
        loader.staticParkAndRide = true;
        
        PruneFloatingIslands pfi = new PruneFloatingIslands();

        File file = new File(getClass().getResource(osm_filename).getFile());
        
        File gtfs_file = new File(getClass().getResource(gtfs_filename).getFile());
        List<GtfsBundle> gtfsBundles = Lists.newArrayList();
        
        GtfsBundle gtfsBundle = new GtfsBundle(gtfs_file);
        gtfsBundle.setTransfersTxtDefinesStationPaths(false);
        gtfsBundles.add(gtfsBundle);
        
        GtfsGraphBuilderImpl gtfsBuilder = new GtfsGraphBuilderImpl(gtfsBundles);

        provider.setPath(file);
        loader.setProvider(provider);

        loader.buildGraph(gg, extra);
        
        extent = gg.getExtent();
                
        //pfi.buildGraph(gg, extra);
        
        index = new StreetVertexIndexServiceImpl(gg);
        
        gtfsBuilder.buildGraph(gg, extra);
        
        if (taggedBuilder) {
            TransitToTaggedStopsGraphBuilderImpl transitToTaggedStopsGraphBuilderImpl = new TransitToTaggedStopsGraphBuilderImpl();
            transitToTaggedStopsGraphBuilderImpl.buildGraph(gg, extra);
        }
        
        if (normalBuilder) {
            TransitToStreetNetworkGraphBuilderImpl transitToStreetNetworkGraphBuilderImpl = new TransitToStreetNetworkGraphBuilderImpl();
            transitToStreetNetworkGraphBuilderImpl.buildGraph(gg, extra);
        }
        
        return gg;
    }
    
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
            Double distance = SphericalDistanceLibrary.getInstance().fastDistance(ts.getCoordinate(), se.getGeometry());
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
     * Graph is generated with loadGraph function
     * 
     * 
     * @param osm_filename filename for OSM (in resource folder of class)
     * @param gtfs_filename filename for GTFS (in resource folder of class)
     * @param wanted_con_filename filename for saved connections (in resource folder of class)
     * @throws Exception 
     */
    private void testBus(String osm_filename, String gtfs_filename, String wanted_con_filename, String name) throws Exception {
        Graph gg = loadGraph(osm_filename, gtfs_filename, true, true);
        assertNotNull(gg);
        
        //Reads saved correct transit stop -> Street edge connections
        FileInputStream fis = new FileInputStream(getClass().getResource(wanted_con_filename).getFile());
        ObjectInputStream ois = new ObjectInputStream(fis);
        List<TransitStopConnToWantedEdge> outList = (List<TransitStopConnToWantedEdge>) ois.readObject();
        Map<String, TransitStopConnToWantedEdge> stop_id_toEdge = new HashMap<>(outList.size());
        for (TransitStopConnToWantedEdge stop_edge_con: outList) {
            stop_id_toEdge.put(stop_edge_con.getStopID(), stop_edge_con);
        }
        ois.close();
        fis.close();
        
        List<TransitToStreetConnection> transitConnections = new ArrayList<>();
        int allStops = 0;
        int correctlyLinkedStops = 0;
        //For each transit stop in current graph check if transitStop is correctly connected
        for(Vertex v: gg.getVertices()) {
            if (v instanceof TransitStop
                    && extent.contains(v.getCoordinate())) {
                 for (Edge e : v.getOutgoing()) {
                    if (e instanceof StreetTransitLink) {
                        allStops++;
                        TransitStop ts = (TransitStop) v;
                        TransitStopConnToWantedEdge wanted_con = stop_id_toEdge.get(ts.getLabel());
                        if (wanted_con == null) {
                            LOG.warn("Stop {} wasn't found in saved stops", ts.getLabel());
                        } else {
                           Vertex connected_vertex = e.getToVertex();
                           String wantedEdgeLabel = wanted_con.getStreetEdge().getLabel();
                           StringBuilder sb = new StringBuilder();
                           boolean foundConnection = false;
                           //Look through all outgoing street edges 
                           //and check if street edge is correct one based on edge label
                           for (Edge con_edge: connected_vertex.getOutgoingStreetEdges()) {
                               StreetEdge se = (StreetEdge) con_edge;
                               if (se.getLabel().equals(wantedEdgeLabel)) {
                                   //assertEquals(String.format("Transit stop %s connected correctly", ts.getLabel()), wantedEdgeLabel, se.getLabel());
                                   collector.checkThat(se.getLabel(),CoreMatchers.describedAs("TransitStop %0 connected correctly to %1", CoreMatchers.equalTo(wantedEdgeLabel), ts.getLabel(), wantedEdgeLabel));
                                   transitConnections.add(new TransitToStreetConnection(wanted_con, (StreetTransitLink) e, true));
                                   foundConnection = true;
                                   correctlyLinkedStops++;
                                   break;
                               } else {
                                   sb.append(se.getLabel());
                                   sb.append("|");
                               }
                           }
                           //If correct connection wasn't found we need to check incoming edges
                           //because sometimes connection is made right on OSM way split which means different labels.
                           if (!foundConnection) {                               
                               for (Edge con_edge : connected_vertex.getIncoming()) {
                                   if (con_edge instanceof StreetEdge) {
                                       StreetEdge se = (StreetEdge) con_edge;
                                       if (se.getLabel().equals(wantedEdgeLabel)) {
                                           //assertEquals(String.format("Transit stop %s connected correctly", ts.getLabel()), wantedEdgeLabel, se.getLabel());
                                           collector.checkThat(se.getLabel(),CoreMatchers.describedAs("TransitStop %0 connected correctly to %1", CoreMatchers.equalTo(wantedEdgeLabel), ts.getLabel(), wantedEdgeLabel));
                                           transitConnections.add(new TransitToStreetConnection(wanted_con, (StreetTransitLink) e, true));
                                           foundConnection = true;
                                           correctlyLinkedStops++;
                                           break;
                                       } else {
                                           sb.append(se.getLabel());
                                           sb.append("|");
                                       }
                                   }
                               }
                           }
                           if (!foundConnection) {
                               //assertTrue(String.format("Transit stop %s connected wrongly", ts.getLabel()), foundConnection);
                               //collector.checkThat(sb.toString(), CoreMatchers.equalTo(wantedEdgeLabel));
                               collector.checkThat(sb.toString(), CoreMatchers.describedAs("TransitStop %0 was wrongly connected to %1 insted of %2", CoreMatchers.equalTo(wantedEdgeLabel), ts.getLabel(), sb.toString(), wantedEdgeLabel));
                               transitConnections.add(new TransitToStreetConnection(wanted_con, (StreetTransitLink) e, false));
                           }
                        }
                    }
                }
            }
        }
        
        LOG.info("Correctly linked {}/{} ({}%) stations for {}", correctlyLinkedStops, allStops, Math.round((double)correctlyLinkedStops/(double)allStops*100), osm_filename);
        
        writeGeoJson("correct_" + name +".geojson", TransitToStreetConnection.toFeatureCollection(transitConnections, TransitToStreetConnection.CollectionType.CORRECT_LINK));
    }
    
    @Rule
    public ErrorCollector collector = new ErrorCollector();
    
    @Test
    public void testMariborBus() throws Exception {
        testBus("maribor_clean.osm.gz", "marprom_fake_gtfs.zip", "maribor_transit.ser", "maribor");
    }
    
    //Creates wanted connections for Maribor
    public void makeTestMariborBus() throws Exception {
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
        
        GeoJsonModule module = new GeoJsonModule();
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
