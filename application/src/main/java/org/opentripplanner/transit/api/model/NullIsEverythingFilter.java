package org.opentripplanner.transit.api.model;

import java.util.Collection;

/**
 * {@link NullIsEverythingFilter} is a subclass of {@link FilterValues} that
 * includes everything only if the values collection is null.
 */
class NullIsEverythingFilter<E> extends FilterValues<E> {

  NullIsEverythingFilter(String name, Collection<E> values) {
    super(name, values);
  }

  @Override
  public boolean includeEverything() {
    return values == null;
  }
}
