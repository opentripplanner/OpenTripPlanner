package org.opentripplanner.transit.model.framework;

public abstract class AbstractEntityBuilder<
  E extends AbstractTransitEntity<E, B>, B extends AbstractEntityBuilder<E, B>
>
  extends AbstractBuilder<E, B>
  implements TransitEntityBuilder<E, B> {

  private FeedScopedId id;

  public AbstractEntityBuilder(FeedScopedId id) {
    super(null);
    this.id = id;
  }

  public AbstractEntityBuilder(E original) {
    super(original);
    this.id = original.getId();
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
  public E save() {
    // TODO RTM - Implement when service/context is added
    return build();
  }

  @Override
  public void delete() {
    // TODO RTM - Implement when service/context is added
  }
}
