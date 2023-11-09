package org.opentripplanner.transit.model.framework;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class AbstractBuilder<
  E extends TransitObject<E, B>, B extends AbstractBuilder<E, B>
>
  implements TransitBuilder<E, B> {

  private final E original;

  public AbstractBuilder(@Nullable E original) {
    this.original = original;
  }

  E original() {
    return original;
  }

  /**
   * Create a new instance, following the pattern (from the Agency class):
   * <pre>
   * protected Agency buildFromValues() {
   *   return new Agency(this);
   * }
   * </pre>
   */
  protected abstract E buildFromValues();

  @Override
  public final @Nonnull E build() {
    var b = buildFromValues();

    if (original == null) {
      return b;
    }
    // Make sure we only make a new object if it is changed.
    // Another approach is also to use the Deduplicator, but that is a hassle without DI in place.
    return original.sameAs(b) ? original : b;
  }
}
