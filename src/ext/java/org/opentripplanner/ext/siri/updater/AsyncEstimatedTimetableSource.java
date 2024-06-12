package org.opentripplanner.ext.siri.updater;

import java.util.function.Consumer;
import uk.org.siri.siri20.ServiceDelivery;

/**
 * A source of estimated timetables produced by an asynchronous (push) SIRI-ET feed.
 */
public interface AsyncEstimatedTimetableSource {
  /**
   * Start reading from the SIRI-ET feed and forward the estimated timetables to a consumer for
   * further processing.
   * <br>Starting the source includes all the necessary steps to set up the network
   * communication with the SIRI-ET feed as well as the (optional) processing of the message
   * backlog, that is the recent history of SIRI-ET messages produced by this feed and made
   * available by a message cache.
   *
   * @param serviceDeliveryConsumer a consumer of estimated timetable responsible for applying the
   *                                update to the transit model.
   */
  void start(Consumer<ServiceDelivery> serviceDeliveryConsumer);
}
