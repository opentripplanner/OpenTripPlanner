package org.opentripplanner.transit.api.model;

import java.util.Collection;

/**
 * {@link FilterValuesNullIsEverything} is a subclass of {@link FilterValues} that
 * includes everything only if the values collection is null.
 */
public class FilterValuesNullIsEverything<E> extends FilterValues<E> {

  FilterValuesNullIsEverything(String name, Collection<E> values) {
    super(name, values);
  }

  @Override
  public boolean includeEverything() {
    return values == null;
  }
}
