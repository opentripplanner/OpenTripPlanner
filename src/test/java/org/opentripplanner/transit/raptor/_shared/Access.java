package org.opentripplanner.transit.raptor._shared;

class Access extends AbstractStopArrival {
    Access(int stop, int departureTime, int arrivalTime) {
        super(0, stop, departureTime, arrivalTime, 1000, null);
    }
    @Override public boolean arrivedByAccessLeg() { return true; }
}
