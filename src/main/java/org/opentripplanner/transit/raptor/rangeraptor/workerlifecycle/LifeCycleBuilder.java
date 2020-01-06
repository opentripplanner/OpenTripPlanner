package org.opentripplanner.transit.raptor.rangeraptor.workerlifecycle;

import org.opentripplanner.transit.raptor.rangeraptor.WorkerLifeCycle;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;


/**
 * The responsibility of this class is to provide an interface for Range Raptor
 * workerlifecycle event listeners to subscribe for such events.
 */
public final class LifeCycleBuilder implements WorkerLifeCycle {
    final List<IntConsumer> setupIterationListeners = new ArrayList<>();
    final List<Runnable> prepareForNextRoundListeners = new ArrayList<>();
    final List<Runnable> transitsForRoundCompleteListeners = new ArrayList<>();
    final List<Runnable> transfersForRoundCompleteListeners = new ArrayList<>();
    final List<Consumer<Boolean>> roundCompleteListeners = new ArrayList<>();
    final List<Runnable> iterationCompleteListeners = new ArrayList<>();


    @Override
    public void onSetupIteration(IntConsumer setupIterationWithDepartureTime) {
        if(setupIterationWithDepartureTime != null) {
            this.setupIterationListeners.add(setupIterationWithDepartureTime);
        }
    }

    @Override
    public void onPrepareForNextRound(Runnable prepareForNextRound) {
        if(prepareForNextRound != null) {
            this.prepareForNextRoundListeners.add(prepareForNextRound);
        }
    }

    @Override
    public void onTransitsForRoundComplete(Runnable transitsForRoundComplete) {
        if(transitsForRoundComplete != null) {
            this.transitsForRoundCompleteListeners.add(transitsForRoundComplete);
        }
    }

    @Override
    public void onTransfersForRoundComplete(Runnable transfersForRoundComplete) {
        if(transfersForRoundComplete != null) {
            this.transfersForRoundCompleteListeners.add(transfersForRoundComplete);
        }
    }

    @Override
    public void onRoundComplete(Consumer<Boolean> roundCompleteWithDestinationReached) {
        if(roundCompleteWithDestinationReached != null) {
            this.roundCompleteListeners.add(roundCompleteWithDestinationReached);
        }
    }

    @Override
    public void onIterationComplete(Runnable iterationComplete) {
        if(iterationComplete != null) {
            this.iterationCompleteListeners.add(iterationComplete);
        }
    }
}
