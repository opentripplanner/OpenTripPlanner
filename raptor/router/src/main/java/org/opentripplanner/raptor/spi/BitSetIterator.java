package org.opentripplanner.raptor.spi;

import java.util.BitSet;

/**
 * Iterate over the set bits in the {@code BitSet}.
 */
final class BitSetIterator implements IntIterator {

  private final BitSet set;
  private int nextIndex;

  BitSetIterator(BitSet set) {
    this.set = set;
    this.nextIndex = set.nextSetBit(nextIndex);
  }

  @Override
  public int next() {
    int index = nextIndex;
    nextIndex = set.nextSetBit(index + 1);
    return index;
  }

  @Override
  public boolean hasNext() {
    return nextIndex != -1;
  }
}
