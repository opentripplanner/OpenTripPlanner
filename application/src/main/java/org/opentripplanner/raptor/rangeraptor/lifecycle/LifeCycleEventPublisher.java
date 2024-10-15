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
    this.onRouteSearchListeners = subscriptions.onRouteSearchListeners.toArray(new Consumer[0]);
    this.setupIterationListeners =
      subscriptions.setupIterationListeners.toArray(new IntConsumer[0]);
    this.prepareForNextRoundListeners =
      subscriptions.prepareForNextRoundListeners.toArray(new IntConsumer[0]);
    this.transitsForRoundCompleteListeners =
      subscriptions.transitsForRoundCompleteListeners.toArray(new Runnable[0]);
    this.transfersForRoundCompleteListeners =
      subscriptions.transfersForRoundCompleteListeners.toArray(new Runnable[0]);
    this.roundCompleteListeners = subscriptions.roundCompleteListeners.toArray(new Consumer[0]);
    this.iterationCompleteListeners =
      subscriptions.iterationCompleteListeners.toArray(new Runnable[0]);
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
