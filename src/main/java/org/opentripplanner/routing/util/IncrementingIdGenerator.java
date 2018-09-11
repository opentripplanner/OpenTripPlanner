package org.opentripplanner.routing.util;


/**
 * Generates unique identifiers by incrementing an internal counter.
 * 
 * @author avi
 */
public class IncrementingIdGenerator<T> implements UniqueIdGenerator<T> {
    
    private int next;
    
    public IncrementingIdGenerator() {
        this(0);
    }
    
    /**
     * Construct with a starting counter. 
     * 
     * First call to next() will return start.
     * 
     * @param start
     */
    public IncrementingIdGenerator(int start) {
        next = start;
    }
    
    /**
     * Generates the next identifier.
     * 
     * @return 
     */
    public int getId(T elem) {
        return next++;
    }
}
