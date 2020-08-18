package org.opentripplanner.graph_builder.linking;

import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.linearref.LinearLocation;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.core.vehicle_sharing.VehicleDescription;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.error.TrivialPathException;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.location.TemporaryStreetLocation;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TemporaryRentVehicleVertex;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * Tries to link vertices to all closest edges in graph. If possible, it maset links directly to vertexes
 * via {@link EdgesMaker}, or else it delegates creating links to {@link ToEdgeLinker}
 */
public class ToStreetEdgeLinker {

    private final ToEdgeLinker toEdgeLinker;

    private final EdgesToLinkFinder edgesToLinkFinder;

    private final LinkingGeoTools linkingGeoTools;

    private final EdgesMaker edgesMaker;

    public ToStreetEdgeLinker(ToEdgeLinker toEdgeLinker, EdgesToLinkFinder edgesToLinkFinder, LinkingGeoTools linkingGeoTools,
                              EdgesMaker edgesMaker) {
        this.toEdgeLinker = toEdgeLinker;
        this.edgesToLinkFinder = edgesToLinkFinder;
        this.linkingGeoTools = linkingGeoTools;
        this.edgesMaker = edgesMaker;
    }

    /**
     * Temporarily link this vertex into the graph (make connection from `origin` or to `destination`)
     */
    public boolean linkTemporarily(TemporaryStreetLocation vertex, TraverseMode traverseMode, RoutingRequest options)
            throws TrivialPathException {
        List<StreetEdge> streetEdges = edgesToLinkFinder.findEdgesToLink(vertex, traverseMode);
        streetEdges.forEach(edge -> linkTemporarilyToEdge(vertex, edge, options));
        return !streetEdges.isEmpty();
    }

    /**
     * Temporarily link this vertex to graph in both directions (make connections both to and from `vertex`)
     */
    public boolean linkTemporarilyBothWays(TemporaryRentVehicleVertex vertex, VehicleDescription vehicle) {
        List<StreetEdge> streetEdges = getStreetEdgesForceIncludeWalkEdge(vertex, vehicle);
        streetEdges.forEach(edge -> linkTemporarilyToEdgeBothWays(vertex, edge));
        return !streetEdges.isEmpty();
    }

    /**
     * Permanently link this vertex into the graph (make connections both to and from `vertex` and remove original edge)
     */
    public boolean linkPermanently(Vertex vertex, TraverseMode traverseMode) {
        List<StreetEdge> streetEdges = edgesToLinkFinder.findEdgesToLink(vertex, traverseMode);
        streetEdges.forEach(edge -> linkPermanentlyToEdge(vertex, edge));
        return !streetEdges.isEmpty();
    }

    private void linkTemporarilyToEdge(TemporaryStreetLocation vertex, StreetEdge edge, RoutingRequest options)
            throws TrivialPathException {
        LineString orig = edge.getGeometry();
        LinearLocation ll = linkingGeoTools.findLocationClosestToVertex(vertex, orig);
        Optional<Vertex> maybeVertexToLinkTo = maybeFindVertexToLinkTo(edge, orig, ll);
        if (maybeVertexToLinkTo.isPresent()) {
            edgesMaker.makeTemporaryEdges(vertex, maybeVertexToLinkTo.get());
        } else {
            //This throws runtime TrivialPathException if same edge is split in origin and destination link
            options.canSplitEdge(edge);
            toEdgeLinker.linkVertexToEdgeTemporarily(vertex, edge, ll);
        }
    }

    private List<StreetEdge> getStreetEdgesForceIncludeWalkEdge(TemporaryRentVehicleVertex vertex,
                                                                VehicleDescription vehicle) {
        List<StreetEdge> streetEdgesForRiding = edgesToLinkFinder.findEdgesToLinkVehicle(vertex, vehicle);
        if (streetEdgesForRiding.stream().anyMatch(edge -> edge.canTraverse(new TraverseModeSet(TraverseMode.WALK)))) {
            return streetEdgesForRiding;
        }
        List<StreetEdge> streetEdgesForWalking = edgesToLinkFinder.findEdgesToLink(vertex, TraverseMode.WALK);
        return Stream.concat(streetEdgesForRiding.stream(), streetEdgesForWalking.stream())
                .distinct()
                .collect(toList());
    }

    private void linkTemporarilyToEdgeBothWays(TemporaryRentVehicleVertex vertex, StreetEdge edge) {
        LineString orig = edge.getGeometry();
        LinearLocation ll = linkingGeoTools.findLocationClosestToVertex(vertex, orig);
        Optional<Vertex> maybeVertexToLinkTo = maybeFindVertexToLinkTo(edge, orig, ll);
        if (maybeVertexToLinkTo.isPresent()) {
            edgesMaker.makeTemporaryEdgesBothWays(vertex, maybeVertexToLinkTo.get());
        } else {
            toEdgeLinker.linkVertexToEdgeBothWaysTemporarily(vertex, edge, ll);
        }
    }

    private void linkPermanentlyToEdge(Vertex vertex, StreetEdge edge) {
        // TODO: we've already built this line string, we should save it
        LineString orig = edge.getGeometry();
        LinearLocation ll = linkingGeoTools.findLocationClosestToVertex(vertex, orig);
        Optional<Vertex> maybeVertexToLinkTo = maybeFindVertexToLinkTo(edge, orig, ll);
        if (maybeVertexToLinkTo.isPresent()) {
            edgesMaker.makePermanentEdges(vertex, (StreetVertex) maybeVertexToLinkTo.get());
        } else {
            toEdgeLinker.linkVertexToEdgePermanently(vertex, edge, ll);
        }
    }

    private Optional<Vertex> maybeFindVertexToLinkTo(StreetEdge edge, LineString lineString, LinearLocation ll) {
        // If we're very close to one end of the line or the other, or endwise, don't bother to split,
        // cut to the chase and link directly
        if (linkingGeoTools.isLocationAtTheBeginning(ll)) {
            return Optional.of(edge.getFromVertex());
        }
        if (linkingGeoTools.isLocationExactlyAtTheEnd(ll, lineString) || linkingGeoTools.isLocationAtTheEnd(ll, lineString)) {
            return Optional.of(edge.getToVertex());
        }
        return Optional.empty();
    }
}
