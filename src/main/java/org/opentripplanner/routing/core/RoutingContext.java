package org.opentripplanner.routing.core;

import com.google.common.collect.Sets;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.routing.algorithm.astar.strategies.EuclideanRemainingWeightHeuristic;
import org.opentripplanner.routing.algorithm.astar.strategies.RemainingWeightHeuristic;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.response.InputField;
import org.opentripplanner.routing.api.response.RoutingError;
import org.opentripplanner.routing.api.response.RoutingErrorCode;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.TemporaryFreeEdge;
import org.opentripplanner.routing.edgetype.TemporaryPartialStreetEdge;
import org.opentripplanner.routing.error.GraphNotFoundException;
import org.opentripplanner.routing.error.RoutingValidationException;
import org.opentripplanner.routing.graph.DisposableEdgeCollection;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TemporaryVertex;
import org.opentripplanner.util.NonLocalizedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A RoutingContext holds information needed to carry out a search for a particular TraverseOptions, on a specific graph.
 * Includes things like (temporary) endpoint vertices, transfer tables, service day caches, etc.
 *
 * In addition, while the RoutingRequest should only carry parameters _in_ to the routing operation, the routing context
 * should be used to carry information back out, such as debug figures or flags that certain thresholds have been exceeded.
 */
public class RoutingContext implements Cloneable {

    private static final Logger LOG = LoggerFactory.getLogger(RoutingContext.class);

    /* FINAL FIELDS */

    public RoutingRequest opt; // not final so we can reverse-clone

    public final Graph graph;

    public final Set<Vertex> fromVertices;

    public final Set<Vertex> toVertices;

    public final Set<FeedScopedId> bannedRoutes;

    private final Set<DisposableEdgeCollection> tempEdges;
    
    // The back edge associated with the origin - i.e. continuing a previous search.
    // NOTE: not final so that it can be modified post-construction for testing.
    // TODO(flamholz): figure out a better way.
    public Edge originBackEdge;

    public RemainingWeightHeuristic remainingWeightHeuristic;

    /** Indicates that the search timed out or was otherwise aborted. */
    public boolean aborted;

    /** Indicates that a maximum slope constraint was specified but was removed during routing to produce a result. */
    public boolean slopeRestrictionRemoved = false;

    /* CONSTRUCTORS */

    /**
     * Constructor that automatically computes origin/target from RoutingRequest.
     */
    public RoutingContext(RoutingRequest routingRequest, Graph graph) {
        this(routingRequest, graph, (Vertex)null, null, true);
    }

    /**
     * Constructor that takes to/from vertices as input.
     */
    public RoutingContext(RoutingRequest routingRequest, Graph graph, Vertex from, Vertex to) {
        this(routingRequest, graph, from, to, false);
    }

    public RoutingContext(RoutingRequest routingRequest, Graph graph, Set<Vertex> from, Set<Vertex> to) {
        this(routingRequest, graph, from, to, false);
    }

    /**
     * Returns the StreetEdges that overlap between two vertices' edge sets.
     * It does not look at the TemporaryPartialStreetEdges, but the real parents
     * of these edges.
     */
    private Set<StreetEdge> overlappingParentStreetEdges(Vertex u, Vertex v) {
        // Fetch the parent edges so we aren't stuck with temporary edges.
        Set<StreetEdge> vEdges = getConnectedParentEdges(v);
        Set<StreetEdge> uEdges = getConnectedParentEdges(u);
        return Sets.intersection(uEdges, vEdges);
    }

    /**
     * Find all parent edges ({@link TemporaryPartialStreetEdge#getParentEdge()}) for
     * {@link Vertex#getIncoming()} and {@link Vertex#getIncoming()} edges.
     * Edges of other types are ignored.
     */
    private static Set<StreetEdge> getConnectedParentEdges(Vertex loc) {
        return Stream.concat(loc.getIncoming().stream(), loc.getOutgoing().stream())
                .filter(it -> it instanceof TemporaryPartialStreetEdge)
                .map(it -> ((TemporaryPartialStreetEdge)it).getParentEdge())
                .collect(Collectors.toSet());
    }

    /**
     * Creates a PartialStreetEdge along the input StreetEdge iff its direction makes this possible.
     */
    private void makePartialEdgeAlong(StreetEdge streetEdge, StreetVertex from, StreetVertex to, DisposableEdgeCollection tempEdges) {
        LineString parent = streetEdge.getGeometry();
        LineString head = GeometryUtils.getInteriorSegment(parent,
                streetEdge.getFromVertex().getCoordinate(), from.getCoordinate());
        LineString tail = GeometryUtils.getInteriorSegment(parent,
                to.getCoordinate(), streetEdge.getToVertex().getCoordinate());

        if (parent.getLength() > head.getLength() + tail.getLength()) {
            LineString partial = GeometryUtils.getInteriorSegment(parent,
                    from.getCoordinate(), to.getCoordinate());

            double lengthRatio = partial.getLength() / parent.getLength();
            double length = streetEdge.getDistanceMeters() * lengthRatio;

            //TODO: localize this
            String name = from.getLabel() + " to " + to.getLabel();
            tempEdges.addEdge(new TemporaryPartialStreetEdge(streetEdge, from, to, partial, new NonLocalizedString(name), length));
        }
    }

    /**
     * Flexible constructor which may compute to/from vertices.
     * 
     * TODO(flamholz): delete this flexible constructor and move the logic to constructors above appropriately.
     * 
     * @param findPlaces if true, compute origin and target from RoutingRequest using spatial indices.
     */
    private RoutingContext(
            RoutingRequest routingRequest,
            Graph graph,
            Set<Vertex> from,
            Set<Vertex> to,
            boolean findPlaces
    ) {
        if (graph == null) {
            throw new GraphNotFoundException();
        }
        this.opt = routingRequest;
        this.graph = graph;
        this.tempEdges = new HashSet<>();

        Set<Vertex> fromVertices;
        Set<Vertex> toVertices;

        if (findPlaces) {
            // normal mode, search for vertices based RoutingRequest and split streets
            fromVertices = graph.streetIndex.getVerticesForLocation(
                opt.from,
                opt,
                false,
                tempEdges
            );
            toVertices = graph.streetIndex.getVerticesForLocation(
                opt.to,
                opt,
                true,
                tempEdges
            );

        } else {
            // debug mode, force endpoint vertices to those specified rather than searching
            fromVertices = from;
            toVertices = to;
        }

        this.fromVertices = routingRequest.arriveBy ? toVertices : fromVertices;
        this.toVertices = routingRequest.arriveBy ? fromVertices : toVertices;

        if (graph.index != null) {
            this.bannedRoutes = routingRequest.getBannedRoutes(graph.index.getAllRoutes());
        } else {
            this.bannedRoutes = Collections.emptySet();
        }

        adjustForSameFromToEdge();

        remainingWeightHeuristic = new EuclideanRemainingWeightHeuristic();
    }

    private RoutingContext(
            RoutingRequest routingRequest,
            Graph graph,
            Vertex from,
            Vertex to,
            boolean findPlaces
    ) {
        this(
                routingRequest,
                graph,
                Collections.singleton(from),
                Collections.singleton(to),
                findPlaces
        );
    }

    /**
     * If the from and to vertices are generated and lie along some of the same edges, we need to wire
     * them up along those edges so that we don't get odd circuitous routes for really short trips.
     */
    private void adjustForSameFromToEdge() {
        DisposableEdgeCollection tempEdges = new DisposableEdgeCollection(graph);
        this.tempEdges.add(tempEdges);

        if (fromVertices != null && toVertices != null) {
            Set<StreetVertex> fromStreetVertices = new HashSet<>();
            for (Vertex from : fromVertices) {
                if (from == null) { continue; }
                for (Edge outgoing : from.getOutgoing()) {
                    Vertex toVertex = outgoing.getToVertex();
                    if (outgoing instanceof TemporaryFreeEdge && toVertex instanceof StreetVertex
                        && toVertex
                        .getOutgoing()
                        .stream()
                        .anyMatch(edge -> edge instanceof TemporaryPartialStreetEdge)) {
                        // The vertex is connected with an TemporaryFreeEdge connector to the
                        // TemporaryPartialStreetEdge
                        fromStreetVertices.add((StreetVertex) toVertex);
                    }
                    else if (outgoing instanceof TemporaryPartialStreetEdge
                        && from instanceof StreetVertex) {
                        fromStreetVertices.add((StreetVertex) from);
                    }
                }
            }

            Set<StreetVertex> toStreetVertices = new HashSet<>();
            for (Vertex to : toVertices) {
                if (to == null) { continue; }
                for (Edge incoming : to.getIncoming()) {
                    Vertex fromVertex = incoming.getFromVertex();
                    if (incoming instanceof TemporaryFreeEdge && fromVertex instanceof StreetVertex
                        && fromVertex
                        .getIncoming()
                        .stream()
                        .anyMatch(edge -> edge instanceof TemporaryPartialStreetEdge)) {
                        // The vertex is connected with an TemporaryFreeEdge connector to the
                        // TemporaryPartialStreetEdge
                        toStreetVertices.add((StreetVertex) fromVertex);
                    }
                    else if (incoming instanceof TemporaryPartialStreetEdge
                        && to instanceof StreetVertex) {
                        toStreetVertices.add((StreetVertex) to);
                    }
                }
            }

            for (StreetVertex fromStreetVertex : fromStreetVertices) {
                for (StreetVertex toStreetVertex : toStreetVertices) {
                    Set<StreetEdge> overlap = overlappingParentStreetEdges(fromStreetVertex,
                        toStreetVertex
                    );
                    for (StreetEdge pse : overlap) {
                        makePartialEdgeAlong(pse, fromStreetVertex, toStreetVertex, tempEdges);
                    }
                }
            }
        }
    }

    /* INSTANCE METHODS */

    public void checkIfVerticesFound() {
        List<RoutingError> routingErrors = new ArrayList<>();

        // check origin present when not doing an arrive-by batch search
        if (fromVertices == null && !(opt.oneToMany == true && opt.arriveBy == true)) {
            routingErrors.add(
                new RoutingError(RoutingErrorCode.LOCATION_NOT_FOUND, InputField.FROM_PLACE)
            );
        }

        // check destination present when not doing a depart-after batch search
        if (toVertices == null && !(opt.oneToMany == true && opt.arriveBy == false)) {
            routingErrors.add(
                new RoutingError(RoutingErrorCode.LOCATION_NOT_FOUND, InputField.TO_PLACE)
            );
        }

        if (routingErrors.size() > 0) {
            throw new RoutingValidationException(routingErrors);
        }
    }

    /**
     * Tear down this routing context, removing any temporary edges from
     * the "permanent" graph objects. This enables all temporary objects
     * for garbage collection.
     */
    public void destroy() {
        this.tempEdges.forEach(DisposableEdgeCollection::disposeEdges);
    }
}