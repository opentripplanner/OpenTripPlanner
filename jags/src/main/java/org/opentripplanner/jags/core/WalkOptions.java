package org.opentripplanner.jags.core;

import org.opentripplanner.jags.gtfs.GtfsContext;

public class WalkOptions {
    public double speed; // in meters/second

    public boolean bicycle;

    public double transferPenalty = 120;

    private GtfsContext _context;

    public WalkOptions() {
        // http://en.wikipedia.org/wiki/Walking
        this.speed = 1.33; // 1.33 m/s ~ 3mph, avg. human speed
        this.bicycle = false;
    }

    public WalkOptions(GtfsContext context) {
        this();
        this._context = context;
    }

    public GtfsContext getGtfsContext() {
        return _context;
    }

    public void setGtfsContext(GtfsContext context) {
        _context = context;
    }
}