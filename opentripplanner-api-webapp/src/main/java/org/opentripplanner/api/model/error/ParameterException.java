package org.opentripplanner.api.model.error;

import org.opentripplanner.api.ws.Message;

/**
 * Signals a problem parsing or interpreting a query parameter.
 */
public class ParameterException extends Exception {

    public Message message;
    
    public ParameterException(Message message) {
        this.message = message;
    }
    
}
