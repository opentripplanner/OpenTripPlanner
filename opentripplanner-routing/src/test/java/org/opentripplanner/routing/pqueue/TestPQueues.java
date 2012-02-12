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
    private static final int N = 200000;
    private static final int ITER = 2;
    private static final int INNER_ITER = 10;
    
    public void doQueue(OTPPriorityQueue<Integer> q, 
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
        long t0 = System.currentTimeMillis();
        for (int iter = 0; iter < INNER_ITER; iter++) {
            int sum = 0;        
            for (Integer i : input) 
                q.insert(i, i);
            while (!q.empty()) 
                sum += q.extract_min();
            // keep compiler from optimizing out extract
            assertTrue(sum == expectedSum);
        }
        long t1 = System.currentTimeMillis();
        System.out.println(q.getClass() + " \ttime " + (t1-t0)/1000.0 + " sec");
    }
    
    public void fillQueue(OTPPriorityQueue<Integer> q, List<Integer> input) {
        long t0 = System.currentTimeMillis();
        for (Integer i : input) {
            q.insert(i, i * 0.5);
        }
        int sum = 0;
        for (int i=0; i<INNER_ITER; i++) {
            for (Integer j : input) {
                sum += q.extract_min();
                q.insert(j, j * 0.5);
            }
        }
        while (!q.empty()) {
            sum += q.extract_min();
        }
        // keep compiler from optimizing out extract
        assertTrue(sum != 0);

        long t1 = System.currentTimeMillis();
        System.out.println(q.getClass() + " time " + (t1 - t0) / 1000.0 + " sec");
    }

    public void testIntBinHeap() {
        IntBinHeap binHeap = new IntBinHeap(100);
        binHeap.insert(0, 123);
        assertEquals(123, binHeap.peek_min_key(), 1e-4);
        assertEquals(1, binHeap.size());

        binHeap.rekey(0, 12);
        assertEquals(12, binHeap.peek_min_key(), 1e-4);
        assertEquals(1, binHeap.size());

        // element 8 is not present
        binHeap.rekey(8, 8);
        assertEquals(12, binHeap.peek_min_key(), 1e-4);
        assertEquals(1, binHeap.size());
    }

    private List<OTPPriorityQueue<Integer>> makeQueues() {
        List<OTPPriorityQueue<Integer>> queues = new ArrayList<OTPPriorityQueue<Integer>>();
        queues.add(new PriorityQueueImpl<Integer>());
        // commented out to avoid slow testing
        // queues.add(new FibHeap<Integer>(N));
        queues.add(new BinHeap<Integer>(N));            
        queues.add(new IntBinHeap(N));
        queues.add(new BinHeap<Integer>(10));
        queues.add(new IntBinHeap(10));
        return queues;
    }

    public void testCompareHeaps() throws InterruptedException {
        List<Integer> input, expected;
        input = new ArrayList<Integer>(N);
        for (int i=0; i<N; i++) input.add((int) (Math.random() * 10000));
        
        System.out.println("\ninsert/extract " + N + " Integers " + INNER_ITER + " times");
        expected = new ArrayList<Integer>(N);
        PriorityQueue<Integer> q = new PriorityQueue<Integer>(N);
        for (Integer j : input) 
            q.add(j);
        while (!q.isEmpty()) 
            expected.add(q.remove());
        System.out.println(q.getClass() + " (expected results)");
        for (int i=0; i<ITER; i++) {
            for (OTPPriorityQueue<Integer> queue : makeQueues()) {
                doQueue(queue, input, expected);
            }            
        }
        System.out.println("\nmaintain queue at size " + N);            
        for (OTPPriorityQueue<Integer> queue : makeQueues()) {
            fillQueue(queue, input);
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
