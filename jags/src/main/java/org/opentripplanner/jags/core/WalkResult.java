package org.opentripplanner.jags.core;

public class WalkResult {
    public double weight;

    public State state;

    public WalkResult(double weight, State sprime) {
        this.weight = weight;
        this.state = sprime;
    }

    public String toString() {
        return this.weight + " " + this.state;
    }
}