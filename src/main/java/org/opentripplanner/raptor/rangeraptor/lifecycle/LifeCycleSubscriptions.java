package org.opentripplanner.raptor.rangeraptor.lifecycle;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import org.opentripplanner.raptor.rangeraptor.internalapi.WorkerLifeCycle;

/**
 * The responsibility of this class is to collect all life-cycle subscriptions, and implement the
 * {@link WorkerLifeCycle} interface for subscribers to add their subscription. This collection is
 * passed on to the publisher after all subscriptions is collected. This make it possible to
 * decouple the publisher and subscriptions during setup.
 */
public final class LifeCycleSubscriptions implements WorkerLifeCycle {

  final List<Consumer<Boolean>> onRouteSearchListeners = new ArrayList<>();
  final List<IntConsumer> setupIterationListeners = new ArrayList<>();
  final List<IntConsumer> prepareForNextRoundListeners = new ArrayList<>();
  final List<Runnable> transitsForRoundCompleteListeners = new ArrayList<>();
  final List<Runnable> transfersForRoundCompleteListeners = new ArrayList<>();
  final List<Consumer<Boolean>> roundCompleteListeners = new ArrayList<>();
  final List<Runnable> iterationCompleteListeners = new ArrayList<>();

  private boolean openForSubscription = true;

  @Override
  public void onRouteSearch(Consumer<Boolean> routeSearchWithDirectionSubscriber) {
    subscribe(onRouteSearchListeners, routeSearchWithDirectionSubscriber);
  }

  @Override
  public void onSetupIteration(IntConsumer setupIterationWithDepartureTime) {
    subscribe(setupIterationListeners, setupIterationWithDepartureTime);
  }

  @Override
  public void onPrepareForNextRound(IntConsumer prepareForNextRound) {
    subscribe(prepareForNextRoundListeners, prepareForNextRound);
  }

  @Override
  public void onTransitsForRoundComplete(Runnable transitsForRoundComplete) {
    subscribe(transitsForRoundCompleteListeners, transitsForRoundComplete);
  }

  @Override
  public void onTransfersForRoundComplete(Runnable transfersForRoundComplete) {
    subscribe(transfersForRoundCompleteListeners, transfersForRoundComplete);
  }

  @Override
  public void onRoundComplete(Consumer<Boolean> roundCompleteWithDestinationReached) {
    subscribe(roundCompleteListeners, roundCompleteWithDestinationReached);
  }

  @Override
  public void onIterationComplete(Runnable iterationComplete) {
    subscribe(iterationCompleteListeners, iterationComplete);
  }

  public void close() {
    this.openForSubscription = false;
  }

  private <T> void subscribe(List<T> subscriptions, T subscriber) {
    assertIsOpen();
    if (subscriber != null) {
      subscriptions.add(subscriber);
    }
  }

  private void assertIsOpen() {
    if (!openForSubscription) {
      throw new IllegalStateException("Unable to subscribe, publisher already created.");
    }
  }
}
