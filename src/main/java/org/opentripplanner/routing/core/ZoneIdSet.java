package org.opentripplanner.routing.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.opentripplanner.routing.graph.GraphIndex;

import com.google.common.collect.Sets;

public class ZoneIdSet {

    private final Set<String> availableZones;
    private final GraphIndex graphIndex;
    private final Map<String, Set<String>> cache = new HashMap<>();

    public ZoneIdSet(final Set<String> availableZones, final GraphIndex graphIndex) {
        this.availableZones = availableZones;
        this.graphIndex = graphIndex;
    }

    public ZoneIdSet() {
        this.availableZones = null;
        this.graphIndex = null;
    }

    public boolean isAllowed(final String zone) {
        if(availableZones==null) {
            return true;
        }
        
        Set<String> requiredZones = cache.get(zone);
        if(requiredZones == null) {
            requiredZones = getNameSet( graphIndex, Sets.newHashSet(zone));
            cache.put(zone,  requiredZones);
        }
       
        for(String required: requiredZones) {
            if(!availableZones.contains(required)) {
                return false;
            }
        }    
        return true;
    }

    
    /**
     * @param index 
     * @param availableTicketNames comma separated list of available ticket names
     * @return
     */
    public static final ZoneIdSet create(final GraphIndex index, final String availableTicketNames) {
        if (availableTicketNames != null) {
            final Set<String> availableSet = ZoneIdSet.getNameSet(index, Sets.newHashSet(availableTicketNames));
            return new ZoneIdSet(availableSet, index);
        }
        return new ZoneIdSet();
    }
    
    
    private static final Map<String, String> getIdNameMap(final GraphIndex index){
        final HashMap<String, String> map = new HashMap<>();
        
        index.getAllTicketTypes().stream()
        .filter(tt->{
           return tt.rs.getContains().size()==1;
        }).forEach(tt->{
           map.put(tt.rs.getContains().iterator().next(),tt.getId());
        });
       
        return map;       
    }
    
    /**
     * Maps ticket types to individual zones. For example HSL_seu becomes HSL_hki, HSL_esp, HSL_van 
     * 
     * @param index Graph index to use
     * @param ticketNames set of ticket names to map
     * @return Set of mapped ticket names
     */
    public static final Set<String> getNameSet(final GraphIndex index, Set<String> ticketNames){
        if (ticketNames == null) {
            return null;
        }
        
        final HashSet<String> mapped = new HashSet<>();
        
        final Map<String, String> map = getIdNameMap(index);
        
        for (TicketType tt : index.getAllTicketTypes()) {
            if (ticketNames.contains(tt.rs.getFareAttribute().getId().toString())) {
                mapped.addAll(tt.rs.getContains().stream().map(id->map.get(id)).collect(Collectors.toList()));
            }
        }
      
        return mapped;       
    }
    
    @Override
    public String toString() {
        return availableZones == null ? "All zones allowed" : "Allowed zones: " + availableZones.toString();
    }

    public Set<String> getTicketIds() {
        return availableZones;
    }
}
