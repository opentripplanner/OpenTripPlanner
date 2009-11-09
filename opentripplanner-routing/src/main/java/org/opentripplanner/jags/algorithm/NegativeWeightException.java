package org.opentripplanner.jags.algorithm;

public class NegativeWeightException extends RuntimeException {

    private static final long serialVersionUID = -1018391017439852795L;

    public NegativeWeightException(String message) {
        super(message);
    }

}
