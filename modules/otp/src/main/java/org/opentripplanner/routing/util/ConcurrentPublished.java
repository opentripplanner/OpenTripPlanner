package org.opentripplanner.routing.util;

/**
 * This class is used in a situation where an updater thread wants to create a sequence of objects
 * over time and share them with a pool of reader threads.
 * <p>
 * All statements leading up to a call to publish(value) by thread A will appear to happen in the
 * order they occur in the source code, as seen by all statements following a call to get() in
 * thread B.
 * <p>
 * The JMM guarantees reference assignment alone is atomic (no read-tearing), but does not guarantee
 * that the combined process of assigning to fields of X and then assigning a reference to X is
 * atomic.
 * <p>
 * This class encapsulates the simple mechanism giving us the desired effect, revealing its
 * semantics through the type name, and providing an object to lock on (to avoid cluttering compound
 * types with lock fields, or locking on the entire compound type instance just to publish one of
 * its fields). Using private fields, it also prevents the containing object from bypassing the
 * synchronized block and accessing the reference directly, and prevents other code from interfering
 * with (locking on) the lock object.
 */
public class ConcurrentPublished<T> {

  private final Object lock = new Object();
  private T value;

  /**
   * The published value should be effectively immutable, i.e. all writes to its fields and
   * referenced objects should be complete before this method is called, and no readers should ever
   * modify its fields or referenced objects.
   */
  public void publish(T value) {
    synchronized (lock) {
      this.value = value;
    }
  }

  public T get() {
    synchronized (lock) {
      return value;
    }
  }
}
