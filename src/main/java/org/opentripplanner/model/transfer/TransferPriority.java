package org.opentripplanner.model.transfer;

/**
 * Prioritize transfers between to trips. The priority goes from the lowest value NOT_ALLOWED to the
 * highest priority PREFERRED. This follow the NeTEx/Transmodel naming and functionality. In GTFS
 * the priority is mapped using {@code transfer_type}:
 * <ol>
 *     <li>
 *         {@code 0 or empty -> PREFERRED}. Recommended transfer point between routes.
 *     </li>
 *     <li>
 *         {@code 1 -> ALLOWED}. Timed transfer point between two routes. The departing vehicle is
 *         expected to wait for the arriving one and leave sufficient time for a rider to transfer
 *         between routes. The transfer is also set as GUARANTEED.
 *     </li>
 *     <li>
 *         {@code 3 -> NOT_ALLOWED}. Transfers are not possible
 *     /li>
 * </ol>
 * <p>
 * Note that for {@code transfer_type=1} the guaranteed flag is also set causing it to take
 * precedence over the priority. A guaranteed ALLOWED transfer is preferred over a PREFERRED
 * none-guaranteed transfer.
 * <p>
 * Note that {@code transfer_type=2} is not a constraint, just a regular path transfer.
 */
public enum TransferPriority {
  /**
   * Avoid this transfer if possible.
   * <p>
   * GTFS: 3 - Transfers are not possible.
   */
  NOT_ALLOWED(1000_00),

  /**
   * This is the same as a regular transfer.
   * <p>
   * GTFS: 1 - Timed transfer point, 2 - Transfer requires a minimum amount of time.
   */
  ALLOWED(3_00),

  /**
   * A recommended transfer, but not as good as preferred.
   * <p>
   * GTFS: Not available in GTFS
   */
  RECOMMENDED(2_00),

  /**
   * The highest priority there exist.
   * <p>
   * GTFS: 0 or empty - Recommended transfer point between routes.
   */
  PREFERRED(1_00);

  private final int cost;

  TransferPriority(int cost) {
    this.cost = cost;
  }

  /**
   * The default priority is ALLOWED. All transfers are ALLOWED unless they have the priority set to
   * something else.
   */
  public boolean isConstrained() {
    return this != ALLOWED;
  }

  /**
   * This method returns a cost for how good a transfer priority is compared to other transfer
   * priorities. The cost can only be used to compare transfers and should not be mixed with the
   * generalized-cost. A regular transfer (without any constraints) has the same cost as ALLOWED.
   * <p>
   * <ol>
   * <li>{@code PREFERRED} - cost: 1 points.</li>
   * <li>{@code RECOMMENDED} - cost: 2 points.</li>
   * <li>{@code ALLOWED} - cost: 3 points.</li>
   * <li>{@code NOT_ALLOWED} - cost: 1000 points.</li>
   * </ol>
   */
  public int cost() {
    return this.cost;
  }
}
