package org.opentripplanner.api.mapping;

import org.opentripplanner.model.TransitMode;
import org.opentripplanner.routing.core.TraverseMode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TraverseModeMapper {

    private static final Map<String, TraverseMode> toDomain;

    static {
        Map<String, TraverseMode> map = new HashMap<>();
        for (TraverseMode it : TraverseMode.values()) {
            map.put(mapToApi(it), it);
        }
        toDomain = Map.copyOf(map);
    }

    public static TraverseMode mapToDomain(String api) {
        if(api == null) {
            return null;
        }
        return toDomain.get(api);
    }

    public static String mapToApi(TraverseMode domain) {
        if(domain == null) {
            return null;
        }

        switch (domain) {
            case AIRPLANE: return "AIRPLANE";
            case BICYCLE: return "BICYCLE";
            case BUS: return "BUS";
            case CAR: return "CAR";
            case CABLE_CAR: return "CABLE_CAR";
            case FERRY: return "FERRY";
            case FUNICULAR: return "FUNICULAR";
            case GONDOLA: return "GONDOLA";
            case LEG_SWITCH: return "LEG_SWITCH";
            case RAIL: return "RAIL";
            case SUBWAY: return "SUBWAY";
            case TRAM: return "TRAM";
            case TRANSIT: return "TRANSIT";
            case WALK: return "WALK";
        }
        throw new IllegalArgumentException("Traverse mode not mapped: " + domain);
    }

    public static List<String> mapToApi(Set<TransitMode> domain) {
        if (domain == null) {
            return null;
        }
        return domain.stream().map(TraverseModeMapper::mapToApi).collect(Collectors.toList());
    }

    public static String mapToApi(TransitMode domain) {
        if(domain == null) {
            return null;
        }

        switch (domain) {
            case AIRPLANE: return "AIRPLANE";
            case BUS: return "BUS";
            case CABLE_CAR: return "CABLE_CAR";
            case FERRY: return "FERRY";
            case FUNICULAR: return "FUNICULAR";
            case GONDOLA: return "GONDOLA";
            case RAIL: return "RAIL";
            case SUBWAY: return "SUBWAY";
            case TRAM: return "TRAM";
        }
        throw new IllegalArgumentException("Traverse mode not mapped: " + domain);
    }
}
