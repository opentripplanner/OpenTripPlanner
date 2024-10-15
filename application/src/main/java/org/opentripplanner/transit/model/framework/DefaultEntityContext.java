package org.opentripplanner.transit.model.framework;

public class DefaultEntityContext<
  E extends AbstractTransitEntity<E, B>, B extends AbstractEntityBuilder<E, B>
>
  implements EntityContext<E, B> {

  private final EntityById<E> target;

  public DefaultEntityContext(EntityById<E> target) {
    this.target = target;
  }

  @Override
  public E save(B builder) {
    E e = builder.build();
    target.add(e);
    return e;
  }
}
