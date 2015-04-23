package org.opentripplanner.profile;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import org.opentripplanner.routing.graph.Vertex;

public class PropagatedHistogramsStore extends Contiguous2DIntArray {

    int nBins;

    public PropagatedHistogramsStore (int nBins) {
        super(Vertex.getMaxIndex(), nBins);
        this.nBins = nBins;
    }

    /** Record a travel time observation at a vertex. Increments the histogram bin for the given travel time. */
    private void updateHistogram (int vertexIndex, int travelTimeSeconds) {
        int bin = travelTimeSeconds / 60;
        if (bin < nBins) {
            adjust(vertexIndex, bin, 1);
        }
    }

    /** Return the histogram for a given vertex as an array of ints. */
    public TIntList getHistogram (Vertex vertex) {
        int index = vertex.getIndex() * dy;
        return TIntArrayList.wrap(values).subList(index, index + dy);
    }

    /**
     * Given a list of minimum travel times to all street vertices at a single departure time,
     * merge them into the per-street-vertex histograms for the whole time window.
     */
    public void mergeIn (int[] timesForVertices) {
        for (int v = 0; v < timesForVertices.length; v++) {
            updateHistogram (v, timesForVertices[v]);
        }
    }

}
