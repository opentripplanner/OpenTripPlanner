package org.opentripplanner.routing.util;

/**
 * Interface for something that generates integer identifiers.
 * 
 * @author avi
 */
public interface UniqueIdGenerator<T> {
    
    /**
     * Generates the identifier. May consider the element.
     * 
     * @return
     */
    int getId();
}
