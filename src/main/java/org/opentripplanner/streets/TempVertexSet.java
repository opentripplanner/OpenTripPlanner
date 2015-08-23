package org.opentripplanner.streets;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntIntMap;
import org.opentripplanner.transit.TransitRouter;

import java.util.ArrayList;
import java.util.List;

/**
 * This is like a SampleSet and a StopTreeCache rolled into one. It is a column store of vertices. Note that we don't
 * ever need to traverse into a temporary vertex, only out of one, so temporary vertices do not interfere at all with
 * the permanent graph.
 *
 * A temp vertex set can be used for a big set of vertices, but also for single origin and target vertices in
 * point to point searches.
 *
 * Temp vertex sets can be persisted in the graph itself to save a worker the effort of loading street geometries and
 * re-linking stops.
 *
 * We can't use TempVertexSets to handle linking transit stops into the street network, because we want other
 * TempVertexSets to see the transit stop splitters as permanent and insert themselves between the intersection and the
 * transit stop splitter vertex.
 *
 * Searches always proceed outward from origins or targets (think StopTreeCache for the targets, which handles the last mile from transit to targets). We can have completely temporary vertices that are not even recorded in the main transport network store.
 * Keep in mind that if origins and destinations are pre-linked into the graph, the street geometries or even the whole street layer can be dropped entirely, and it’s still useful for routing. So the TransportNetwork must be serializable and usable with a null StreetLayer.
 *
 */
public class TempVertexSet {

    private final StreetLayer streetLayer;
    TIntList edges = new TIntArrayList(); // These are in fact the even (forward) edge IDs of closest edge pairs.
    TIntList fromDistancesMillimeters = new TIntArrayList();
    TIntList toDistancesMillimeters = new TIntArrayList();
    List<TIntIntMap> stopTrees = null;

    public TempVertexSet (StreetLayer streetLayer) {
        this.streetLayer = streetLayer;
    }

    public int getSize() {
        return edges.size();
    }

    public void addTempVertex (int fixedLat, int fixedLon) {
        // TODO find nearest edge pair, record distances to its endpoints
        // Reuse code from stop linker.
        Split split = streetLayer.findSplit(fixedLat, fixedLon, 300);
        edges.add(split.edge);
        fromDistancesMillimeters.add(split.lengthBefore_mm);
        toDistancesMillimeters.add(split.lengthAfter_mm);
    }

    public void buildStopTrees () {
        EdgeStore.Edge edge = streetLayer.edgeStore.getCursor();
        stopTrees = new ArrayList<>();
        for (int i = 0; i < edges.size(); i++) {
            StreetRouter streetRouter = new StreetRouter(streetLayer);
            edge.seek(edges.get(i));
            //streetRouter.addInitialVertex(edge.getFromVertex(), fromDistancesMillimeters.get(i));
            //streetRouter.addInitialVertex(edge.getToVertex(), toDistancesMillimeters.get(i));
            // streetRouter.setDestination(coords); goal direction is entirely geographical, just set a target location
            // streetRouter.route();
            // TODO run a search at each temp vertex, retaining the walk times to vertices.
        }
    }

    public void eval (StreetRouter streetRouter, TransitRouter transitRouter) {
        // Take times at transit stops and on street vertices, and find a min travel time for every point in this set.
    }
}
