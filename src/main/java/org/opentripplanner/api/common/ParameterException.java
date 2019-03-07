package org.opentripplanner.api.common;


/**
 * Signals a problem parsing or interpreting a query parameter.
 */
public class ParameterException extends Exception {
    private static final long serialVersionUID = 1L;
    
    public Message message;
    
    public ParameterException(Message message) {
        this.message = message;
    }
    
}
