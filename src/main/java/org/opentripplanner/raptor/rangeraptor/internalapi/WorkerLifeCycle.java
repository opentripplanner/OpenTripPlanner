package org.opentripplanner.raptor.rangeraptor.internalapi;

import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * The class is responsible for registration of Range Raptor Worker life cycle event listeners. The
 * listeners add them self to this class an receive a callbacks on the subscribed event. The
 * listener is notified when thise Range Raptor events occur:
 * <ol>
 *     <li><b>setupIteration</b> with iteration departureTime</li>
 *     <li><b>prepareForNextRound</b></li>
 *     <li><b>transitsForRoundComplete</b></li>
 *     <li><b>transfersForRoundComplete</b></li>
 *     <li><b>roundComplete</b> with flag to indicate if the destination is reached</li>
 *     <li><b>iterationComplete</b></li>
 * </ol>
 * By providing the ability to subscribe to such events each class can decide
 * independently of its relations to subscribe. For example can the DestinationArrivals
 * class subscribe to any events, without relying on its parent (WorkerState)
 * to delegate these events down the relationship three. This decouples the
 * code.
 */
public interface WorkerLifeCycle {
  /**
   * Subscribe to 'routing search' events by register a boolean consumer. The route search
   * subscriber is notified before each routing search is done and the search direction is passed in
   * as a boolean flag to subscriber.
   */
  void onRouteSearch(Consumer<Boolean> routeSearchWithDirectionSubscriber);

  /**
   * Subscribe to 'setup iteration' events by register a int consumer. Every time an iteration start
   * the listener(the input parameter) is notified with the {@code iterationDepartureTime} passed in
   * as an argument.
   *
   * @param setupIterationWithDepartureTime if {@code null} nothing is added to the publisher.
   */
  void onSetupIteration(IntConsumer setupIterationWithDepartureTime);

  /**
   * Subscribe to 'prepare for next round' events by register listener. Every time a new round start
   * the listener(the input parameter) is notified/invoked with the current round as a argument.
   *
   * @param prepareForNextRound if {@code null} nothing is added to the publisher. The round
   *                            number(0..n) is passed to the subscriber.
   */
  void onPrepareForNextRound(IntConsumer prepareForNextRound);

  /**
   * Subscribe to 'transits for round complete' events by register listener. This event occur when
   * the transit calculation in each round is finished/complete and the registered listener(the
   * input parameter) is notified/invoked.
   *
   * @param transitsForRoundComplete if {@code null} nothing is added to the publisher.
   */
  void onTransitsForRoundComplete(Runnable transitsForRoundComplete);

  /**
   * Subscribe to 'transfers for round complete' events by register listener. This event occur when
   * the all transfers are calculated in each round. The registered listener(the input parameter) is
   * notified/invoked when this happens.
   *
   * @param transfersForRoundComplete if {@code null} nothing is added to the publisher.
   */
  void onTransfersForRoundComplete(Runnable transfersForRoundComplete);

  /**
   * Subscribe to 'round complete' events by register a boolean consumer. Every time a round finish
   * the listener(the input parameter) is notified with a flag indicating if the destination is
   * reached in the current round.
   *
   * @param roundCompleteWithDestinationReached if {@code null} nothing is added to the publisher.
   */
  void onRoundComplete(Consumer<Boolean> roundCompleteWithDestinationReached);

  /**
   * Subscribe to 'iteration complete' events by register listener. Every time an iteration
   * finish/completes the listener(the input parameter) is notified/invoked.
   *
   * @param iterationComplete if {@code null} nothing is added to the publisher.
   */
  void onIterationComplete(Runnable iterationComplete);
}
