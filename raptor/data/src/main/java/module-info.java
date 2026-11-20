/**
 * Default implementations of the Raptor transit data SPI: the stop index and the regular transfer
 * store. Depends on the Raptor SPI and the core model, but not on any other OTP code.
 */
module org.opentripplanner.raptor.data {
  requires transitive org.opentripplanner.core.model;
  requires transitive org.opentripplanner.raptor;
  requires com.carrotsearch.hppc;
  requires org.opentripplanner.utils;
  requires static org.jspecify;

  exports org.opentripplanner.raptor.data.stop;
  exports org.opentripplanner.raptor.data.transfers.regular;
}
