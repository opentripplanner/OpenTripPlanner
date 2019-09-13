package org.opentripplanner.api.model.error;

public class TransitError {
    private String message;
    
    public TransitError() {}
    
    public TransitError (String message) {
        this.message = message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

}
