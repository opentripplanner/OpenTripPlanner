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

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import junit.framework.TestCase;

/*
 * Test correctness and relative speed of various
 * priority queue implementations.
 */
public class TestPQueues extends TestCase { 
    private static final int N = 50000;

    public void doQueue(BinHeap<Integer> q,
                        List<Integer> input, List<Integer> expected) {
        List<Integer> result = new ArrayList<Integer>(N);
        int expectedSum = 0;
        for (Integer i : input) {
            q.insert(i, i * 0.5);
            expectedSum += i;
        }
        while (!q.empty()) {
            result.add(q.extract_min());
        }
        assertEquals(result, expected);
        // check behavior when queue is empty
        assertEquals(q.size(), 0);
        assertNull(q.peek_min());
        assertNull(q.extract_min());
        q.insert(100, 10);
        q.insert(200, 20);
        assertEquals(q.size(), 2);
        assertNotNull(q.extract_min());
        assertNotNull(q.extract_min());
        assertNull(q.extract_min());
        assertEquals(q.size(), 0);
        // fill and empty the queue a few times
        int sum = 0;
        for (Integer i : input)
            q.insert(i, i);
        while (!q.empty())
            sum += q.extract_min();
        // keep compiler from optimizing out extract
        assertTrue(sum == expectedSum);
    }
    
    public void fillQueue(BinHeap<Integer> q, List<Integer> input) {
        for (Integer i : input) {
            q.insert(i, i * 0.5);
        }
        int sum = 0;
        for (Integer j : input) {
            sum += q.extract_min();
            q.insert(j, j * 0.5);
        }
        while (!q.empty()) {
            sum += q.extract_min();
        }
        // keep compiler from optimizing out extract
        assertTrue(sum != 0);
    }

    public void testCompareHeaps() throws InterruptedException {
        List<Integer> input, expected;
        input = new ArrayList<Integer>(N);
        for (int i=0; i<N; i++) input.add((int) (Math.random() * 10000));
        
        // First determine the expected results using a plain old PriorityQueue
        expected = new ArrayList<Integer>(N);
        PriorityQueue<Integer> q = new PriorityQueue<Integer>(N);
        for (Integer j : input) {
            q.add(j);
        }
        while (!q.isEmpty()) {
            expected.add(q.remove());
        }
        doQueue(new BinHeap<Integer>(), input, expected);
        fillQueue(new BinHeap<Integer>(), input);
    }

    /*
     * You must be careful to produce unique objects for rekeying,
     * otherwise the same object might be rekeyed twice or more.
     */
    public void testRekey() throws InterruptedException {
    	final int N = 5000;
    	final int ITER = 2;

    	List<Double>  keys;
        List<Integer> vals;
        keys = new ArrayList<Double>(N);
        vals = new ArrayList<Integer>(N);
        
        BinHeap<Integer> bh = new BinHeap<Integer>(20);

        for (int iter = 0; iter < ITER; iter++) {

        	// reuse internal array in binheap
        	bh.reset();

        	// fill both keys and values with random numbers
		    for (int i=0; i<N; i++) {
		    	keys.add(i, (Math.random() * 10000));
		    	vals.add(i, (N - i) * 3);
		    }        	
		    
		    // insert them into the queue
		    for (int i=0; i<N; i++) {
		    	bh.insert(vals.get(i), keys.get(i));
		    }

		    // requeue every item with a new key that is an 
		    // order-preserving function of its place in the original list
		    for (int i=0; i<N; i++) {
		    	bh.rekey(vals.get(i), i * 2.0D + 10);
		    	// bh.dump();
		    }        	

		    // pull everything out of the queue in order
		    // and check that the order matches the original list
		    for (int i=0; i<N; i++) {
		    	Double  qp = bh.peek_min_key();
		    	Integer qi = bh.extract_min();
		    	assertEquals(qi, vals.get(i));
		    }
		    
		    // the queue should be empty at the end of each iteration
		    assertTrue(bh.empty());

        }
    }    
}
