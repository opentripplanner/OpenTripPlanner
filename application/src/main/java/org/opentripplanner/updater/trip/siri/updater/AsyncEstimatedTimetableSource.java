package org.opentripplanner.updater.trip.siri.updater;

import java.util.concurrent.Future;
import java.util.function.Function;
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
   * @param serviceDeliveryConsumer apply asynchronously the updates to the transit model. Return a
   *                                future indicating when the updates are applied.
   */
  void start(Function<ServiceDelivery, Future<?>> serviceDeliveryConsumer);

  /**
   * Return true if the message backlog is processed and the source is ready to listen to the feed.
   */
  boolean isPrimed();
}
