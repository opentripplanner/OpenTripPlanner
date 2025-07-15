package org.opentripplanner.transit.api.model;

import java.util.Collection;

/**
 * {@link EmptyIsEverythingFilter} is a subclass of {@link FilterValues} that includes
 * everything if the values are null or empty.
 */
class EmptyIsEverythingFilter<E> extends FilterValues<E> {

  EmptyIsEverythingFilter(String name, Collection<E> values) {
    super(name, values);
  }

  @Override
  public boolean includeEverything() {
    return values == null || values.isEmpty();
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof EmptyIsEverythingFilter<?> && super.equals(obj);
  }
}
