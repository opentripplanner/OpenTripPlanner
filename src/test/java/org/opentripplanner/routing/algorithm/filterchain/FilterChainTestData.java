package org.opentripplanner.routing.algorithm.filterchain;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.routing.core.TraverseMode;

import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Calendar.FEBRUARY;

public class FilterChainTestData {
    public static final Place A = place("A", 5.0, 8.0);
    public static final Place B = place("B", 6.0, 8.5);
    public static final Place C = place("C", 7.0, 9.0);
    public static final Place D = place("D", 8.0, 9.5);
    public static final Place E = place("E", 9.0, 10.0);
    public static final Place F = place("F", 9.0, 10.5);

    public static Itinerary itinerary(Leg ... legs) {
        return new Itinerary(Arrays.asList(legs));
    }

    public static Leg leg(Place from, Place to, int t0, int t1, double distanceMeters, TraverseMode mode) {
        Leg leg = new Leg();
        leg.mode = mode;
        leg.from = from;
        leg.to = to;
        leg.startTime = newTime(t0);
        leg.endTime = newTime(t1);
        leg.distanceMeters = distanceMeters;
        return leg;
    }

    public static Leg leg(String legName, int t0, int t1, double distanceMeters, TraverseMode mode) {
        Leg leg = new Leg();
        leg.mode = mode;
        leg.from = place(legName, 1.0, 2.0);
        leg.to = place("", 3.0, 4.0);
        leg.startTime = newTime(t0);
        leg.endTime = newTime(t1);
        leg.distanceMeters = distanceMeters;
        return leg;
    }

    public static GregorianCalendar newTime(int minutes) {
        return new GregorianCalendar(2020, FEBRUARY, 2, 12, minutes);
    }

    public static Place place(String name, double lon, double lat) {
        Place p = new Place(1.2, 1.2, name);
        p.stopId = new FeedScopedId("S", name);
        return p;
    }

    public static String toStr(List<Itinerary> list) {
        return list.stream().map(it -> "{" + toStr(it) + "}").collect(Collectors.joining(", "));
    }

    public static String toStr(Itinerary it) {
        return it.legs.stream()
                .map(FilterChainTestData::toStr)
                .collect(Collectors.joining(", "))
                ;
    }

    public static String toStr(Leg leg) {
        return leg.from.name + leg.to.name
                + "(" + leg.startTime.get(Calendar.MINUTE) + "-"
                + leg.endTime.get(Calendar.MINUTE) + ")";
    }
}
