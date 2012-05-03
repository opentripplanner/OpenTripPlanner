package org.opentripplanner.api.common;


/**
 * Signals a problem parsing or interpreting a query parameter.
 */
public class ParameterException extends Exception {

    public Message message;
    
    public ParameterException(Message message) {
        this.message = message;
    }
    
}
