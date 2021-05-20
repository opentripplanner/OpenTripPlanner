package org.opentripplanner.model.transfer;

/**
 * Prioritize transfers between to trips. The priority goes from the lowest value NOT_ALLOWED to
 * the highest priority PREFERRED. This follow the NeTEx/Transmodel naming and functionality. In
 * GTFS the priority is mapped using {@code transfer_type}:
 * <ol>
 *     <li>NOT_ALLOWED: 3 - Transfers are not possible
 *     <li>ALLOWED:  1 - Timed transfer point and 2 - Transfer requires a minimum amount of time,
 *     <li>PREFERRED: 0 or empty - Recommended transfer point between routes
 * </ol>
 * Note that for {@code transfer_type=1} the guarantied flag is also set causing it to take
 * residence over the priority. A guarantied ALLOWED transfer is preferred over a PREFERRED
 * none-guarantied transfer.
 */
public enum TransferPriority {
  /**
   * Avoid this transfer if possible.
   * <p>
   * GTFS: 3 - Transfers are not possible.
   */
  NOT_ALLOWED(1_000),

  /**
   * This is the same as a regular transfer.
   * <p>
   * GTFS: 1 - Timed transfer point, 2 - Transfer requires a minimum amount of time.
   */
  ALLOWED(0),

  /**
   * A recommended transfer, but not as good as preferred.
   * <p>
   * GTFS: Not available in GTFS
   */
  RECOMMENDED(-1),


  /**
   * The highest priority there exist.
   * <p>
   * GTFS: 0 or empty - Recommended transfer point between routes.
   */
  PREFERRED(-2);

  /**
   * STAY_SEATED is not a priority, but we assign a cost to it to be able to compare it with other
   * transfers with a priority and the {@link #GUARANTIED_TRANSFER_COST}.
   */
  public static final int  STAY_SEATED_TRANSFER_COST = -100;

  /**
   * GUARANTIED is not a priority, but we assign a cost to it to be able to compare it with other
   * transfers with a priority. The cost is better than a pure prioritized transfer, but the
   * priority and GUARANTIED attribute is added together; Hence a (GUARANTIED, RECOMMENDED)
   * transfer is better than (GUARANTIED, ALLOWED).
   */
  public static final int  GUARANTIED_TRANSFER_COST = -10;


  private final int cost;

  TransferPriority(int cost) {
    this.cost = cost;
  }

  /**
   * This method return a cost for how good a transfer is compared with another transfer. This
   * cost can only be used to compare transfers and should not be mixed with the transit
   * generalized-cost. A regular transit have cost 0(zero). A PREFERRED transfer have a negative
   * cost and the NOT_ALLOWED have a positive very high cost. We include stay-seated and guarantied
   * transfers here, even if thy are not priority values. The STAY_SEATED and GUARANTIED cost
   * value are added to the PRIORITY cost.
   *
   * @param staySeated is given a super-low cost to take precedence over all other possible options.
   * @param guarantied is lower than stay-seated, but better than the PREFERRED priority.
   */
  public int cost(boolean staySeated, boolean guarantied) {
    int cost = this.cost;
    if(staySeated) { cost += STAY_SEATED_TRANSFER_COST; }
    if(guarantied) { cost += GUARANTIED_TRANSFER_COST; }
    return cost;
  }
}
