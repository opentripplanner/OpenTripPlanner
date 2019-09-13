package org.opentripplanner.routing.edgetype;

/** An interface to be implemented by edges that need to perform some cleanup upon being detached */
public interface EdgeWithCleanup {
    public void detach();
}
