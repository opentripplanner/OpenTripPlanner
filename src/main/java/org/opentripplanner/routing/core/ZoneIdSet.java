package org.opentripplanner.routing.core;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.opentripplanner.routing.graph.GraphIndex;

import com.google.common.collect.Sets;

public class ZoneIdSet {

    private final Set<String> zones;

    public ZoneIdSet(Set<String> zones) {
        this.zones = zones;
    }

    public ZoneIdSet() {
        this.zones = null;
    }

    public boolean isAllowed(String zone) {
        return zones == null || zones.contains(zone);
    }

    public static final ZoneIdSet create(GraphIndex index, String ticketIds) {
        if (ticketIds != null) {
            Set<String> ticketTypes = Arrays.asList(ticketIds.split(",")).stream().map(String::trim)
                    .collect(Collectors.toSet());
            Set<String> zones = Sets.newHashSet();
            for (TicketType tt : index.getAllTicketTypes()) {
                if (ticketTypes.contains(tt.rs.getFareAttribute().getId().toString())) {
                    zones.addAll(tt.rs.getContains());
                }
            }
            return new ZoneIdSet(zones);
        }
        return new ZoneIdSet();
    }

    @Override
    public String toString() {
        return zones == null ? "All zones allowed" : zones.toString();
    }
}
