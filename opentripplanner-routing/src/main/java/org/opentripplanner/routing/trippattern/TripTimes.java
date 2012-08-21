package org.opentripplanner.routing.trippattern;

import org.onebusaway.gtfs.model.Trip;

/**
 * A TripTimes represents the arrival and departure times for a single trip in an Timetable. It
 * is carried along by States when routing to ensure that they have a consistent, fast view of the 
 * trip when realtime updates are being applied. 
 * All times are expressed as seconds since midnight (as in GTFS). The indexes into a StopTimes are 
 * not stop indexes, but inter-stop segment ("hop") indexes, so hop 0 refers to the hop between 
 * stops 0 and 1, and arrival 0 is actually an arrival at stop 1. The main reason for this is that 
 * it saves two extra array elements in every stopTimes. It might be worth it to just use stop 
 * indexes everywhere for simplicity.
 */
public interface TripTimes {

    public static final int PASSED = -1;
    public static final int CANCELED = -2;
    
    /** @return the trips whose arrivals and departures are represented by this TripTimes */
    public Trip getTrip();
    
    /** @return the base trip times which this particular TripTimes represents or modifies */
    public ScheduledTripTimes getScheduledTripTimes();

    /** @return the number of inter-stop segments (hops) on this trip */
    public int getNumHops();
    
    /** 
     * @return the amount of time in seconds that the vehicle waits at the stop *before* traversing 
     * each inter-stop segment ("hop"). It is undefined for hop 0, and at the end of a trip. 
     */
    public int getDwellTime(int hop);
    
    /** 
     * @return the length of time time in seconds that it takes for the vehicle to traverse each 
     * inter-stop segment ("hop"). 
     */
    public int getRunningTime(int hop);

    /** 
     * @return the time in seconds after midnight at which the vehicle begins traversing each 
     * inter-stop segment ("hop"). 
     */
    public int getDepartureTime(int hop);
    
    /** 
     * @return the time in seconds after midnight at which the vehicle arrives at the end of each 
     * inter-stop segment ("hop"). A null value indicates that all dwells are 0-length, and arrival 
     * times are to be derived from the departure times array. 
     */
    public int getArrivalTime(int hop);
    
    /** 
     * Request that this TripTimes be analyzed and its memory usage reduced if possible. 
     * @return whether or not compaction occurred. 
     */
    public boolean compact();
    
    /** Not named toString (which is in Object) so Lombok will delegate it */
    public String dumpTimes(); //toStringVerbose

}
