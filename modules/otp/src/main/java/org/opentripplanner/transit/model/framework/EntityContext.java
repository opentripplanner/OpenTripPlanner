package org.opentripplanner.transit.model.framework;

public interface EntityContext<
  E extends AbstractTransitEntity<E, B>, B extends AbstractEntityBuilder<E, B>
> {
  default E save(B builder) {
    return builder.build();
  }
}
