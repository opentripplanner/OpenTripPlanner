/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opentripplanner.graph_builder.impl;

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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
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

/**
 *
 * @author mabu
 */
public class TransitToStreetNetworkBuilderTest extends TestCase {
    private HashMap<Class<?>, Object> extra;
    StreetVertexIndexServiceImpl index;
    private Envelope extent;
    private double searchRadiusM = 10;
    private double searchRadiusLat = SphericalDistanceLibrary.metersToDegrees(searchRadiusM);
    
    @Before
    public void setUp() {
        extra = new HashMap<Class<?>, Object>();
    }
    
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
    
    public boolean findNeighbours(Vertex ts, StreetTransitLink stl, List<TransitToStreetConnection> transitConnections) {
        Envelope envelope = new Envelope(ts.getCoordinate());
        double xscale = Math.cos(ts.getCoordinate().y * Math.PI / 180);
        envelope.expandBy(searchRadiusLat / xscale, searchRadiusLat);
        Geometry poly = GeometryUtils.getGeometryFactory().toGeometry(envelope);
        Collection<Edge> edges = index.getEdgesForEnvelope(envelope);
        List<T2<StreetEdge, Double>> candidateStreets = new ArrayList<>(edges.size());
        boolean hasAWalkPathNear = false;
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
            if ((se.getPermission().allows(StreetTraversalPermission.PEDESTRIAN)
                    || (se.getPermission().allows(StreetTraversalPermission.BICYCLE)))
                    && !(se.getPermission().allows(StreetTraversalPermission.CAR))) {
                candidateStreets.add(new T2<>(se, distance));
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
                transitConnections.add(new TransitToStreetConnection((TransitStop)ts, stl, closestRoad.first, StreetType.WALK_BIKE));
            }
        
        } else {
            System.out.println("  NO");
        }
        return hasAWalkPathNear;
    }
    
    @Test
    public void testMariborBus() throws Exception {
        Graph gg = loadGraph("maribor_clean.osm.gz", "marprom_winter_arriva_block.zip", true, true);
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
                        boolean hasWalkNeighbours = findNeighbours(e.getFromVertex(), (StreetTransitLink)e, transitConnections);
                        if (!hasWalkNeighbours) {
                            //HACK: Is street edge always the first in to vertex?
                            transitConnections.add(new TransitToStreetConnection((TransitStop)v, (StreetTransitLink)e, (StreetEdge)e.getToVertex().getOutgoingStreetEdges().get(0), StreetType.NORMAL));
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
    }
    
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
