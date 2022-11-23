package org.opentripplanner.raptor.spi;

/**
 * Iterator for fast iteration over int base type integers without boxing and unboxing.
 */
public interface IntIterator {
  /**
   * Retrieve the next int in sequence. SHOULD only be called ONCE per iteration, the implementation
   * can optimize the iterator implementation by incrementing the value in this method.
   */
  int next();

  /**
   * @return true if there is more int values in the sequence.
   */
  boolean hasNext();
}
