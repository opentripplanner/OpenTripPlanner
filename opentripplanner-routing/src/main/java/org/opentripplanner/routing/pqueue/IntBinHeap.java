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

import java.util.Arrays;

public class IntBinHeap implements OTPPriorityQueue<Integer> {

    private static final double GROW_FACTOR = 2.0;
    
    private double[] prio;
    private int[] elem;
    private int   size; 
    private int capacity;
    
    public IntBinHeap() {
    	this(1000);
    }
    
    public IntBinHeap(int capacity) {
        if (capacity < 10) capacity = 10;
    	this.capacity = capacity;
    	size = 0;
        elem = new int[capacity + 1];
        prio = new double[capacity + 1];    // 1-based indexing
        prio[0] = Double.NEGATIVE_INFINITY; // set sentinel
    }

    public int size() {
    	return size;
    }
    
    public boolean empty() {
    	return size <= 0;
    }

    public double peek_min_key() {
    	if (size > 0) 
    		return prio[1];
    	else 
    		throw new IllegalStateException("An empty queue does not have a minimum key.");
   }
    
    public int p_peek_min() {
    	if (size > 0)
    		return elem[1];
    	else 
    	    throw new IllegalStateException("An empty queue does not have a minimum value.");
    }
    
    public Integer peek_min() {
        if (size > 0)
            return p_peek_min(); 
        else
            return null;
    }
    
    public void rekey(int e, double p) {
        // Perform "inefficient" but straightforward linear search 
    	// for an element then change its key by sifting up or down
        int i;
    	for (i=1; i <= size; i++) {
    		if (elem[i] == e)
    		    break;
    	}
    	if (i > size) {
        	//System.out.printf("did not find element %s\n", e);
    		return; 
    	}
    	//System.out.printf("found element %s with key %f at %d\n", e, prio[i], i);
    	if (p > prio[i]) { 
    		// sift up (as in extract)
            while (i*2 <= size) {
                int child = i * 2;
                if (child != size && prio[child+1] < prio[child])
                    child ++;
                if (p > prio[child]) {
                    elem[i] = elem[child];
                    prio[i] = prio[child];
                    i = child;
                } else break;
            }
            elem[i] = e;
            prio[i] = p;
    	} else { 
    		// sift down (as in insert)
            while (prio[i/2] > p) {
                elem[i] = elem[i/2];
                prio[i] = prio[i/2];
                i /= 2;
            }
            elem[i] = e;
            prio[i] = p;
    	}
    }

    public void dump() {
    	for (int i=0; i<=capacity; i++) {
    		String topMarker = (i > size) ? "(UNUSED)" : ""; 
        	System.out.printf("%d\t%f\t%s\t%s\n", i, prio[i], elem[i], topMarker);
    	}
    	System.out.printf("-----------------------\n");
    }
    
    public void reset() {
    	// empties the queue in one operation
    	size=0;
    } 

    public void insert(int e, double p) {
        int i;
        size += 1;
        if (size > capacity) 
        	resize((int) (capacity * GROW_FACTOR));
        for (i = size; prio[i/2] > p; i /= 2) {
            elem[i] = elem[i/2];
            prio[i] = prio[i/2];
        }
        elem[i] = e;
        prio[i] = p;
    }    
    
    @Override
    public void insert(Integer payload, double key) {
        insert(payload.intValue(), key);
    }
    
    public int p_extract_min() {
        int    i, child;
        int    minElem  = elem[1];
        int    lastElem = elem[size];
        double lastPrio = prio[size];
        if (size <= 0) 
            throw new IllegalStateException("An empty queue does not have a minimum value.");
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
    
    @Override
    public Integer extract_min() {
        if (size <= 0) 
            return null;
        else
            return p_extract_min();
    }
    
    public void resize(int capacity) {
    	// System.out.println("Growing queue to " + capacity);
    	if (capacity < size) 
    		throw new IllegalStateException("BinHeap contains too many elements to fit in new capacity.");
    	this.capacity = capacity;
    	prio = Arrays.copyOf(prio, capacity + 1);
    	elem = Arrays.copyOf(elem, capacity + 1);
    }

    @Override
    public void insert_or_dec_key(Integer payload, double key) {
        throw new UnsupportedOperationException();
    }

}
