package org.opentripplanner.warmup;

import org.opentripplanner.street.geometry.WgsCoordinate;

/** Strategy for executing warmup queries against a specific GraphQL API. */
interface WarmupQueryStrategy {
  /**
   * Execute one warmup query. The strategy picks access/egress modes and any other per-query
   * parameters from the given {@code queryCount}, so the caller only needs a running counter.
   *
   * @return true if the query executed without GraphQL errors.
   */
  boolean execute(WgsCoordinate from, WgsCoordinate to, boolean arriveBy, int queryCount);
}
