package org.opentripplanner.streets;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntIntMap;

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
 */
public class TempVertexSet {

    private final StreetLayer streetLayer;
    TIntList edgePairs = new TIntArrayList();
    TIntList firstDistancesMm = new TIntArrayList();
    TIntList secondDistancesMm = new TIntArrayList();
    List<TIntIntMap> stopTrees = null;

    public TempVertexSet (StreetLayer streetLayer) {
        this.streetLayer = streetLayer;
    }

    public int getSize() {
        return edgePairs.size();
    }

    public void addTempVertex (int fixedLat, int fixedLon) {
        // TODO find nearest edge pair, record distances to its endpoints
    }

    public void buildStopTrees () {
        stopTrees = new ArrayList<>();
        for (int i = 0; i < edgePairs.size(); i++) {
            // TODO run a search at each temp vertex, retaining the walk times to vertices.
        }
    }
}
