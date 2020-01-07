package org.opentripplanner.transit.raptor.rangeraptor.workerlifecycle;

import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * The responsibility of this class is to publish workerlifecycle events on behalf
 * of the Range Raptor Worker.RangeRaptor Worker delegate this to this class.
 */
public class LifeCycleEventPublisher {
    private final IntConsumer[] setupIterationListeners;
    private final Runnable[] prepareForNextRoundListeners;
    private final Runnable[] transitsForRoundCompleteListeners;
    private final Runnable[] transfersForRoundCompleteListeners;
    private final Consumer<Boolean>[] roundCompleteListeners;
    private final Runnable[] iterationCompleteListeners;

    @SuppressWarnings("unchecked")
    public LifeCycleEventPublisher(LifeCycleBuilder builder) {
        this.setupIterationListeners = builder.setupIterationListeners.toArray(new IntConsumer[0]);
        this.prepareForNextRoundListeners = builder.prepareForNextRoundListeners.toArray(new Runnable[0]);
        this.transitsForRoundCompleteListeners = builder.transitsForRoundCompleteListeners.toArray(new Runnable[0]);
        this.transfersForRoundCompleteListeners = builder.transfersForRoundCompleteListeners.toArray(new Runnable[0]);
        this.roundCompleteListeners = builder.roundCompleteListeners.toArray(new Consumer[0]);
        this.iterationCompleteListeners = builder.iterationCompleteListeners.toArray(new Runnable[0]);
    }

    /* Lifecycle methods invoked by the Range Raptor Worker */

    public final void setupIteration(int iterationDepartureTime) {
        for (IntConsumer it : setupIterationListeners) {
            it.accept(iterationDepartureTime);
        }
    }

    public final void prepareForNextRound() {
        for (Runnable it : prepareForNextRoundListeners) {
            it.run();
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
