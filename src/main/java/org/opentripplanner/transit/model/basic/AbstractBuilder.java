package org.opentripplanner.transit.model.basic;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class AbstractBuilder<
  E extends TransitObject<E, B>, B extends AbstractBuilder<E, B>
> {

  private E original;

  public AbstractBuilder(@Nullable E original) {
    setOriginal(original);
  }

  /**
   * The original entity used as a template for the builer,
   */
  @Nullable
  public E original() {
    return original;
  }

  public final void setOriginal(@Nullable E original) {
    this.original = original;

    if (original == null) {
      clearValues();
    } else {
      updateValues(original);
    }
  }

  protected abstract void updateValues(@Nonnull E original);

  protected abstract void clearValues();

  @Nullable
  protected abstract E buildFromValues();

  public final @Nonnull E build() {
    if (original == null) {
      return buildFromValues();
    }
    var b = buildFromValues();

    // Make sure we only make a new object if it is changed.
    // Another approach is also to use the Deduplicator, but that is a hassle without DI in place.
    return original.sameValue(b) ? original : b;
  }
}
