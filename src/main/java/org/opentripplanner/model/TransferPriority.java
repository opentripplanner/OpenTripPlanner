package org.opentripplanner.model;

/**
 * This is equivalent to NeTEx InterchangeWeightingEnumeration. It can specify the priority
 * of transfers both at a Stop level or Interchange/Transfer level. OTP currently only supports
 * TransferPriority at the Stop level.
 *
 * GTFS currently does not support transfer priority.
 */
public enum TransferPriority {
  /**
   * The transfer should have the highest possible priority.
   */
  PREFERRED_TRANSFER,
  /**
   * The transfer should be prioritized over other transfers.
   */
  RECOMMENDED_TRANSFER,
  /**
   * Standard transfer rules should be used.
   */
  TRANSFER_ALLOWED,
  /**
   * Transfer is not allowed.
   */
  NO_TRANSFER
}
