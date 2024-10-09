package org.opentripplanner.transit.model.framework;

public interface TransitBuilder<E extends TransitObject<E, B>, B extends TransitBuilder<E, B>> {
  /**
   * Build a new object based on the values set in the builder. This method is NOT context aware -
   * any context is not updated. Use the {@link TransitEntityBuilder#save()} method instead to
   * build an object and store it in the context. This method is useful if you need to build an
   * object which should be request scoped or used in a test.
   * <p>
   * For value objects are stored as "part of" an entity, but OTP tries to reuse objects using the
   * {@code Deduplicator}. This method may or may not be context aware, using a deduplicator to
   * avoid unnecessary object creation.
   */
  E build();
}
