package org.opentripplanner.graph_builder.linking;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.linearref.LinearLocation;
import org.opentripplanner.common.geometry.HashGridSpatialIndex;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.SplitterVertex;
import org.opentripplanner.routing.vertextype.TemporarySplitterVertex;

/**
 * Splits street edges temporarily or permanently at the given fraction
 */
public class StreetSplitter {

    private final Graph graph;

    private final HashGridSpatialIndex<Edge> idx;

    public StreetSplitter(Graph graph, HashGridSpatialIndex<Edge> idx) {
        this.graph = graph;
        this.idx = idx;
    }

    /**
     * Split the street edge at the given fraction
     *
     * @param edge      to be split
     * @param ll        fraction at which to split the edge
     * @param endVertex this is true if this is end vertex
     * @return Splitter vertex with added new edges
     */
    public SplitterVertex splitTemporarily(StreetEdge edge, LinearLocation ll, boolean endVertex) {
        Coordinate splitPoint = getSplitPoint(edge, ll);

        // every edge can be split exactly once, so this is a valid label
        SplitterVertex v = new TemporarySplitterVertex("split from " + edge.getId(), splitPoint.x, splitPoint.y, edge, endVertex);

        // Split the 'edge' at 'v' in 2 new edges and connect these 2 edges to the
        // existing vertices
        edge.split(v, false);
        return v;
    }

    /**
     * Split the street edge at the given fraction
     *
     * @param edge to be split
     * @param ll   fraction at which to split the edge
     * @return Splitter vertex with added new edges
     */
    public SplitterVertex splitPermanently(StreetEdge edge, LinearLocation ll) {
        Coordinate splitPoint = getSplitPoint(edge, ll);

        // every edge can be split exactly once, so this is a valid label
        SplitterVertex v = new SplitterVertex(graph, "split from " + edge.getId(), splitPoint.x, splitPoint.y, edge);

        // Split the 'edge' at 'v' in 2 new edges and connect these 2 edges to the
        // existing vertices
        P2<StreetEdge> edges = edge.split(v, true);

        // update indices of new edges
        // (no need to remove original edge from index, we filter it when it comes out of the index)
        idx.insert(edges.first.getGeometry(), edges.first);
        idx.insert(edges.second.getGeometry(), edges.second);

        // remove original edge from the graph
        edge.getToVertex().removeIncoming(edge);
        edge.getFromVertex().removeOutgoing(edge);

        return v;
    }

    private Coordinate getSplitPoint(StreetEdge edge, LinearLocation ll) {
        return ll.getCoordinate(edge.getGeometry());
    }
}
