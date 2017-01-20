package org.opentripplanner.graph_builder.module.osm;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.graph_builder.linking.SimpleStreetSplitter;
import org.opentripplanner.openstreetmap.model.OSMNode;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.AreaEdge;
import org.opentripplanner.routing.edgetype.PathwayEdge;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.OsmVertex;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PlatformLinker {

    public static GeometryFactory geometryFactory = GeometryUtils.getGeometryFactory();

    private Graph graph;
    private OSMDatabase osmdb;
    private SimpleStreetSplitter simpleStreetSplitter;

    public static final int MAX_SEARCH_RADIUS_METERS = 1000;

    public static final double DUPLICATE_WAY_EPSILON_METERS = 0.001;


    public PlatformLinker(Graph graph, OSMDatabase osmdb) {
        this.graph = graph;
        this.osmdb = osmdb;

        simpleStreetSplitter = new SimpleStreetSplitter(graph, null, null, false);
    }

    public void linkEntriesToPlatforms(){

        List<OsmVertex> endpoints = graph.getVertices().stream().
                filter(OsmVertex.class::isInstance).
                map(OsmVertex.class::cast).
                filter(this::isEndpoint).
                collect(Collectors.toList());


        List<Area> platforms = osmdb.getWalkableAreas().stream().
                filter(area -> "platform".equals(area.parent.getTag("public_transport"))).
                collect(Collectors.toList());

        for (OsmVertex endpoint : endpoints) {
            Vertex nearbyVertex = findCloesestVertex(endpoint);
            if (nearbyVertex == null) {
                continue;
            }

            for (Area area : platforms) {
                List<Ring> rings = area.outermostRings;
                boolean inArea = false;
                for (Ring ring : rings) {
                    for (OSMNode node : ring.nodes) {
                        if (((OsmVertex) nearbyVertex).nodeId == node.getId()) {
                            inArea = true;
                        }
                    }

                    if (inArea) {
                        for (OSMNode nodi : ring.nodes) {
                            Vertex vertexById = graph.getVertex("osm:node:" + nodi.getId());
                            if (vertexById != null) {
                                makePlatformEdges(endpoint, vertexById);
                            }

                        }
                    }
                }
            }
        }

    }

    private boolean isEndpoint(OsmVertex ov) {
        boolean isStairs = false;
        Vertex start = null;
        for (Edge e : ov.getIncoming()) {
            if (e instanceof StreetEdge) {
                StreetEdge se = (StreetEdge) e;
                if (se.isStairs()) {
                    isStairs = true;
                    start = se.getFromVertex();
                    break;
                }
            }
        }

        if (isStairs && start != null) {
            boolean isEndpoint = true;
            for (Edge se : ov.getOutgoing()) {
                if (!se.getToVertex().getCoordinate().equals(start.getCoordinate()) && !(se instanceof AreaEdge)) {
                    isEndpoint = false;
                }
            }
            return isEndpoint;
        }
        return false;
    }

    private Vertex findCloesestVertex(Vertex vertex) {

        final double radiusDeg = SphericalDistanceLibrary.metersToDegrees(MAX_SEARCH_RADIUS_METERS);
        double duplicateDeg = SphericalDistanceLibrary.metersToDegrees(DUPLICATE_WAY_EPSILON_METERS);
        final double xscale = Math.cos(vertex.getLat() * Math.PI / 180);
        final TIntDoubleMap distances = new TIntDoubleHashMap();
        Envelope env = new Envelope(vertex.getCoordinate());
        env.expandBy(radiusDeg / xscale, radiusDeg);

        List<StreetEdge> nearbyStreetEdges = simpleStreetSplitter.findNearbyStreetEdges(vertex, TraverseMode.WALK, distances, xscale, radiusDeg);

        nearbyStreetEdges = nearbyStreetEdges.stream().filter(streetEdge -> !vertex.getIncoming().contains(streetEdge)).
                collect(Collectors.toList());

        nearbyStreetEdges = nearbyStreetEdges.stream().filter(streetEdge -> !vertex.getOutgoing().contains(streetEdge)).
                collect(Collectors.toList());

        nearbyStreetEdges = filterEdgesInConnectedRing((OsmVertex) vertex, nearbyStreetEdges);

        if (!nearbyStreetEdges.isEmpty()) {
            // find the best edges
            List<StreetEdge> bestEdges = simpleStreetSplitter.findBestEdges(duplicateDeg, distances, nearbyStreetEdges);

            for (StreetEdge edge : bestEdges) {
                if (edge instanceof AreaEdge) {
                    return edge.getToVertex();
                }
            }
            return null;
        }
        return null;
    }

    private List<StreetEdge> filterEdgesInConnectedRing(OsmVertex vertex, List<StreetEdge> candidates) {

        List<StreetEdge> connectedRing = new ArrayList<>();

        for (Edge edge : vertex.getOutgoing()) {
            if(edge instanceof AreaEdge){

                Vertex currentVertex = edge.getToVertex();
                Vertex oldVertex = vertex;
                boolean isRing = false;
                for (int i = 0; i < 50; i++) {

                    Vertex newToVertex = null;
                    for(Edge currentOutgoingEdge : currentVertex.getOutgoing()) {
                        Vertex toVertex = currentOutgoingEdge.getToVertex();
                        if(!toVertex.getLabel().equals(oldVertex.getLabel())){
                            newToVertex = toVertex;
                            connectedRing.addAll(currentVertex.getOutgoing().stream().
                                    filter(AreaEdge.class::isInstance).map(StreetEdge.class::cast).collect(Collectors.toList()));
                            connectedRing.addAll(currentVertex.getIncoming().stream().
                                    filter(AreaEdge.class::isInstance).map(StreetEdge.class::cast).collect(Collectors.toList()));
                        }
                    }

                    if(newToVertex == null){
                        for(Edge currentIncommingEdge : currentVertex.getIncoming()) {
                            Vertex fromVertex = currentIncommingEdge.getFromVertex();
                            if(!fromVertex.getLabel().equals(oldVertex.getLabel())){
                                newToVertex = fromVertex;
                                connectedRing.addAll(currentVertex.getOutgoing().stream().
                                        map(StreetEdge.class::cast).collect(Collectors.toList()));
                                connectedRing.addAll(currentVertex.getIncoming().stream().
                                        map(StreetEdge.class::cast).collect(Collectors.toList()));
                            }
                        }
                    }

                    if(newToVertex != null && !newToVertex.getLabel().equals(vertex.getLabel())){
                        currentVertex = newToVertex;
                        oldVertex = currentVertex;
                    }else{
                        isRing = true;
                        break;
                    }
                }
                if(!isRing){
                    return candidates;
                }else{
                    break ;
                }
            }

        }


        //filter
        List<StreetEdge> filtered = new ArrayList<>();
        for(StreetEdge candidate : candidates){

            boolean exclude = false;
            for (StreetEdge excluded : connectedRing){
                if(candidate.getId() == excluded.getId()){
                    exclude = true;
                }
            }

            if(!exclude){
                filtered.add(candidate);
            }
        }

        return filtered;
    }

    private void makePlatformEdges(OsmVertex from, Vertex to) {
        new PathwayEdge(from, to, 0);
        new PathwayEdge(to, from, 0);
    }

}
