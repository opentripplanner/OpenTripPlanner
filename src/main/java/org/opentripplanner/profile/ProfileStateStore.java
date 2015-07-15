package org.opentripplanner.profile;

import java.util.Collection;

import org.opentripplanner.routing.vertextype.TransitStop;

/** Stores profile states for a search */
public interface ProfileStateStore {
    /**
     * store a profile state, if it is not dominated.
     * @return true if state was nondominated
     */
    public boolean put(ProfileState ps);
    
    /** get the nondominated states at a particular vertex */
    public Collection<ProfileState> get(TransitStop tstop);
    
    /** get all nondominated states */
    public Collection<ProfileState> getAll();
    
    /** the number of profile states stored */
    public int size();
    
    /** the transit stops represented */
    public Collection<TransitStop> keys ();

    public boolean containsKey(TransitStop transitStop);
}
