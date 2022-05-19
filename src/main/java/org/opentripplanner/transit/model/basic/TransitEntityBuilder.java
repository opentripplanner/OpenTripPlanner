package org.opentripplanner.transit.model.basic;

import javax.annotation.Nonnull;

public abstract class TransitEntityBuilder<
  E extends TransitEntity2<E, B>, B extends TransitEntityBuilder<E, B>
>
  extends AbstractBuilder<E, B> {

  private FeedScopedId id;

  public TransitEntityBuilder(FeedScopedId id) {
    super(null);
    setId(id);
  }

  public TransitEntityBuilder(E original) {
    super(original);
  }

  public final FeedScopedId getId() {
    return id;
  }

  public final B setId(FeedScopedId id) {
    this.id = id;
    //noinspection unchecked
    return (B) this;
  }

  @Override
  protected final void updateValues(@Nonnull E original) {
    this.id = original.getId();
    updateValues2(original);
  }

  @Override
  protected final void clearValues() {
    this.id = null;
    clearValues2();
  }

  protected abstract void updateValues2(@Nonnull E original);

  protected abstract void clearValues2();
}
