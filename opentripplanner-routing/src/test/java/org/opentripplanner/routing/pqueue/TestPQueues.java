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

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import junit.framework.TestCase;

/*
 * Test correctness and relative speed of various
 * priority queue implementations.
 */
public class TestPQueues extends TestCase { 
    private static final int N = 100000;
    private static final int ITER = 3;
    
    public void doQueue(OTPPriorityQueue<Integer> q, 
                        List<Integer> input, List<Integer> expected) {
        List<Integer> result = new ArrayList<Integer>(N);
        long t0 = System.currentTimeMillis();
        for (Integer i : input) 
            q.insert(i, i * 0.5);
        while (!q.empty()) 
            result.add(q.extract_min());
        long t1 = System.currentTimeMillis();
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
        System.out.println(q.getClass() + " time " + (t1-t0)/1000.0 + " sec");
    }
    
    public void testCompareHeaps() throws InterruptedException {
        List<Integer> input, expected;
        input = new ArrayList<Integer>(N);
        for (int i=0; i<N; i++) input.add((int) (Math.random() * 10000));

        for (int i=0; i<ITER; i++) {
            System.out.println("\nIteration " + i + " insert/extract " + N);
            expected = new ArrayList<Integer>(N);
            PriorityQueue<Integer> q = new PriorityQueue<Integer>(N);
            long t0 = System.currentTimeMillis();
            for (Integer j : input) 
                q.add(j);
            while (!q.isEmpty()) 
                expected.add(q.remove());
            long t1 = System.currentTimeMillis();
            System.out.println(q.getClass() + " time " + (t1-t0)/1000.0 + " sec");
            
            doQueue(new PriorityQueueImpl<Integer>(),  input, expected);
            doQueue(new FibHeap<Integer>(N), input, expected);
            doQueue(new BinHeap<Integer>(N), input, expected);            
            System.out.println("BinHeap initial capacity set to 10 (force grow)");
            doQueue(new BinHeap<Integer>(10), input, expected);            
        }
    }    

    /*
     * You must be careful to produce unique objects for rekeying,
     * otherwise the same object might be rekeyed twice or more.
     */
    public void testRekey() throws InterruptedException {
    	final int N = 50000;
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
		    	//bh.dump();
		    }        	

		    // requeue every item with a new key that is an 
		    // order-preserving function of its place in the original list
		    System.out.printf("\nRekey %d elements\n", N);
		    long t0 = System.currentTimeMillis();
		    for (int i=0; i<N; i++) {
		    	bh.rekey(vals.get(i), i * 2.0D + 10);
		    	// bh.dump();
		    }        	
            long t1 = System.currentTimeMillis();
            double tSec = (t1-t0)/1000.0;
            System.out.printf("time %f sec\n", tSec);
            System.out.printf("time per rekey %f\n", tSec / N);

		    // pull everything out of the queue in order
		    // and check that the order matches the original list
		    System.out.printf("Comparing to expected results\n");
		    for (int i=0; i<N; i++) {
		    	Double  qp = bh.peek_min_key();
		    	Integer qi = bh.extract_min();
		    	//System.out.printf("%3d : queue key %f queue val %d expected %d\n", i, qp, qi, vals.get(i));
		    	assertEquals(qi, vals.get(i));
		    }
		    
		    // the queue should be empty at the end of each iteration
		    assertTrue(bh.empty());

        }
    }    
}
