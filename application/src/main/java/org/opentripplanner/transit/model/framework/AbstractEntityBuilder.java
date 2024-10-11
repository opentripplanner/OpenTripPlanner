package org.opentripplanner.transit.model.framework;

import java.util.Objects;

public abstract class AbstractEntityBuilder<
  E extends AbstractTransitEntity<E, B>, B extends AbstractEntityBuilder<E, B>
>
  extends AbstractBuilder<E, B>
  implements TransitEntityBuilder<E, B> {

  @SuppressWarnings("rawtypes")
  private static final EntityContext<?, ?> NOOP = new EntityContext() {};

  private FeedScopedId id;

  private final EntityContext<E, B> context;

  public AbstractEntityBuilder(FeedScopedId id, EntityContext<E, B> context) {
    super(null);
    this.id = id;
    this.context = Objects.requireNonNull(context);
  }

  public AbstractEntityBuilder(FeedScopedId id) {
    this(id, noopContext());
  }

  public AbstractEntityBuilder(E original, EntityContext<E, B> context) {
    super(original);
    this.id = original.getId();
    this.context = context;
  }

  public AbstractEntityBuilder(E original) {
    this(original, noopContext());
  }

  public final FeedScopedId getId() {
    return id;
  }

  public final B withId(FeedScopedId id) {
    this.id = id;
    //noinspection unchecked
    return (B) this;
  }

  @SuppressWarnings("unchecked")
  @Override
  public E save() {
    return context.save((B) this);
  }

  @SuppressWarnings("unchecked")
  private static <
    E extends AbstractTransitEntity<E, B>, B extends AbstractEntityBuilder<E, B>
  > EntityContext<E, B> noopContext() {
    return (EntityContext<E, B>) NOOP;
  }
}
