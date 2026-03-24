package org.opentripplanner.astar.model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class BinHeap<T> {

  private static final double GROW_FACTOR = 2.0;

  private double[] prio;
  private T[] elem;
  private int size;
  private int capacity;
  private final Map<T, Integer> positionMap;

  public BinHeap() {
    this(1000);
  }

  @SuppressWarnings("unchecked")
  public BinHeap(int capacity) {
    if (capacity < 10) {
      capacity = 10;
    }
    this.capacity = capacity;
    // erasure voodoo
    elem = (T[]) new Object[capacity + 1];
    // 1-based indexing
    prio = new double[capacity + 1];
    size = 0;
    // set sentinel
    prio[0] = Double.NEGATIVE_INFINITY;
    positionMap = new HashMap<>((int) (capacity * 0.75));
  }

  public int size() {
    return size;
  }

  public boolean empty() {
    return size == 0;
  }

  public double peek_min_key() {
    if (size > 0) {
      return prio[1];
    } else {
      throw new IllegalStateException("An empty queue does not have a minimum key.");
    }
  }

  public T peek_min() {
    if (size > 0) {
      return elem[1];
    } else {
      return null;
    }
  }

  public void rekey(T e, double p) {
    // O(1) lookup using position map instead of O(n) linear search
    Integer position = positionMap.get(e);
    if (position == null) {
      return;
    }
    int i = position;
    if (p > prio[i]) {
      // sift up (as in extract)
      while (i * 2 <= size) {
        int child = i * 2;
        if (child != size && prio[child + 1] < prio[child]) {
          child++;
        }
        if (p > prio[child]) {
          elem[i] = elem[child];
          prio[i] = prio[child];
          positionMap.put(elem[i], i);
          i = child;
        } else {
          break;
        }
      }
    } else {
      // sift down (as in insert)
      while (prio[i / 2] > p) {
        elem[i] = elem[i / 2];
        prio[i] = prio[i / 2];
        positionMap.put(elem[i], i);
        i /= 2;
      }
    }
    elem[i] = e;
    prio[i] = p;
    positionMap.put(e, i);
  }

  public void reset() {
    // empties the queue in one operation
    size = 0;
    positionMap.clear();
  }

  public void insert(T e, double p) {
    int i;
    size += 1;
    if (size > capacity) {
      resize((int) (capacity * GROW_FACTOR));
    }
    for (i = size; prio[i / 2] > p; i /= 2) {
      elem[i] = elem[i / 2];
      prio[i] = prio[i / 2];
      positionMap.put(elem[i], i);
    }
    elem[i] = e;
    prio[i] = p;
    positionMap.put(e, i);
  }

  public T extract_min() {
    if (size == 0) {
      return null;
    }
    int i, child;
    T minElem = elem[1];
    T lastElem = elem[size];
    double lastPrio = prio[size];
    positionMap.remove(minElem);
    size -= 1;
    for (i = 1; i * 2 <= size; i = child) {
      child = i * 2;
      if (child != size && prio[child + 1] < prio[child]) {
        child++;
      }
      if (lastPrio > prio[child]) {
        elem[i] = elem[child];
        prio[i] = prio[child];
        positionMap.put(elem[i], i);
      } else {
        break;
      }
    }
    if (size > 0) {
      elem[i] = lastElem;
      prio[i] = lastPrio;
      positionMap.put(lastElem, i);
    }
    return minElem;
  }

  public void resize(int capacity) {
    // System.out.println("Growing queue to " + capacity);
    if (capacity < size) {
      throw new IllegalStateException("BinHeap contains too many elements to fit in new capacity.");
    }
    this.capacity = capacity;
    prio = Arrays.copyOf(prio, capacity + 1);
    elem = Arrays.copyOf(elem, capacity + 1);
  }
}
