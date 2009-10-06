package org.opentripplanner.jags.core;
public class WalkOptions {
    public double speed;
    public boolean bicycle;
    
    public WalkOptions() {
        this.speed = 0.85;
        this.bicycle = false;
    }
}