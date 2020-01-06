package org.opentripplanner.transit.raptor.rangeraptor.debug;

import org.opentripplanner.transit.raptor.rangeraptor.view.DebugHandler;
import org.opentripplanner.transit.raptor.util.paretoset.ParetoSet;
import org.opentripplanner.transit.raptor.util.paretoset.ParetoSetEventListener;


/**
 * Use this class to attach a debugHandler to a pareto set. The handler will
 * be notified about all changes in the set.
 *
 * @param <T> The {@link ParetoSet} type.
 */
final class ParetoSetDebugHandlerAdapter<T> implements ParetoSetEventListener<T> {

    private final DebugHandler<? super T> debugHandler;

    ParetoSetDebugHandlerAdapter(DebugHandler<? super T> debugHandler) {
        this.debugHandler = debugHandler;
    }

    @Override
    public void notifyElementAccepted(T newElement) {
        debugHandler.accept(newElement);
    }

    @Override
    public void notifyElementDropped(T element, T droppedByElement) {
        debugHandler.drop(element, droppedByElement, null);
    }

    @Override
    public void notifyElementRejected(T element, T droppedByElement) {
        debugHandler.reject(element, droppedByElement, null);
    }
}
