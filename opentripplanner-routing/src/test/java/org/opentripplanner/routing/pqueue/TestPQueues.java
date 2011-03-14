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
    private static final int N = 50000;
    private static final int ITER = 3;
    
    public void doQueue(DirectoryPriorityQueue<Integer> q, 
                        List<Integer> input, List<Integer> expected) {
        List<Integer> result = new ArrayList<Integer>(N);
        long t0 = System.currentTimeMillis();
        for (Integer i : input) q.insert(i, i * 0.5);
        while (!q.empty()) result.add(q.extract_min());
        long t1 = System.currentTimeMillis();
        assertEquals(result, expected);
        System.out.println(q.getClass() + " time " + (t1-t0)/1000.0 + " sec");
    }
    
    public void testCompareHeaps() throws InterruptedException {
        List<Integer> input, expected;
        input = new ArrayList<Integer>(N);
        for (int i=0; i<N; i++) input.add((int) (Math.random() * 10000));

        for (int i=0; i<ITER; i++) {
            System.out.println("Iteration " + i + " insert/extract " + N);
            expected = new ArrayList<Integer>(N);
            PriorityQueue<Integer> q = new PriorityQueue<Integer>(N);
            long t0 = System.currentTimeMillis();
            for (Integer j : input) q.add(j);
            while (!q.isEmpty()) expected.add(q.remove());
            long t1 = System.currentTimeMillis();
            System.out.println(q.getClass() + " time " + (t1-t0)/1000.0 + " sec");
            
            doQueue(new FibHeap<Integer>(N), input, expected);
            doQueue(new BinHeap<Integer>(N), input, expected);            
            doQueue(new TLHeap<Integer>(N),  input, expected);            
        }
    }    
}
