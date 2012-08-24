package org.opentripplanner.routing.edgetype;

/**
 * The updater module depends on the routing module. Maven doesn't allow circular dependencies 
 * (for good reasons). Therefore the updater modules cannot be visible from the routing module, 
 * and we cannot poll them without defining an interface in routing.
 * 
 * This feels like a hack and is probably symptomatic of a need for either reshuffling
 * classes and modules, or using Spring wiring.
 * 
 * @author abyrd
 */
public interface TimetableSnapshotSource {

    /** 
     * @return an up-to-date snapshot mapping TripPatterns to Timetables. This snapshot and the
     * timetable objects it references are guaranteed to never change, so the requesting thread is 
     * provided a consistent view of all TripTimes. The routing thread need only release its 
     * reference to the snapshot to release resources.
     */
    public TimetableResolver getSnapshot();
    
}
