package org.opentripplanner.graph_builder.linking;

import org.opentripplanner.common.geometry.HashGridSpatialIndex;
import org.opentripplanner.graph_builder.annotation.BikeParkUnlinked;
import org.opentripplanner.graph_builder.annotation.BikeRentalStationUnlinked;
import org.opentripplanner.graph_builder.annotation.StopUnlinked;
import org.opentripplanner.graph_builder.services.DefaultStreetEdgeFactory;
import org.opentripplanner.graph_builder.services.StreetEdgeFactory;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.StreetTransitLink;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.BikeParkVertex;
import org.opentripplanner.routing.vertextype.BikeRentalStationVertex;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

/**
 * This class links transit stops to streets by splitting the streets (unless the stop is extremely close to the street
 * intersection).
 * <p>
 * This code is intended to be deterministic in linking to streets, independent of the order in which the JVM decides to
 * iterate over Maps and even in the presence of points that are exactly halfway between multiple candidate linking
 * points.
 * <p>
 * [LEGACY] See discussion in pull request #1922, follow up issue #1934, and the original issue calling for replacement
 * of the stop linker, #1305.
 */
public class PermanentStreetSplitter {

    private static final Logger LOG = LoggerFactory.getLogger(PermanentStreetSplitter.class);

    private final Graph graph;

    private final HashGridSpatialIndex<Edge> idx;

    private final ToStreetEdgeLinker toStreetEdgeLinker;

    public PermanentStreetSplitter(Graph graph, HashGridSpatialIndex<Edge> hashGridSpatialIndex, ToStreetEdgeLinker toStreetEdgeLinker) {
        this.graph = graph;
        this.idx = hashGridSpatialIndex;
        this.toStreetEdgeLinker = toStreetEdgeLinker;
    }

    /**
     * Construct a new PermanentStreetSplitter.
     * NOTE: Only one PermanentStreetSplitter should be active on a graph at any given time.
     * NOTE: Whole module should share the same index of edges as we add new edges to it.
     *
     * @param graph
     * @param index                If not null this index is used instead of creating new one
     * @param addExtraEdgesToAreas True if we want to add {@link org.opentripplanner.routing.edgetype.AreaEdge}
     *                             when linking edges to areas
     */
    public static PermanentStreetSplitter createNewDefaultInstance(Graph graph, @Nullable HashGridSpatialIndex<Edge> index,
                                                                   boolean addExtraEdgesToAreas) {
        if (index == null) {
            index = LinkingGeoTools.createHashGridSpatialIndex(graph);
        }
        StreetEdgeFactory streetEdgeFactory = new DefaultStreetEdgeFactory();
        EdgesMaker edgesMaker = new EdgesMaker();
        LinkingGeoTools linkingGeoTools = new LinkingGeoTools();
        BestCandidatesGetter bestCandidatesGetter = new BestCandidatesGetter();
        StreetSplitter splitter = new StreetSplitter(graph, index);
        EdgesToLinkFinder edgesToLinkFinder = new EdgesToLinkFinder(index, linkingGeoTools, bestCandidatesGetter);
        ToEdgeLinker toEdgeLinker = new ToEdgeLinker(streetEdgeFactory, splitter, edgesMaker, linkingGeoTools, addExtraEdgesToAreas);
        ToStreetEdgeLinker toStreetEdgeLinker = new ToStreetEdgeLinker(toEdgeLinker, edgesToLinkFinder, linkingGeoTools, edgesMaker);
        return new PermanentStreetSplitter(graph, index, toStreetEdgeLinker);
    }

    public HashGridSpatialIndex<Edge> getIdx() {
        return idx;
    }

    /**
     * Link all relevant vertices to the street network
     */
    public void link() {
        for (Vertex v : graph.getVertices()) {
            if (hasToBeLinked(v)) {
                if (!link(v)) {
                    logLinkingFailure(v);
                }
            }
        }
    }

    /**
     * Link this vertex into the graph to the closest walkable edge
     */
    public boolean link(Vertex vertex) {
        return toStreetEdgeLinker.linkPermanently(vertex, TraverseMode.WALK);
    }

    private boolean hasToBeLinked(Vertex v) {
        if (v instanceof TransitStop || v instanceof BikeRentalStationVertex || v instanceof BikeParkVertex) {
            return v.getOutgoing().stream().noneMatch(e -> e instanceof StreetTransitLink); // not yet linked
        }
        return false;
    }

    private void logLinkingFailure(Vertex v) {
        if (v instanceof TransitStop)
            LOG.warn(graph.addBuilderAnnotation(new StopUnlinked((TransitStop) v)));
        else if (v instanceof BikeRentalStationVertex)
            LOG.warn(graph.addBuilderAnnotation(new BikeRentalStationUnlinked((BikeRentalStationVertex) v)));
        else if (v instanceof BikeParkVertex)
            LOG.warn(graph.addBuilderAnnotation(new BikeParkUnlinked((BikeParkVertex) v)));
    }
}
