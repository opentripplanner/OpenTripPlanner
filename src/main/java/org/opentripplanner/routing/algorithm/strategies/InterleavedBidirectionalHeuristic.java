package org.opentripplanner.routing.algorithm.strategies;

import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.pqueue.BinHeap;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.StreetTransitLink;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.location.StreetLocation;
import org.opentripplanner.routing.spt.DominanceFunction;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.TemporaryVertex;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.routing.vertextype.TransitVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * This the goal direction heuristic used for transit searches.
 *
 * Euclidean heuristics are terrible for transit routing because the maximum transit speed is quite high, especially
 * relative to the walk speed. Transit can require going away from the destination in Euclidean space to approach it
 * according to the travel time metric. This heuristic is designed to be good for transit.
 *
 * After many experiments storing travel time metrics in tables or embedding them in low-dimensional Euclidean space I
 * eventually came to the conclusion that the most efficient structure for representing the metric was already right
 * in front of us: a graph.
 *
 * This heuristic searches backward from the target vertex over the street and transit network, removing any
 * time-dependent component of the network (e.g. by evaluating all boarding wait times as zero). This produces an
 * admissible heuristic (i.e. it always underestimates path weight) making it valid independent of the clock time.
 * This is important because you don't know precisely what time you will arrive at the destination until you get there.
 *
 * Because we often make use of the first path we find in the main search, this heuristic must be both admissible and
 * consistent (monotonic). If the heuristic is non-monotonic, nodes can be re-discovered and paths are not necessarily
 * discovered in order of increasing weight. When finding paths one by one and banning trips or routes,
 * suboptimal paths may be found and reported before or instead of optimal ones.
 *
 * This heuristic was previously not consistent for the reasons discussed in ticket #2153. It was possible for the
 * "zero zone" around the origin to overlap the egress zone around the destination, leading to decreases in the
 * heuristic across an edge that were greater in magnitude than the weight of that edge. This has been solved by
 * creating two separate distance maps, one pre-transit and one post-transit.
 *
 * Note that the backward search does not happen in a separate thread. It is interleaved with the main search in a
 * ratio of N:1 iterations.
 *
 * This heuristic effectively makes traversal of non-transit vertices far away from the origin and
 * destination impossible by returning an estimated weight of Inifinity for vertices not within
 * walking or driving distance of the origin or destination.  The heuristic is initialized before
 * the main graph search and attempts to discover all non-transit verticies up to a certain limit.
 * Transit vertices are also explored, but only during the main graph search in the
 * {@link InterleavedBidirectionalHeuristic#doSomeWork} method.  During the main graph search, this
 * heuristic estimates remaining weights of vertices by returning the lower bound weights at
 * vertices found during the heuristic initialization and doSomeWork method.
 *
 * The limits encountered are currently either the {@link RoutingRequest#maxWalkDistance} or the
 * {@link RoutingRequest#maxPreTransitTime}.  The maxWalkDistance currently applies to both walking
 * and bicycling.  When traversing the network in a car, the maxWalkDistance is not incremented.
 * To mask off excessive travel by car, the maxPreTransitTime is used as a limiter.  However for
 * driving a car after using transit (for example with either a TNC, car rental or ride and kiss)
 * a limitation is added in {@link InterleavedBidirectionalHeuristic#streetSearch} to stop searching
 * through the graph once the origin has been found.
 *
 * In order for this heuristic to return the proper estimated weights during the main graph search,
 * it must be sensitive to the current mode of the current state in the main graph search.  For
 * example, in Park and Ride searches, the initialization of the heuristic will have explored many
 * more vertices while driving compared to the number of vertices explored by walking from the
 * origin/destination.  Therefore in the
 * {@link InterleavedBidirectionalHeuristic#estimateRemainingWeight} method, a mode-specific weight
 * is returned when a non-transit vertex is being evaluated.
 *
 * Shown below are a few visualizations of what the heuristic will do in various searches.
 *
 * Key:
 * O = origin vertex
 * D = destination vertex
 * W = reachable by walking up to maxWalkDistance
 * C = reachable by car up to the maxPreTransitTime
 * B = reachable by car or walking
 * - = vertices unreachable by either mode
 *
 * In a simple TRANSIT,WALK search, only the vertices within walking distance are marked as
 * reachable by the heuristic.
 *
 * ----------------
 * ----WWW-----WWW-
 * ----WOW-----WDW-
 * ----WWW-----WWW-
 * ----------------
 *
 * In a TRANSIT,WALK,CAR (park and ride) search, there will be vertices reachable by both walking
 * and driving, some only by walking and some only reachable by driving.  The park and ride search
 * does not allow driving after transit, so the vertices around the destination are only reachable
 * by walking.
 *
 * ----CCC---------
 * ---CBBBC----WWW-
 * ---CBOWCC---WDW-
 * ---CBWWCC---WWW-
 * ----CCCC--------
 *
 * In a TRANSIT,WALK,CAR_HAIL (transit + TNC) search, there are many more vertices explored while
 * searching from the destination in this current implementation.  The preTransitVertices may still
 * look about the same as the above graphic, but the postTransitVertices could look as follows:
 *
 * --------CCCCCCC-
 * ------CCCCCCBBBC
 * -----OCCCC--WDBC
 * ------CCCCC-BWBC
 * -------CCCCCCCCC
 *
 * This heuristic is espeically useful for large graphs, but could be turned off entirely for small
 * graphs.
 */
public class InterleavedBidirectionalHeuristic implements RemainingWeightHeuristic {

    private static final long serialVersionUID = 20160215L;

    private static Logger LOG = LoggerFactory.getLogger(InterleavedBidirectionalHeuristic.class);

    // For each step in the main search, how many steps should the reverse search proceed?
    private static final int HEURISTIC_STEPS_PER_MAIN_STEP = 8; // TODO determine a good value empirically

    /** The vertex at which the main search begins. */
    Vertex origin;

    /** The vertex that the main search is working towards. */
    Vertex target;

    /**
     * A map of all vertices within walking (or driving) distance of the origin (the vertex at which the
     * main search begins) and their corresponding mode-sensitive lower bound weights.
     */
    Map<Vertex, VertexModeWeight> preTransitVertices;

    /**
     * A lower bound on the weight of the lowest-cost path to the target (the vertex at which the
     * main search ends) from each vertex within walking (or driving) distance of the target. In
     * previous OTP versions, this map used to also store transit vertex weights, but that data is
     * now stored in {@link InterleavedBidirectionalHeuristic#transitVertexWeights}.
     */
    Map<Vertex, VertexModeWeight> postTransitVertices;

    /**
     * A map containing a lower bound on weight of a path to the destination from transit vertices.
     * This includes only transit vertices, not street or other vertices reached before or after
     * using transit.
     */
    TObjectDoubleMap<Vertex> transitVertexWeights;

    Graph graph;

    RoutingRequest routingRequest;

    // The maximum weight yet seen at a closed node in the reverse search. The priority queue head has a uniformly
    // increasing weight, so any unreached transit node must have greater weight than this.
    double maxWeightSeen = 0;

    // The priority queue for the interleaved backward search through the transit network.
    BinHeap<Vertex> transitQueue;

    // True when the entire transit network has been explored by the reverse search.
    boolean finished = false;

    // The speed for calculating the pre-transit remaining weight with a euclidean heuristic
    private double remainingDistanceSpeed;

    // A cache of pre-transit remaining weights based on a vertex as a key
    private Map<Vertex, Double> preTransitRemainingWeightEstimates;

    // A threshold in meters beyond which the pre-transit euclidean heuristic is applied in order
    // to save time with trip calculations
    private final double PRE_TRANSIT_EUCLIDEAN_WALK_DISTANCE_THRESHOLD = 5000; // TODO determine a good value empirically

    // A flag for whether the pre-transit euclidean heuristic should be used
    private boolean useEuclideanRemainingWeightEstimateForPreTransitVertices;


    /**
     * Before the main search begins, the heuristic must search on the streets around the origin and destination.
     * This also sets up the initial states for the reverse search through the transit network, which progressively
     * improves lower bounds on travel time to the target to guide the main search.
     */
    @Override
    public void initialize(RoutingRequest request, long abortTime) {
        Vertex target = request.rctx.target;
        if (target == this.target) {
            LOG.debug("Reusing existing heuristic, the target vertex has not changed.");
            return;
        }
        LOG.debug("Initializing heuristic computation.");
        this.graph = request.rctx.graph;
        long start = System.currentTimeMillis();
        this.target = target;
        this.routingRequest = request;
        request.softWalkLimiting = false;
        request.softPreTransitLimiting = false;
        // change the defaults in bikeWalkingOptions because traversals of one-way streets or
        // otherwise non-traversable streets may occur while walking a bicycle that extend the
        // pretransit search further than is needed
        request.bikeWalkingOptions.softWalkLimiting = false;
        request.bikeWalkingOptions.softPreTransitLimiting = false;
        transitQueue = new BinHeap<>();
        // Forward street search first, mark street vertices around the origin so H evaluates to 0.
        preTransitVertices = streetSearch(request, false, abortTime);
        if (preTransitVertices == null) {
            return; // Search timed out
        }
        LOG.debug("end forward street search {} ms", System.currentTimeMillis() - start);
        postTransitVertices = streetSearch(request, true, abortTime);
        if (postTransitVertices == null) {
            return; // Search timed out
        }
        LOG.debug("end backward street search {} ms", System.currentTimeMillis() - start);

        // initialize the transit vertices to be an empty map.  Transit vertices will be added later
        // in the doSomeWork method.
        transitVertexWeights = new TObjectDoubleHashMap<>(100, 0.5f, Double.POSITIVE_INFINITY);

        // Set the remaining distance speed to the upper bound speed of the modes available in the
        // routing request.
        remainingDistanceSpeed = request.getStreetSpeedUpperBound();

        // Initialize the pre-transit remaining weight cache
        preTransitRemainingWeightEstimates = new HashMap<>();

        // In certain cases, it will make sense to use a euclidean heuristic for estimating the
        // remaining weight of pre-transit vertices. In transit+walk searches, this is generally not
        // needed because the weight of existing states increases rapidly enough such that not all
        // pre-transit vertices will be explored before the transitVertexWeights of nearby transit
        // stops jump to the beginning of the priority queue. However, with the bicycle or car modes
        // many thousands of extra vertices could be explored before the transit vertices rise to
        // the top of the priority queue which can slow down the graph search enough that not as
        // many itineraries would be generated in various queries. Therefore, whenever the car mode
        // is enabled, or the bicycle mode is enabled with at least 5km allowed bicycling, a flag is
        // activated so that a euclidean heuristic will be used to estimate the remaining weight.
        useEuclideanRemainingWeightEstimateForPreTransitVertices = request.modes.getCar() || (
            request.modes.getBicycle() &&
                request.maxWalkDistance > PRE_TRANSIT_EUCLIDEAN_WALK_DISTANCE_THRESHOLD
        );

        // once street searches are done, raise the limits to max
        // because hard walk limiting is incorrect and is observed to cause problems
        // for trips near the cutoff.
        request.setMaxWalkDistance(Double.POSITIVE_INFINITY);
        request.setMaxPreTransitTime(Integer.MAX_VALUE);

        LOG.debug("initialized SSSP");
        request.rctx.debugOutput.finishedPrecalculating();
    }

    /**
     * This function supplies the main search with an (under)estimate of the remaining path weight to the target.
     * No matter how much progress has been made on the reverse heuristic search, we must return an underestimate
     * of the cost to reach the target (i.e. the heuristic must be admissible).
     * All on-street vertices within walking (or driving) distance of the origin or destination will
     * have been explored by the heuristic before the main search starts.
     */
    @Override
    public double estimateRemainingWeight (State s) {
        final Vertex v = s.getVertex();
        if (v instanceof TransitVertex) {
            // The main search is on transit. If the current vertex has been explored during the
            // doSomeWork method, then return the stored lower bound. Otherwise return the highest
            // lower bound yet seen -- this location must have a higher cost than that.
            double h = transitVertexWeights.get(v);
            if (h == Double.POSITIVE_INFINITY) {
                return maxWeightSeen;
            } else {
                return h;
            }
        } else if (v instanceof StreetLocation || v instanceof TemporaryVertex) {
            // Temporary vertices or StreetLocations might not be found in the street searches.
            // These can be temporary vertices created for the origin or destination vertex or also
            // the temporary vertices that link the split street to the origin/destination. This
            // can also include other vertices used to create items such as bike rental stations.
            // Zero is always an underestimate.
            return 0;
        } else if (!s.isEverBoarded() && preTransitVertices.containsKey(v)) {
            // The current search state is not on transit, is not a temporary vertex and hasn't
            // boarded transit yet. Calculate the remaining weight of the vertex based on the
            // current non-transit mode. This differentiation is needed because during the
            // pre-transit search, the same vertex could be explored in a driving state or a
            // walking state or both. Therefore, the vertices need to be assigned different weights
            // for when a vertex is unreachable via a specific mode.
            VertexModeWeight weight = preTransitVertices.get(v);
            TraverseMode nonTransitMode = s.getNonTransitMode();
            if (nonTransitMode != null && nonTransitMode.isDriving()) {
                return getPreTransitRemainingWeightEstimate(v, weight.carWeight);
            } else {
                return getPreTransitRemainingWeightEstimate(v, weight.walkWeight);
            }
        } else if (s.isEverBoarded() && postTransitVertices.containsKey(v)) {
            // The main search has boarded transit and is on a vertex that was reachable during the
            // post-transit heuristic initialization. Return the mode-dependent lower bound weight
            // found during the heuristic initialization. This can return Infinity if this vertex
            // was unreachable during the heuristic initialization with the current mode.
            VertexModeWeight weight = postTransitVertices.get(v);
            TraverseMode nonTransitMode = s.getNonTransitMode();
            if (nonTransitMode != null && nonTransitMode.isDriving()) {
                return weight.carWeight;
            } else {
                return weight.walkWeight;
            }
        }

        // The main search is not on a StreetLocation, TemporaryVertex or TransitVertex and hasn't
        // been explored during the appropriate pre or post transit initialization of the heuristic.
        // Therefore, it is unreachable.
        return Double.POSITIVE_INFINITY;
    }

    /**
     * Calculate the estimated pre-transit remaining weight for a vertex.
     *
     * If the vertex was visited by a certain mode during the streetSearch it will have a
     * non-infinite value which indicates that it was reachable (within the maxWalkDistance for mode
     * combinations that exclude CAR or within the maxPreTransitTime for mode combos that do include
     * the CAR mode). If the vertex was not reachable, the method always returns Infinity which
     * results in the vertex not being explored further in the graph search. If the vertex was
     * reachable, a non-infinite value is returned. If the
     * useEuclideanRemainingWeightEstimateForPreTransitVertices flag is set to true, a euclidean
     * remaining weight is calculated (and stored in a cache) based off of the remaining distance
     * and the speed that the remaining distance could be covered. Otherwise, it is assumed that all
     * pre-transit vertices are equally as likely to have the same remaining weight to the target,
     * so an underestimate of 0 is returned.
     */
    private double getPreTransitRemainingWeightEstimate(Vertex v, double preTransitStreetSearchWeight) {
        return preTransitStreetSearchWeight == Double.POSITIVE_INFINITY
               ? Double.POSITIVE_INFINITY
               : useEuclideanRemainingWeightEstimateForPreTransitVertices
                 ? preTransitRemainingWeightEstimates.computeIfAbsent(
                   v,
                   (vertex) -> SphericalDistanceLibrary.fastDistance(
                       v.getLat(),
                       v.getLon(),
                       target.getLat(),
                       target.getLon()
                   ) / remainingDistanceSpeed
                 )
                 : 0;
    }

    @Override
    public void reset() { }

    /**
     * Move backward N steps through the transit network.
     * This improves the heuristic's knowledge of the transit network as seen from the target,
     * making its lower bounds on path weight progressively more accurate.
     */
    @Override
    public void doSomeWork() {
        if (finished) return;
        for (int i = 0; i < HEURISTIC_STEPS_PER_MAIN_STEP; ++i) {
            if (transitQueue.empty()) {
                finished = true;
                break;
            }
            int uWeight = (int) transitQueue.peek_min_key();
            Vertex u = transitQueue.extract_min();
            // The weight of the queue head is uniformly increasing.
            // This is the highest weight ever seen for a closed vertex.
            maxWeightSeen = uWeight;
            // Now that this vertex is closed, we can store its weight for use as a lower bound / heuristic value.
            // We don't implement decrease-key operations though, so check whether a smaller value is already known.
            double uWeightOld = transitVertexWeights.get(u);
            if (uWeight < uWeightOld) {
                // Including when uWeightOld is infinite because the vertex is not yet closed.
                transitVertexWeights.put(u, uWeight);
            } else {
                // The vertex was already closed. This time it necessarily has a higher weight, so skip it.
                continue;
            }
            // This search is proceeding backward relative to the main search.
            // When the main search is arriveBy the heuristic search looks at OUTgoing edges.
            for (Edge e : routingRequest.arriveBy ? u.getOutgoing() : u.getIncoming()) {
                // Do not enter streets in this phase, which should only touch transit.
                if (e instanceof StreetTransitLink) {
                    continue;
                }
                Vertex v = routingRequest.arriveBy ? e.getToVertex() : e.getFromVertex();
                double edgeWeight = e.weightLowerBound(routingRequest);
                // INF heuristic value indicates unreachable (e.g. non-running transit service)
                // this saves time by not reverse-exploring those routes and avoids maxFound of INF.
                if (Double.isInfinite(edgeWeight)) {
                    continue;
                }
                double vWeight = uWeight + edgeWeight;
                double vWeightOld = transitVertexWeights.get(v);
                if (vWeight < vWeightOld) {
                    // Should only happen when vWeightOld is infinite because it is not yet closed.
                    transitQueue.insert(v, vWeight);
                }
            }
        }
    }

    /**
     * Explore the streets around the origin or target, recording the minimum weight of a path to each street vertex.
     * When searching around the target, also retain the states that reach transit stops since we'll want to
     * explore the transit network backward, in order to guide the main forward search.
     *
     * The main search always proceeds from the "origin" to the "target" (names remain unchanged in arriveBy mode).
     * The reverse heuristic search always proceeds outward from the target (name remains unchanged in arriveBy).
     *
     * When the main search is departAfter:
     * it gets outgoing edges and traverses them with arriveBy=false,
     * the heuristic search gets incoming edges and traverses them with arriveBy=true,
     * the heuristic destination street search also gets incoming edges and traverses them with arriveBy=true,
     * the heuristic origin street search gets outgoing edges and traverses them with arriveBy=false.
     *
     * When main search is arriveBy:
     * it gets incoming edges and traverses them with arriveBy=true,
     * the heuristic search gets outgoing edges and traverses them with arriveBy=false,
     * the heuristic destination street search also gets outgoing edges and traverses them with arriveBy=false,
     * the heuristic origin street search gets incoming edges and traverses them with arriveBy=true.
     * The streetSearch method traverses using the real traverse method rather than the lower bound traverse method
     * because this allows us to keep track of the distance walked.
     * Perhaps rather than tracking walk distance, we should just check the straight-line radius and
     * only walk within that distance. This would avoid needing to call the main traversal functions.
     *
     * This initial search from the origin or destination will usually naturally terminate before
     * searching the entire graph.  Since this search does not enter transit, a few limits are
     * reached that constrain this initial search.  In a search with walking or biking, the
     * maxWalkDistance is typically encountered.  In a search with driving such as park and ride,
     * kiss and ride or ride and kiss, either the maxPreTransitTime or maxWalkDistance will be
     * encountered.  When driving to a park and ride or kiss and ride the max walk distance isn't
     * encountered, so the maxPreTransitTime acts as the limiter from searching the whole graph.
     * However, on the other part of the park and ride/kiss and ride, only walking to transit is
     * allowed, so the maxWalkDistance limit is encountered.  The code for each of these limitations
     * can be found in {@link org.opentripplanner.routing.edgetype.StreetEdge#doTraverse}.
     *
     * However, we do need to limit the post-transit search in here to account for the possibility
     * of hailing a car after taking transit or renting a car after transit.  Since the calculation
     * of the estimated remaining weight after transit will be the largest weight seen, we can
     * safely cut off the post-transit search once the origin is found.
     */
    private Map<Vertex, VertexModeWeight> streetSearch (RoutingRequest rr, boolean fromTarget, long abortTime) {
        LOG.debug("Heuristic street search around the {}.", fromTarget ? "target" : "origin");
        rr = rr.clone();
        if (fromTarget) {
            rr.setArriveBy(!rr.arriveBy);
        }
        // Create a map that returns Infinity when it does not contain a vertex.
        Map<Vertex, VertexModeWeight> vertices = new HashMap<>();
        ShortestPathTree spt = new DominanceFunction.MinimumWeight().getNewShortestPathTree(rr);
        // TODO use normal OTP search for this.
        BinHeap<State> pq = new BinHeap<State>();
        Vertex initVertex = fromTarget ? rr.rctx.target : rr.rctx.origin;
        State initState = new State(initVertex, rr);
        pq.insert(initState, 0);
        while ( ! pq.empty()) {
            if (abortTime < Long.MAX_VALUE  && System.currentTimeMillis() > abortTime) {
                return null;
            }
            State s = pq.extract_min();
            Vertex v = s.getVertex();
            // At this point the vertex is closed (pulled off heap).
            // This is the lowest cost we will ever see for this vertex. We can record the cost to reach it.
            if (v instanceof TransitStop) {
                // We don't want to continue into the transit network yet, but when searching around the target
                // place vertices on the transit queue so we can explore the transit network backward later.
                if (fromTarget) {
                    double weight = s.getWeight();
                    transitQueue.insert(v, weight);
                    if (weight > maxWeightSeen) {
                        maxWeightSeen = weight;
                    }
                }
                continue;
            }
            // We don't test whether we're on an instanceof StreetVertex here because some other vertex types
            // (park and ride or bike rental related) that should also be explored and marked as usable.

            // Get the mode-dependent weights for the current vertex, or create an instance if this
            // vertex hasn't been explored yet.
            VertexModeWeight weights;
            if (!vertices.containsKey(v)) {
                weights = new VertexModeWeight();
                vertices.put(v, weights);
            } else {
                weights = vertices.get(v);
            }

            // Set the weight depending on the current non-transit mode
            // Always keep the minimum weight for a lower-bound estimate of weight. This is to avoid
            // situations where a vertex could be traversed twice with a different mode for example
            // with a bicycle rental trip.
            TraverseMode nonTransitMode = s.getNonTransitMode();
            if (nonTransitMode != null && s.getNonTransitMode().isDriving()) {
                weights.carWeight = Math.min(s.getWeight(), weights.carWeight);
            } else {
                weights.walkWeight = Math.min(s.getWeight(), weights.walkWeight);
            }

            // if searching from the target and traveling via car and the origin has been reached,
            // immediately return the vertices so that the entire graph isn't searched.
            if (fromTarget && rr.modes.getCar() && v == rr.rctx.origin) {
                break;
            }

            // Attempt to traverse all edges going in the direction desired
            for (Edge e : rr.arriveBy ? v.getIncoming() : v.getOutgoing()) {
                // arriveBy has been set to match actual directional behavior in this subsearch.
                // Max walk distance cutoff or pre transit time cutoff will happen in the street
                // edge traversal method.
                for (State s1 = e.traverse(s); s1 != null; s1 = s1.getNextResult()) {
                    // Add all states that are derived from traversing the edge.  Sometimes a fork state
                    // will be encountered and those need to be added to the shortest path tree as well.
                    if (spt.add(s1)) {
                        pq.insert(s1,  s1.getWeight());
                    }
                }
            }
        }
        LOG.debug("Heuristic street search hit {} vertices.", vertices.size());
        LOG.debug("Heuristic street search hit {} transit stops.", transitQueue.size());
        return vertices;
    }

    /**
     * A simple class to keep track of mode-specific lower bound weights at vertices explored during
     * the heuristic initialization in the {@link InterleavedBidirectionalHeuristic#streetSearch}
     * method.  Values are set to infinity by default to indicate an unreachable vertex.  These
     * values are then used in the {@link InterleavedBidirectionalHeuristic#estimateRemainingWeight}
     * method.
     */
    static class VertexModeWeight {
        public double walkWeight = Double.POSITIVE_INFINITY;
        public double carWeight = Double.POSITIVE_INFINITY;
    }
}