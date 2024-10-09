package org.opentripplanner.astar.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import org.junit.jupiter.api.Test;

/*
 * Test correctness and relative speed of various
 * priority queue implementations.
 */
public class BinHeapTest {

  private static final int N = 50000;

  public void doQueue(BinHeap<Integer> q, List<Integer> input, List<Integer> expected) {
    List<Integer> result = new ArrayList<>(N);
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
    for (Integer i : input) q.insert(i, i);
    while (!q.empty()) sum += q.extract_min();
    // keep compiler from optimizing out extract
    assertEquals(sum, expectedSum);
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

  @Test
  public void testCompareHeaps() throws InterruptedException {
    List<Integer> input, expected;
    input = new ArrayList<>(N);
    for (int i = 0; i < N; i++) input.add((int) (Math.random() * 10000));

    // First determine the expected results using a plain old PriorityQueue
    expected = new ArrayList<>(N);
    PriorityQueue<Integer> q = new PriorityQueue<>(N);
    q.addAll(input);
    while (!q.isEmpty()) {
      expected.add(q.remove());
    }
    doQueue(new BinHeap<>(), input, expected);
    fillQueue(new BinHeap<>(), input);
  }

  /*
   * You must be careful to produce unique objects for rekeying,
   * otherwise the same object might be rekeyed twice or more.
   */
  @Test
  public void testRekey() throws InterruptedException {
    final int N = 5000;
    final int ITER = 2;

    List<Double> keys;
    List<Integer> vals;
    keys = new ArrayList<>(N);
    vals = new ArrayList<>(N);

    BinHeap<Integer> bh = new BinHeap<>(20);

    for (int iter = 0; iter < ITER; iter++) {
      // reuse internal array in binheap
      bh.reset();

      // fill both keys and values with random numbers
      for (int i = 0; i < N; i++) {
        keys.add(i, (Math.random() * 10000));
        vals.add(i, (N - i) * 3);
      }

      // insert them into the queue
      for (int i = 0; i < N; i++) {
        bh.insert(vals.get(i), keys.get(i));
      }

      // requeue every item with a new key that is an
      // order-preserving function of its place in the original list
      for (int i = 0; i < N; i++) {
        bh.rekey(vals.get(i), i * 2.0D + 10);
        // bh.dump();
      }

      // pull everything out of the queue in order
      // and check that the order matches the original list
      for (int i = 0; i < N; i++) {
        Double qp = bh.peek_min_key();
        Integer qi = bh.extract_min();
        assertEquals(qi, vals.get(i));
      }

      // the queue should be empty at the end of each iteration
      assertTrue(bh.empty());
    }
  }
}
