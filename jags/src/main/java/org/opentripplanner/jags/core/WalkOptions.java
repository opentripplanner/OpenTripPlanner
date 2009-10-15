package org.opentripplanner.jags.core;
public class WalkOptions {
    public double speed; //fixme: figure out units
    public boolean bicycle;
	public double transferPenalty = 120; 
    
    public WalkOptions() {
        this.speed = 0.85;
        this.bicycle = false;
    }
}