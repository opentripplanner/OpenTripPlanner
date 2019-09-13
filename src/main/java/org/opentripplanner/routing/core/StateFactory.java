package org.opentripplanner.routing.core;

/**
 * Factory interface for creating initial {@link State} objects. Useful if you wish to override the
 * default {@link State} or {@link StateData} implementation.
 * 
 * @author bdferris
 */
public interface StateFactory {
    public State createState(long time);
}
