package org.opentripplanner.graph_builder.module;

import com.google.common.collect.Iterables;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.routing.edgetype.StreetTransitLink;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.StreetVertexIndexServiceImpl;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * This module takes advantage of the fact that in some cities, an authoritative linking location for GTFS stops is
 * provided by tags in the OSM data.
 *
 * When OSM data is being loaded, certain OSM nodes that represent transit stops are made into TransitStopStreetVertex
 * instances. In some cities, these nodes have a ref=* tag which gives the corresponding GFTS stop ID for the stop.
 * See http://wiki.openstreetmap.org/wiki/Tag:highway%3Dbus_stop
 *
 * This module will attempt to link all transit stops to such nodes in the OSM data, based on the stop ID and ref tag.
 * It is run before the main transit stop linker, and if no linkage was created here, the main linker should create
 * one based on distance or other heuristics.
 */
public class TransitToTaggedStopsModule implements GraphBuilderModule {
    
    private static final Logger LOG = LoggerFactory.getLogger(TransitToTaggedStopsModule.class);
    
    private double searchRadiusM = 250;
    private double searchRadiusLat = SphericalDistanceLibrary.metersToDegrees(searchRadiusM);
    private String vertexConnectorName;
    private StreetVertexIndexServiceImpl indexService;
    
    public TransitToTaggedStopsModule(String vertexConnectorName) {
        this.vertexConnectorName = vertexConnectorName;
    }
    
    public List<String> provides() {
        return Arrays.asList("street to transit", "linking");
    }
    
    public List<String> getPrerequisites() {
        return Arrays.asList("streets"); // why not "transit" ?
    }
    
    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {
        LOG.info("Linking transit stops to tagged bus stops...");
        indexService = new StreetVertexIndexServiceImpl(graph);
        
        // Iterate over a copy of vertex list because it will be modified
        List<Vertex> vertices = new ArrayList<>(graph.getVertices());
        VertexConnector connector = VertexConnectorFactory.getVertexConnector(vertexConnectorName);
        
        Iterables.filter(vertices, TransitStop.class).forEach(
            stop -> createLinkOnCondition(stop, connector)
        );
    }
    
    private void createLinkOnCondition(TransitStop stop, VertexConnector connector) {
        // If the street is already linked, there is no need to link it again,
        // Could happen if using the prune isolated island
        boolean stopAlreadyLinked = stop.getOutgoing().stream().anyMatch(link -> link instanceof StreetTransitLink);
        
        if (stopAlreadyLinked)
            return;
        
        // Only connect transit stops that (a) are entrances, or (b) have no associated entrances
        if (!stop.isEntrance() && stop.hasEntrances())
            return;
        
        boolean linkCreated = createLinkIfStopCodeExists(stop, connector);
        
        if (!linkCreated) {
            LOG.debug("Could not connect {} at {}", stop.getStopCode(), stop.getCoordinate().toString());
            //LOG.warn(graph.addBuilderAnnotation(new StopUnlinked(ts)));
        }
    }
    
    private boolean createLinkIfStopCodeExists(TransitStop stop, VertexConnector connector) {
        if (stop.getStopCode() == null)
            return false;
        
        boolean wheelchairAccessible = stop.hasWheelchairEntrance();
        Collection<Vertex> vertices  = findTransitStopVertices(stop);
        return connector.connectVertex(stop, wheelchairAccessible, vertices);
    }
    
    private Collection<Vertex> findTransitStopVertices(TransitStop stop) {
        Envelope searchEnvelope = createSearchEnvelope(stop.getCoordinate());
        return indexService.getVerticesForEnvelope(searchEnvelope);
    }
    
    private Envelope createSearchEnvelope(Coordinate coordinate) {
        double xScale = Math.cos(coordinate.y * Math.PI / 180);
        Envelope envelope = new Envelope(coordinate);
        envelope.expandBy(searchRadiusLat / xScale, searchRadiusLat);
        return envelope;
    }
    
    @Override
    public void checkInputs() {
        //no inputs
    }
    
}
