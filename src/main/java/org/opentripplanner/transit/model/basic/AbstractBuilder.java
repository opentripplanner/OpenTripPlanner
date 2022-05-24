package org.opentripplanner.transit.model.basic;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class AbstractBuilder<
  E extends TransitObject<E, B>, B extends AbstractBuilder<E, B>
> {

  private E original;

  public AbstractBuilder(@Nullable E original) {
    this.original = original;
    if (this.original != null) {
      update(original);
    }
  }

  /**
   * The original entity used as a template for the builer,
   */
  @Nullable
  public E original() {
    return original;
  }

  /**
   * Set all fields using the given <em>none null</em> original. This method is call from the
   * constructor.
   */
  protected abstract void update(@Nonnull E original);

  /**
   * Create a new instance, following the pattern (from the Agency class):
   * <pre>
   * protected Agency buildFromValues() {
   *   return new Agency(this);
   * }
   * </pre>
   */
  protected abstract E buildFromValues();

  public final @Nonnull E build() {
    var b = buildFromValues();

    if (original == null) {
      return b;
    }
    // Make sure we only make a new object if it is changed.
    // Another approach is also to use the Deduplicator, but that is a hassle without DI in place.
    return original.sameValue(b) ? original : b;
  }
}
