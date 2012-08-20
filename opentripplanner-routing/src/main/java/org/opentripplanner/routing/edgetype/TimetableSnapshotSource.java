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

    public TimetableResolver getSnapshot();
    
}
