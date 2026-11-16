package org.opentripplanner.raptor.spi;

/*
 * This interface defines the data needed by Raptor. It is the main/top-level interface and
 * together with the {@code RaptorRequest} if provide all information needed by Raptor to perform
 * the search. It makes it possible to write small adapter between the "OTP Transit Layer" and the
 * Raptor algorithm.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public interface RaptorDataProvider<T extends RaptorTripSchedule> {
  /**
   * Return the transfer provider for this data set. This is used to find both regular and constrained transfers
   * between stops.
   */
  RaptorTransitDataProvider<T> transitData();

  /**
   * Return the transfer provider for this data set. This is used to find both regular and constrained transfers
   * between stops.
   */
  RaptorTransferDataProvider<T> transferData();
}
