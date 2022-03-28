package org.opentripplanner.transit.raptor.speed_test.testcase;

import java.util.List;
import org.opentripplanner.routing.api.request.RequestModes;
import org.opentripplanner.transit.raptor.speed_test.model.Place;
import org.opentripplanner.util.time.DurationUtils;
import org.opentripplanner.util.time.TimeUtils;

import static org.opentripplanner.transit.raptor.speed_test.testcase.TestCase.NOT_SET;

public record TestCaseDefinition(
        String id,
        String description,
        int departureTime,
        int arrivalTime,
        int window,
        Place fromPlace,
        Place toPlace,
        List<String>tags,
        RequestModes modes
) {



    @Override
    public String toString() {
        return String.format(
                "#%s %s - %s, %s - %s, %s-%s(%s)",
                id, fromPlace.name(), toPlace.name(),
                fromPlace.coordinate(),
                toPlace.coordinate(),
                TimeUtils.timeToStrCompact(departureTime, NOT_SET),
                TimeUtils.timeToStrCompact(arrivalTime, NOT_SET),
                DurationUtils.durationToStr(window, NOT_SET)
        );
    }
}
