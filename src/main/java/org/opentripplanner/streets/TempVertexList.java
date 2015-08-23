package org.opentripplanner.streets;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import org.opentripplanner.transit.TransitRouter;

/**
 * This is like a SampleSet.
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
public class TempVertexList {

    private final StreetLayer streetLayer;
    TIntList edges; // These are in fact the even (forward) edge IDs of closest edge pairs.
    TIntList fromDistancesMillimeters = new TIntArrayList();
    TIntList toDistancesMillimeters = new TIntArrayList();

    public TempVertexList(StreetLayer streetLayer) {
        this.streetLayer = streetLayer;
    }

    public int getSize() {
        return edges.size();
    }

    public void addTempVertex (double lat, double lon) {
        int fixedLat = VertexStore.floatingDegreesToFixed(lat);
        int fixedLon = VertexStore.floatingDegreesToFixed(lon);
        Split split = streetLayer.findSplit(fixedLat, fixedLon, 300);
        edges.add(split.edge);
        fromDistancesMillimeters.add(split.lengthBefore_mm);
        toDistancesMillimeters.add(split.lengthAfter_mm);
    }

    /**
     * Given a StreetRouter and a TransitRouter that have already run searches and contain results, determine the
     * travel time to every temporary vertex in this set.
     * TODO: Departure times and walking speeds should be taken from those routers.
     * @param streetRouter containing results for a non-transit search from the origin.
     * @param transitRouter containing corresponding results for a transit search following the street search.
     * @return a list of travel times to each temporary vertex (in order).
     */
    public TIntList eval (StreetRouter streetRouter, TransitRouter transitRouter) {
        TIntList travelTimes = new TIntArrayList(edges.size());
        // Iterate over all locations in this temporary vertex list.
        EdgeStore.Edge edge = streetLayer.edgeStore.getCursor();
        for (int i = 0; i < edges.size(); i++) {
            if (edges.get(i) < 0) {
                travelTimes.add(Integer.MAX_VALUE);
                continue;
            }
            edge.seek(edges.get(i));
            int elapsedFromTime = Integer.MAX_VALUE;
            int elapsedToTime = Integer.MAX_VALUE;
            int fromVertex = edge.getFromVertex();
            int toVertex = edge.getToVertex();
            // FIXME Check both walking time and transit time here, finding the better one.
            // TODO apply walk speed
            elapsedFromTime += fromDistancesMillimeters.get(i) / 1000;
            elapsedToTime += toDistancesMillimeters.get(i) / 1000;
            travelTimes.add(elapsedFromTime < elapsedToTime ? elapsedFromTime : elapsedToTime);
        }
        return travelTimes;
    }

}
