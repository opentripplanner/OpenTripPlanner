package org.opentripplanner.transit.raptor.speed_test;

import org.opentripplanner.transit.raptor.api.request.Optimization;
import org.opentripplanner.transit.raptor.api.request.RangeRaptorProfile;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


public enum SpeedTestProfile {
    std_range_raptor(
            "rr",
            "Standard Range Raptor, super fast [ transfers, arrival time, travel time ].",
            RangeRaptorProfile.STANDARD,
            true
    ),
    std_range_raptor_reverse(
            "rrr",
            "Reverse Standard Range Raptor",
            RangeRaptorProfile.STANDARD,
            false
    ),
    std_best_time(
            "bt",
            "Best Time Range Raptor, super fast. Arrival time only, no path.",
            RangeRaptorProfile.BEST_TIME,
            true
    ),
    std_best_time_reverse(
            "btr",
            "Reverse Best Time Range Raptor",
            RangeRaptorProfile.BEST_TIME,
            false
    ),
    no_wait_std(
            "ws",
            "Standard Range Raptor without waiting time.",
            RangeRaptorProfile.NO_WAIT_STD,
            true
    ),
    no_wait_std_reverse(
            "wsr",
            "Reverse Standard Range Raptor without waiting time.",
            RangeRaptorProfile.NO_WAIT_STD,
            false
    ),
    no_wait_best_time(
            "wt",
            "Best Time Range Raptor without waiting time.",
            RangeRaptorProfile.NO_WAIT_BEST_TIME,
            true
    ),
    no_wait_best_time_reverse(
            "wtr",
            "Reverse Best Time Range Raptor without waiting time.",
            RangeRaptorProfile.NO_WAIT_BEST_TIME,
            false
    ),
    mc_range_raptor(
            "mc",
            "Multi-Criteria Range Raptor [ transfers, arrival time, travel time, cost ].",
            RangeRaptorProfile.MULTI_CRITERIA,
            true
    ),
    mc_destination(
            "md",
            "Multi-Criteria Range Raptor with check on destination arrival.",
            RangeRaptorProfile.MULTI_CRITERIA,
            true,
            Optimization.PARETO_CHECK_AGAINST_DESTINATION
    ),
    mc_filter_stops(
            "ms",
            "Multi-Criteria Range Raptor with check on stop filter.",
            RangeRaptorProfile.MULTI_CRITERIA,
            true,
            Optimization.TRANSFERS_STOP_FILTER
    ),
    mc_stop_destination(
            "mds",
            "Multi-Criteria Range Raptor with check on stop filter and destination arrival.",
            RangeRaptorProfile.MULTI_CRITERIA,
            true,
            Optimization.PARETO_CHECK_AGAINST_DESTINATION,
            Optimization.TRANSFERS_STOP_FILTER
    ),
    ;

    final String shortName;
    final String description;
    final RangeRaptorProfile raptorProfile;
    final boolean forward;
    final List<Optimization> optimizations;

    SpeedTestProfile(String shortName, String description, RangeRaptorProfile profile, boolean forward, Optimization... optimizations) {
        this.shortName = shortName;
        this.description = description;
        this.raptorProfile = profile;
        this.forward = forward;
        this.optimizations = Arrays.asList(optimizations);
    }

    public static org.opentripplanner.transit.raptor.speed_test.SpeedTestProfile[] parse(String profiles) {
        return Arrays.stream(profiles.split(",")).map(org.opentripplanner.transit.raptor.speed_test.SpeedTestProfile::parseOne).toArray(org.opentripplanner.transit.raptor.speed_test.SpeedTestProfile[]::new);
    }

    public static List<String> options() {
        return Arrays.stream(values()).map(org.opentripplanner.transit.raptor.speed_test.SpeedTestProfile::description).collect(Collectors.toList());
    }

    /* private methods */

    private static org.opentripplanner.transit.raptor.speed_test.SpeedTestProfile parseOne(String value) {
        try {
            return valueOf(value);
        } catch (IllegalArgumentException ignore) {
            for (org.opentripplanner.transit.raptor.speed_test.SpeedTestProfile it : values()) {
                if (it.shortName.equalsIgnoreCase(value)) {
                    return it;
                }
            }
            throw new IllegalArgumentException(
                    "Profile is not valid: '" + value + "'\nProfiles:\n\t" +
                            Arrays.stream(values())
                                    .map(org.opentripplanner.transit.raptor.speed_test.SpeedTestProfile::description)
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