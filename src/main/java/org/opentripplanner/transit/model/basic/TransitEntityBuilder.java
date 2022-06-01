package org.opentripplanner.transit.model.basic;

import javax.annotation.Nonnull;

public abstract class TransitEntityBuilder<
  E extends TransitEntity2<E, B>, B extends TransitEntityBuilder<E, B>
>
  extends AbstractBuilder<E, B> {

  private FeedScopedId id;

  public TransitEntityBuilder(FeedScopedId id) {
    super(null);
    withId(id);
  }

  public TransitEntityBuilder(E original) {
    super(original);
  }

  public final FeedScopedId getId() {
    return id;
  }

  public final B withId(FeedScopedId id) {
    this.id = id;
    //noinspection unchecked
    return (B) this;
  }

  @Override
  protected final void update(@Nonnull E original) {
    this.id = original.getId();
    updateLocal(original);
  }

  /**
   * This method replaces the {@link #update(TransitEntity2)} in subclasses. This is done to
   * enforce the contract - the {@link #update(TransitEntity2)} is made final and call this
   * method. Any subclass must implement this method and assign all local fields in the method
   * implementation. The alternative is to override the {@link #update(TransitEntity2)} method and
   * call the super update, but this is error prone.
   */
  protected abstract void updateLocal(@Nonnull E original);
}
