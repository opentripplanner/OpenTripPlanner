package org.opentripplanner.transit.transfer.regular.api;

import java.util.List;

/**
 * A report of the keys of the cache of the Raptor transfers, the user preferences of the requests,
 * and how many requests used each. The keys are tracked in a bounded list, the least recently used
 * key is dropped if the list is full, so the report is not complete if there are many keys and the
 * counts are approximate.
 * <p>
 * The statistics are only collected if they are enabled, the report is empty if they are not.
 * <p>
 * Implementations are thread-safe.
 */
public interface RegularTransferCacheReport {
  /** No statistics are collected. */
  RegularTransferCacheReport DISABLED = List::of;

  /** The tracked keys, the most used first. */
  List<TransferCacheReportEntry> entries();
}
