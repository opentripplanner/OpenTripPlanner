package org.opentripplanner.streets;

import org.opentripplanner.analyst.PointSet;

/**
 * A LinkedPointSet is a PointSet that has been pre-connected to a StreetLayer in a non-destructive, reversible way.
 *
 * This is a replacement for SampleSet.
 * It is a column store of locations that have not been permanently spliced into the graph.
 * We cannot just adapt the SampleSet constructor because it holds on to OTP Vertex objects. In the new system we have
 * only vertex indexes. Well, there are Vertex cursor objects but we're not going to save those.
 * Likewise for TimeSurfaces which are intersected with SampleSets. They are keyed on Vertex references so need to be replaced.
 *
 * Semi-obsolete notes:
 * Note that we don't ever need to traverse into a temporary vertex, only out of one, so temporary vertices do not
 * interfere at all with the permanent graph.
 * A temp vertex set can be used for a big set of vertices, but also for single origin and target vertices in
 * point to point searches.
 * Temp vertex sets can be persisted in the graph itself to save a worker the effort of loading street geometries and
 * re-linking stops.
 *
 * We can't use TempVertexSets to handle linking transit stops into the street network, because we want other
 * TempVertexSets to see the transit stop splitters as permanent and insert themselves between the intersection and the
 * transit stop splitter vertex.
 *
 * Searches always proceed outward from origins or targets (think StopTreeCache for the targets, which handles the last
 * mile from transit to targets). We can have completely temporary vertices that are not even recorded in the main
 * transport network store.
 * Keep in mind that if origins and destinations are pre-linked into the graph, the street geometries or even the
 * whole street layer can be dropped entirely, and it’s still useful for routing. So the TransportNetwork must be
 * serializable and usable with a null StreetLayer.
 *
 */
public class LinkedPointSet {

    /**
     * LinkedPointSets are long-lived and not extremely numerous, so we keep references to the objects it was built from.
     * Besides these fields are useful for later processing of LinkedPointSets.
     */
    public final PointSet pointSet;

    /**
     * We need to retain the street layer so we can look up edge endpoint vertices for the edge IDs.
     * Besides, this object is inextricably linked to one street network.
     */
    public final StreetLayer streetLayer;

    /**
     * For each point, the closest edge in the street layer.
     * This is in fact the even (forward) edge ID of the closest edge pairs.
     */
    private int[] edges;

    /** For each point, distance from the initial vertex of the edge to the split point. */
    private int[] distances0_mm;

    /** For each point, distance from the final vertex of the edge to the split point. */
    private int[] distances1_mm;

    /**
     * A LinkedPointSet is a PointSet that has been pre-connected to a StreetLayer in a non-destructive, reversible way.
     * These objects are long-lived and not extremely numerous, so we keep references to the objects it was built from.
     * Besides they are useful for later processing of LinkedPointSets.
     */
    public LinkedPointSet(PointSet pointSet, StreetLayer streetLayer) {
        this.pointSet = pointSet;
        this.streetLayer = streetLayer;
        edges = new int[pointSet.capacity];
        distances0_mm = new int[pointSet.capacity];
        distances1_mm = new int[pointSet.capacity];
        for (int i = 0; i < pointSet.capacity; i++) {
            Split split = streetLayer.findSplit(pointSet.getLat(i), pointSet.getLon(i), 300);
            if (split == null) {
                edges[i] = -1;
            } else {
                edges[i] = split.edge;
                distances0_mm[i] = split.distance0_mm;
                distances1_mm[i] = split.distance1_mm;
            }
        }
    }

    public int size () {
        return edges.length;
    }

    /**
     * A functional interface for fetching the travel time to any street vertex in the transport network.
     * Note that TIntIntMap::get matches this functional interface.
     * There may be a generic IntToIntFunction library interface somewhere, but this interface provides type information
     * about what the function and its parameters mean.
     */
    @FunctionalInterface
    public static interface TravelTimeFunction {
        /**
         * @param vertexId the index of a vertex in the StreetLayer of a TransitNetwork.
         * @return the travel time to the given street vertex, or Integer.MAX_VALUE if the vertex is unreachable.
         */
        public int getTravelTime (int vertexId);
    }

    /**
     * Determine the travel time to every temporary vertex in this set.
     * The parameter is a function from street vertex indexes to elapsed travel times.
     *
     * TODO: Departure times and walking speeds should be supplied.
     * @return a list of travel times to each point in the PointSet. Integer.MAX_VALUE means a vertex was unreachable.
     */
    public PointSetTimes eval (TravelTimeFunction travelTimeForVertex) {
        int[] travelTimes = new int[edges.length];
        // Iterate over all locations in this temporary vertex list.
        EdgeStore.Edge edge = streetLayer.edgeStore.getCursor();
        for (int i = 0; i < edges.length; i++) {
            if (edges[i] < 0) {
                travelTimes[i] = Integer.MAX_VALUE;
                continue;
            }
            edge.seek(edges[i]);
            int time0 = travelTimeForVertex.getTravelTime(edge.getFromVertex());
            int time1 = travelTimeForVertex.getTravelTime(edge.getToVertex());
            // TODO apply walk speed
            if (time0 != Integer.MAX_VALUE) {
                time0 += distances0_mm[i] / 1000;
            }
            if (time1 != Integer.MAX_VALUE) {
                time1 += distances1_mm[i] / 1000;
            }
            travelTimes[i] = time0 < time1 ? time0 : time1;
        }
        return new PointSetTimes (pointSet, travelTimes);
    }

}
