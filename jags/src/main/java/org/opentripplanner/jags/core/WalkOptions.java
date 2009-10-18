package org.opentripplanner.jags.core;

import org.opentripplanner.jags.gtfs.GtfsContext;

public class WalkOptions {
    public double speed; //fixme: figure out units
    public boolean bicycle;
	public double transferPenalty = 120;
	
	private GtfsContext _context;
    
    public WalkOptions() {
        this.speed = 0.85;
        this.bicycle = false;
    }
    
    public GtfsContext getGtfsContext( ){
      return _context;
    }

    public void setGtfsContext(GtfsContext context) {
      _context = context;
    }
}