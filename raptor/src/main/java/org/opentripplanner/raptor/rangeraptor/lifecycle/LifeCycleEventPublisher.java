package org.opentripplanner.raptor.rangeraptor.lifecycle;

import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * The responsibility of this class is to publish workerlifecycle events on behalf of the Range
 * Raptor Worker.RangeRaptor Worker delegate this to this class.
 */
public class LifeCycleEventPublisher {

  private final Consumer<Boolean>[] onRouteSearchListeners;
  private final IntConsumer[] setupIterationListeners;
  private final IntConsumer[] prepareForNextRoundListeners;
  private final Runnable[] transitsForRoundCompleteListeners;
  private final Runnable[] transfersForRoundCompleteListeners;
  private final Consumer<Boolean>[] roundCompleteListeners;
  private final Runnable[] iterationCompleteListeners;

  @SuppressWarnings("unchecked")
  public LifeCycleEventPublisher(LifeCycleSubscriptions subscriptions) {
    this.onRouteSearchListeners = subscriptions.onRouteSearchListeners.toArray(Consumer[]::new);
    this.setupIterationListeners =
      subscriptions.setupIterationListeners.toArray(IntConsumer[]::new);
    this.prepareForNextRoundListeners =
      subscriptions.prepareForNextRoundListeners.toArray(IntConsumer[]::new);
    this.transitsForRoundCompleteListeners =
      subscriptions.transitsForRoundCompleteListeners.toArray(Runnable[]::new);
    this.transfersForRoundCompleteListeners =
      subscriptions.transfersForRoundCompleteListeners.toArray(Runnable[]::new);
    this.roundCompleteListeners = subscriptions.roundCompleteListeners.toArray(Consumer[]::new);
    this.iterationCompleteListeners =
      subscriptions.iterationCompleteListeners.toArray(Runnable[]::new);
    subscriptions.close();
  }

  /* Lifecycle methods invoked by the Range Raptor Worker */

  public final void notifyRouteSearchStart(boolean searchForward) {
    for (Consumer<Boolean> it : onRouteSearchListeners) {
      it.accept(searchForward);
    }
  }

  public final void setupIteration(int iterationDepartureTime) {
    for (IntConsumer it : setupIterationListeners) {
      it.accept(iterationDepartureTime);
    }
  }

  public final void prepareForNextRound(final int round) {
    for (IntConsumer it : prepareForNextRoundListeners) {
      it.accept(round);
    }
  }

  public final void transitsForRoundComplete() {
    for (Runnable it : transitsForRoundCompleteListeners) {
      it.run();
    }
  }

  public final void transfersForRoundComplete() {
    for (Runnable it : transfersForRoundCompleteListeners) {
      it.run();
    }
  }

  public final void roundComplete(boolean destinationReached) {
    for (Consumer<Boolean> it : roundCompleteListeners) {
      it.accept(destinationReached);
    }
  }

  public final void iterationComplete() {
    for (Runnable it : iterationCompleteListeners) {
      it.run();
    }
  }
}
