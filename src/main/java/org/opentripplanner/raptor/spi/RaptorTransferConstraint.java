package org.opentripplanner.raptor.spi;

/**
 * Raptor does not need any information from the constrained transfer, but it passes the instance in
 * a callback to the cost calculator.
 */
public interface RaptorTransferConstraint {
  /**
   * A regular transfer is a transfer with no constraints.
   */
  RaptorTransferConstraint REGULAR_TRANSFER = new RaptorTransferConstraint() {
    @Override
    public boolean isNotAllowed() {
      return false;
    }

    @Override
    public boolean isRegularTransfer() {
      return true;
    }

    @Override
    public boolean isStaySeated() {
      return false;
    }
  };

  /**
   * Return {@code true} if the constrained transfer is not allowed between the two routes. Note! If
   * a constraint only apply to specific trips, then the {@link RaptorConstrainedTripScheduleBoardingSearch}
   * is reponsible for NOT returning the NOT-ALLOWED transfer, and finding the next ALLOWED trip.
   */
  boolean isNotAllowed();

  /**
   * Returns {@code true} if this is a regular transfer without any constrains.
   */
  boolean isRegularTransfer();

  /**
   * Also known as interlining of GTFS trips with the same block id.
   */
  boolean isStaySeated();
}
