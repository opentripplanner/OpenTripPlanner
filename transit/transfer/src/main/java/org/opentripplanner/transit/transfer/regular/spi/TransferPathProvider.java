package org.opentripplanner.transit.transfer.regular.spi;

/**
 * The only boundary the regular-transfer pipeline has into street-search territory. The street
 * path type {@code P} and the request type {@code R} stay fully opaque to this module - it never
 * sees {@code Edge}/{@code State}/{@code RouteRequest}. An implementation is backed by a street
 * search.
 *
 * @param <P> the street path type
 * @param <R> the request type, see {@link TransferRequestMapper}
 */
public interface TransferPathProvider<P, R> {
  /** The street search and re-costing of transfer paths with {@code request}. */
  TransferSearch<P> searchFor(R request);
}
