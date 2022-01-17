package org.opentripplanner.routing.core;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.ext.dataoverlay.routing.DataOverlayContext;
import org.opentripplanner.graph_builder.linking.SameEdgeAdjuster;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.algorithm.astar.strategies.EuclideanRemainingWeightHeuristic;
import org.opentripplanner.routing.algorithm.astar.strategies.RemainingWeightHeuristic;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.response.InputField;
import org.opentripplanner.routing.api.response.RoutingError;
import org.opentripplanner.routing.api.response.RoutingErrorCode;
import org.opentripplanner.routing.error.GraphNotFoundException;
import org.opentripplanner.routing.error.RoutingValidationException;
import org.opentripplanner.graph_builder.linking.DisposableEdgeCollection;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TransitStopVertex;
import org.opentripplanner.util.OTPFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

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

    /**
     * DataOverlay Sandbox module context.
     */
    public DataOverlayContext dataOverlayContext;

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
        this.dataOverlayContext = OTPFeature.DataOverlay.isOnElseNull(() ->
            new DataOverlayContext(graph.dataOverlayParameterBindings, routingRequest.dataOverlay)
        );

        Set<Vertex> fromVertices;
        Set<Vertex> toVertices;

        if (findPlaces) {
            // normal mode, search for vertices based RoutingRequest and split streets
            fromVertices = graph.getStreetIndex().getVerticesForLocation(
                opt.from,
                opt,
                false,
                tempEdges
            );
            toVertices = graph.getStreetIndex().getVerticesForLocation(
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

        if (fromVertices != null && toVertices != null) {
            for (Vertex fromVertex : fromVertices) {
                for (Vertex toVertex : toVertices) {
                    tempEdges.add(SameEdgeAdjuster.adjust(fromVertex, toVertex, graph));
                }
            }
        }

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

    /* INSTANCE METHODS */

    public void checkIfVerticesFound() {
        List<RoutingError> routingErrors = new ArrayList<>();

        // check that vertices where found if from-location was specified
        if (opt.from.isSpecified() && isDisconnected(fromVertices, true)) {
            routingErrors.add(new RoutingError(
                getRoutingErrorCodeForDisconnected(opt.from),
                InputField.FROM_PLACE
            ));
        }

        // check that vertices where found if to-location was specified
        if (opt.to.isSpecified() && isDisconnected(toVertices, false)) {
            routingErrors.add(new RoutingError(
                getRoutingErrorCodeForDisconnected(opt.to),
                InputField.TO_PLACE
            ));
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


    /* PRIVATE METHODS */

    private boolean isDisconnected(Set<Vertex> vertices, boolean isFrom) {
        // Not connected if linking was not attempted, and vertices were not specified in the request.
        if (vertices == null) { return true; }

        Predicate<Vertex> isNotTransit = Predicate.not(TransitStopVertex.class::isInstance);
        Predicate<Vertex> hasNoIncoming = v -> v.getIncoming().isEmpty();
        Predicate<Vertex> hasNoOutgoing = v -> v.getOutgoing().isEmpty();

        // Not connected if linking did not create incoming/outgoing edges depending on the
        // direction and the end.
        Predicate<Vertex> isNotConnected = (isFrom == opt.arriveBy) ? hasNoIncoming : hasNoOutgoing;

        return vertices.stream().allMatch(isNotTransit.and(isNotConnected));
    }

    private RoutingErrorCode getRoutingErrorCodeForDisconnected(GenericLocation location) {
        Coordinate coordinate = location.getCoordinate();
        GeometryFactory gf = GeometryUtils.getGeometryFactory();
        return coordinate != null && graph.getConvexHull().disjoint(gf.createPoint(coordinate))
                ? RoutingErrorCode.OUTSIDE_BOUNDS
                : RoutingErrorCode.LOCATION_NOT_FOUND;
    }
}