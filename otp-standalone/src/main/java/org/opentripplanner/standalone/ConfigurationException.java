package org.opentripplanner.standalone;

/**
 * Indicates that something went wrong when interpreting command line options, loading properties,
 * or assembling the OTP server based on those settings. 
 * @author abyrd
 */
public class ConfigurationException extends Exception {

    private static final long serialVersionUID = -23L;

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

}
