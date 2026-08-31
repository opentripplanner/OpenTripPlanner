package org.opentripplanner.routing.graph.kryosupport;

import com.esotericsoftware.kryo.util.MapReferenceResolver;

/**
 * A {@link MapReferenceResolver} pre-sized for the number of reference-tracked objects in an OTP
 * graph. With references enabled, Kryo tracks every serialized object: a country-sized graph holds
 * tens of millions of them (~59 million measured for Norway). Kryo's default resolver grows to
 * that size incrementally — the write side rehashes its identity map ~20 times, each rehash
 * re-inserting every entry, and the read side repeatedly grows an ArrayList. Pre-sizing removes
 * that churn from graph save and load (measured: -16% save time for Norway).
 * <p>
 * Each side is sized lazily on its first use, so a serializing Kryo never allocates the read-side
 * list and, more importantly, a deserializing Kryo never allocates the ~1 GB write-side identity
 * map.
 * <p>
 * The expected count is passed to the superclass as {@code maximumCapacity} as well, so that the
 * {@link MapReferenceResolver#reset()} called between the top-level objects in a graph file does
 * not trim the pre-sized structures back down.
 */
class PreSizedReferenceResolver extends MapReferenceResolver {

  private final int expectedObjects;
  private boolean writeSideSized = false;
  private boolean readSideSized = false;

  PreSizedReferenceResolver(int expectedObjects) {
    super(expectedObjects);
    this.expectedObjects = expectedObjects;
  }

  @Override
  public int addWrittenObject(Object object) {
    if (!writeSideSized) {
      writeSideSized = true;
      writtenObjects.ensureCapacity(expectedObjects);
    }
    return super.addWrittenObject(object);
  }

  @Override
  public int nextReadId(Class type) {
    if (!readSideSized) {
      readSideSized = true;
      readObjects.ensureCapacity(expectedObjects);
    }
    return super.nextReadId(type);
  }
}
