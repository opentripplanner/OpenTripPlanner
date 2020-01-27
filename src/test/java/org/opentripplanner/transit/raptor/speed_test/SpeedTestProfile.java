package org.opentripplanner.transit.raptor.speed_test;

import org.opentripplanner.transit.raptor.api.request.Optimization;
import org.opentripplanner.transit.raptor.api.request.RaptorProfile;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


public enum SpeedTestProfile {
    std_range_raptor(
            "rr",
            "Standard Range Raptor, super fast [ transfers, arrival time, travel time ].",
            RaptorProfile.STANDARD,
            true
    ),
    std_range_raptor_reverse(
            "rrr",
            "Reverse Standard Range Raptor",
            RaptorProfile.STANDARD,
            false
    ),
    std_best_time(
            "bt",
            "Best Time Range Raptor, super fast. Arrival time only, no path.",
            RaptorProfile.BEST_TIME,
            true
    ),
    std_best_time_reverse(
            "btr",
            "Reverse Best Time Range Raptor",
            RaptorProfile.BEST_TIME,
            false
    ),
    no_wait_std(
            "ws",
            "Standard Range Raptor without waiting time.",
            RaptorProfile.NO_WAIT_STD,
            true
    ),
    no_wait_std_reverse(
            "wsr",
            "Reverse Standard Range Raptor without waiting time.",
            RaptorProfile.NO_WAIT_STD,
            false
    ),
    no_wait_best_time(
            "wt",
            "Best Time Range Raptor without waiting time.",
            RaptorProfile.NO_WAIT_BEST_TIME,
            true
    ),
    no_wait_best_time_reverse(
            "wtr",
            "Reverse Best Time Range Raptor without waiting time.",
            RaptorProfile.NO_WAIT_BEST_TIME,
            false
    ),
    mc_range_raptor(
            "mc",
            "Multi-Criteria Range Raptor [ transfers, arrival time, travel time, cost ].",
            RaptorProfile.MULTI_CRITERIA,
            true
    ),
    mc_destination(
            "md",
            "Multi-Criteria Range Raptor with check on destination arrival.",
            RaptorProfile.MULTI_CRITERIA,
            true,
            Optimization.PARETO_CHECK_AGAINST_DESTINATION
    ),
    mc_filter_stops(
            "ms",
            "Multi-Criteria Range Raptor with check on stop filter.",
            RaptorProfile.MULTI_CRITERIA,
            true,
            Optimization.TRANSFERS_STOP_FILTER
    ),
    mc_stop_destination(
            "mds",
            "Multi-Criteria Range Raptor with check on stop filter and destination arrival.",
            RaptorProfile.MULTI_CRITERIA,
            true,
            Optimization.PARETO_CHECK_AGAINST_DESTINATION,
            Optimization.TRANSFERS_STOP_FILTER
    ),
    ;

    final String shortName;
    final String description;
    final RaptorProfile raptorProfile;
    final boolean forward;
    final List<Optimization> optimizations;

    SpeedTestProfile(String shortName, String description, RaptorProfile profile, boolean forward, Optimization... optimizations) {
        this.shortName = shortName;
        this.description = description;
        this.raptorProfile = profile;
        this.forward = forward;
        this.optimizations = Arrays.asList(optimizations);
    }

    public static SpeedTestProfile[] parse(String profiles) {
        return Arrays.stream(profiles.split(",")).map(SpeedTestProfile::parseOne).toArray(SpeedTestProfile[]::new);
    }

    public static List<String> options() {
        return Arrays.stream(values()).map(SpeedTestProfile::description).collect(Collectors.toList());
    }


    /* private methods */

    private static SpeedTestProfile parseOne(String value) {
        try {
            return valueOf(value);
        } catch (IllegalArgumentException ignore) {
            for (SpeedTestProfile it : values()) {
                if (it.shortName.equalsIgnoreCase(value)) {
                    return it;
                }
            }
            throw new IllegalArgumentException(
                    "Profile is not valid: '" + value + "'\nProfiles:\n\t" +
                            Arrays.stream(values())
                                    .map(SpeedTestProfile::description)
                                    .collect(Collectors.joining("\n\t"))
                                    .replace('.', ' ')
            );
        }
    }

    private String description() {
        String text = description;

        if (name().equals(shortName)) {
            text = String.format("%s : %s", name(), text);
        } else {
            text = String.format("%s, %s : %s", shortName, name(), text);
        }

        if (raptorProfile != null) {
            text += String.format("\n·%22s%s", "", raptorProfile);
            text += forward ? ", FORWARD" : ", REVERSE";
            for (Optimization it : optimizations) {
                text += ", " + it.name();
            }
        }
        return text;
    }
}