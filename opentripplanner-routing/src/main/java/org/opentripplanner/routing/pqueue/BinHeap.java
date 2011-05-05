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

package org.opentripplanner.routing.pqueue;

public class BinHeap<T> implements OTPPriorityQueue<T> {
    
    public static OTPPriorityQueueFactory FACTORY = new BinHeapFactory();
    
    private double[] prio;
    private T[] elem;
    private int size; 
    private int[] dir; // TODO: track element index by GenericVertex.index 
    
    @SuppressWarnings("unchecked")
    public BinHeap(int maxSize) {
        if (maxSize < 10) maxSize = 10; 
        elem = (T[]) new Object[maxSize + 1]; // erasure voodoo
        prio = new double[maxSize + 1];       // 1-based indexing
        size = 0;
        prio[0] = Double.NEGATIVE_INFINITY;   // set sentinel
    }
    
    @Override
    public int size() {return size;}
    
    @Override
    public boolean empty() {return size <= 0;}

    @Override
    public double peek_min_key() {return prio[1];} // user must check for empty
    
    @Override
    public T peek_min() {return elem[1];}
    
    @Override
    public void insert_or_dec_key(T e, double p) {insert(e, p);} // broken implementation

    public void reset() {size=0;} // empty the queue in one operation

    @Override
    public void insert(T e, double p) {
        int i;
        size += 1;
        for (i = size; prio[i/2] > p; i /= 2) {
            elem[i] = elem[i/2];
            prio[i] = prio[i/2];
        }
        elem[i] = e;
        prio[i] = p;
    }    
    
    @Override
    public T extract_min() {
        int    i, child;
        T      minElem  = elem[1];
        T      lastElem = elem[size];
        double lastPrio = prio[size];
        size -= 1;
        for (i=1; i*2 <= size; i=child) {
            child = i*2;
            if (child != size && prio[child+1] < prio[child])
                child++;
            if (lastPrio > prio[child]) {
                elem[i] = elem[child];
                prio[i] = prio[child];
            } else break;
        }
        elem[i] = lastElem;
        prio[i] = lastPrio;
        return minElem;
    }    
    
    private static class BinHeapFactory implements OTPPriorityQueueFactory {

        @Override
        public <T> OTPPriorityQueue<T> create(int maxSize) {
            return new BinHeap<T>(maxSize);
        }
    }
}
