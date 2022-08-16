package org.opentripplanner.transit.raptor.util;

import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.BitSet;
import org.opentripplanner.transit.raptor.api.transit.IntIterator;

/**
 * TODO TGR
 */
public final class LuceneBitSetIterator implements IntIterator {

  private final BitSet set;
  private int nextIndex;

  public LuceneBitSetIterator(BitSet set) {
    this.set = set;
    this.nextIndex = set.nextSetBit(nextIndex);
  }

  @Override
  public int next() {
    int index = nextIndex;
    nextIndex =
      index == set.length() - 1 ? DocIdSetIterator.NO_MORE_DOCS : set.nextSetBit(index + 1);
    return index;
  }

  @Override
  public boolean hasNext() {
    return nextIndex != DocIdSetIterator.NO_MORE_DOCS;
  }
}
