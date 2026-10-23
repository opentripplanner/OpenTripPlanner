package org.opentripplanner.transit.transfer.regular.spi;

import org.opentripplanner.transit.transfer.regular.api.AbstractUserPreferences;

/**
 * The only boundary the regular-transfer pipeline has into street-search territory. The street
 * path type {@code P} stays fully opaque to this module - it never sees
 * {@code Edge}/{@code State}/{@code RouteRequest}. An implementation is backed by a street
 * search.
 *
 * @param <P> the street path type
 */
public interface TransferPathProvider<P> {
  /** The street search and re-costing of transfer paths under {@code userPreferences}. */
  TransferSearch<P> searchFor(AbstractUserPreferences<?> userPreferences);
}
