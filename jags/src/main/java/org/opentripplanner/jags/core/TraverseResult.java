package org.opentripplanner.jags.core;

public class TraverseResult {
    public double weight;

    public State state;

    public TraverseResult(double weight, State sprime) {
        this.weight = weight;
        this.state = sprime;
    }

    public String toString() {
        return this.weight + " " + this.state;
    }
}