/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.common.pqueue;

/*
 * Relatively simple priority queue structure with worse asymptotic complexity 
 * than a Fibonacci heap, but better running time in most real use cases.
 * 
 * Based on the article:
 * 
 * K. Subramani1 and Kamesh Madduri (2009)
 * Two-Level Heaps: A New Priority Queue Structure 
 * with Applications to the Single Source Shortest Path Problem
 * 
 */

public class TLHeap<T> implements OTPPriorityQueue<T> {
    
    public static OTPPriorityQueueFactory FACTORY = new TLHeapFactory();
    
    private static final double loge2 = Math.log(2);
    private int nSubheaps;
    private int subheapSize;
    private BinHeap<T>[] subheaps;    
    private int maxSize;
    private int size;
    private int curHeap;

    // base 2 logarithm
    private static double log (double x) { return Math.log(x)/loge2; }
        
    // function for finding optimal number of subheaps, see article
    private static double optimize(int n, int m) {
        double k = log((m * log(log(n))) / (2*n*Math.log(log(n)))) / log(log(n));
        return Math.pow(log(n), k); // optimal number of subheaps
    }

    @SuppressWarnings("unchecked")
    public TLHeap (int capacity) {
        nSubheaps = (int) optimize(10, 100);
        //System.out.println("TLHeap number of subheaps: " + nSubheaps);
        if (capacity < nSubheaps) capacity = nSubheaps;
        maxSize = capacity;
        size = 0;
        subheapSize = (maxSize / nSubheaps) + 1;
        subheaps = (BinHeap<T>[]) (new BinHeap[nSubheaps]); // erasure casting voodoo
        for (int i=0; i<nSubheaps; i++) 
            subheaps[i] = new BinHeap<T>(subheapSize);
        curHeap = 0;
    }
    
    @Override
    public T extract_min() {
        double bestPrio = Double.POSITIVE_INFINITY;
        BinHeap<T> bestHeap = null;
        for (int i=0; i<nSubheaps; i++) {
            BinHeap<T> h = subheaps[i];
            if (!h.empty()) {
                double p = h.peek_min_key();
                if (p < bestPrio) {
                    bestPrio = p;
                    bestHeap = h;
                }
            }
        }
        size -= 1;
        if (bestHeap == null)
            return null;
        else
            return bestHeap.extract_min();
    }

    @Override
    public void insert(T e, double p) {
        while (subheaps[curHeap].size() >= subheapSize) {
            curHeap += 1;
            if (curHeap >= nSubheaps) curHeap = 0;
        }
        subheaps[curHeap].insert(e, p);
        size += 1;
    }

    @Override
    public void insert_or_dec_key(T e, double p) { insert(e, p); } // broken implementation

    @Override
    public int size() { return size; }

    @Override
    public boolean empty() { return size <= 0; }
    
    @Override
    public double peek_min_key() {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public T peek_min() {
        return null;
    }

    private static class TLHeapFactory implements OTPPriorityQueueFactory {
        @Override
        public <T> OTPPriorityQueue<T> create(int maxSize) {
            return new TLHeap<T>(maxSize);
        }
    }
}
