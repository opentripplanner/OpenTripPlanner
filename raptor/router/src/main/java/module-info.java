/**
 * Transit routing engine (Range Raptor). Isolated from the rest of OTP: the only OTP dependency is
 * the utils module. Transit data is supplied through the SPI package.
 */
module org.opentripplanner.raptor {
  requires com.carrotsearch.hppc;
  requires org.opentripplanner.utils;
  requires org.slf4j;
  requires static org.jspecify;

  exports org.opentripplanner.raptor;
  exports org.opentripplanner.raptor.api.debug;
  exports org.opentripplanner.raptor.api.model;
  exports org.opentripplanner.raptor.api.path;
  exports org.opentripplanner.raptor.api.request;
  exports org.opentripplanner.raptor.api.request.via;
  exports org.opentripplanner.raptor.api.response;
  exports org.opentripplanner.raptor.configure;
  exports org.opentripplanner.raptor.debug;
  exports org.opentripplanner.raptor.extensions.direct.api;
  exports org.opentripplanner.raptor.extensions.extrasearch;
  exports org.opentripplanner.raptor.path;
  exports org.opentripplanner.raptor.spi;
}
