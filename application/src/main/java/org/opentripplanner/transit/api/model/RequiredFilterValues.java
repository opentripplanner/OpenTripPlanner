package org.opentripplanner.transit.api.model;

import java.util.Collection;

/**
 * {@link RequiredFilterValues} is a subclass of {@link FilterValues} that requires at least one
 * value to be included.
 */
public class RequiredFilterValues<E> extends FilterValues<E> {

  RequiredFilterValues(String name, Collection<E> values) {
    super(name, values);
    if (values == null || values.isEmpty()) {
      throw new IllegalArgumentException("Filter %s values must not be empty.".formatted(name));
    }
  }

  @Override
  public boolean includeEverything() {
    // RequiredFilterValues should never include everything. In theory the filter values could be
    // exhaustive, but there is no check for that currently.
    return false;
  }
}
