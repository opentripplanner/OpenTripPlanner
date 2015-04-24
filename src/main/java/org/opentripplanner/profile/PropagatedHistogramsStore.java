package org.opentripplanner.profile;

import gnu.trove.iterator.TIntIterator;
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
        return getHistogram(vertex.getIndex());
    }

    /** Return the histogram for a given vertexIndex as an array of ints. */
    public TIntList getHistogram (int vertexIndex) {
        int index = vertexIndex * dy;
        return TIntArrayList.wrap(values).subList(index, index + dy);
    }

    /**
     * Given a list of minimum travel times to all street vertices at a single departure time,
     * merge them into the per-street-vertex histograms for the whole time window.
     */
    public void mergeIn (int[] timesForVertices) {
        for (int v = 0; v < timesForVertices.length; v++) {
            updateHistogram(v, timesForVertices[v]);
        }
    }

    public void dump() {
        char[] box = new char[] {' ', 'o', 'O', 'X'};
        for (int x = 0; x < dx; x++) {
            int max = 0;
            TIntList hist = getHistogram(x);
            TIntIterator iter = hist.iterator();
            while (iter.hasNext()) {
                int val = iter.next();
                if (val > max) {
                    max = val;
                }
            }
            iter = hist.iterator();
            while (iter.hasNext()) {
                double val = iter.next();
                val /= max;
                val *= 4.0;
                if (val > 3) val = 3;
                char c = box[(int)val];
                System.out.write(c);
            }
            System.out.write('\n');
        }
    }

}
