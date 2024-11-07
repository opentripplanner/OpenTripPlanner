package org.opentripplanner.raptor.util;

import java.util.BitSet;
import org.opentripplanner.raptor.spi.IntIterator;

/**
 * TODO TGR
 */
public final class BitSetIterator implements IntIterator {

  private final BitSet set;
  private int nextIndex;

  public BitSetIterator(BitSet set) {
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
