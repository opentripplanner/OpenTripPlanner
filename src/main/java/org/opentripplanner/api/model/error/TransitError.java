package org.opentripplanner.api.model.error;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class TransitError {
    private String message;
    
    public TransitError() {}
    
    public TransitError (String message) {
        this.message = message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }

    @XmlElement(name="message")
    public String getMessage() {
        return message;
    }

}
