package org.opentripplanner.routing.core;

import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.stream.Collectors;

public class TraversalStatistics implements Cloneable {

    protected double distanceInWalk = 0;
    private double distanceInCar = 0;
    private double distanceInBicycle = 0;
    private double distanceInTransit = 0;

    private int timeInWalk = 0;
    private int timeInCar = 0;
    private int timeInBicycle = 0;
    private int timeInTransit = 0;

    protected TraversalStatistics copy() {
        TraversalStatistics copy = new TraversalStatistics();
        copy.distanceInWalk = this.distanceInWalk;
        copy.distanceInCar = this.distanceInCar;
        copy.distanceInBicycle = this.distanceInBicycle;
        copy.distanceInTransit = this.distanceInTransit;
        copy.timeInWalk = this.timeInWalk;
        copy.timeInCar = this.timeInCar;
        copy.timeInBicycle = this.timeInBicycle;
        copy.timeInTransit = this.timeInTransit;
        return copy;
    }

    protected void increaseDistance(TraverseMode mode, double value) {
        if (mode == null) {
            return;
        }
        switch (mode) {
            case WALK:
                distanceInWalk += value;
                break;
            case CAR:
                distanceInCar += value;
                break;
            case BICYCLE:
                distanceInBicycle += value;
                break;
            default:
                if (mode.isTransit()) {
                    distanceInTransit += value;
                }
        }
    }

    protected void increaseTime(TraverseMode mode, int value) {
        if (mode == null) {
            return;
        }
        switch (mode) {
            case WALK:
                timeInWalk += value;
                break;
            case CAR:
                timeInCar += value;
                break;
            case BICYCLE:
                timeInBicycle += value;
                break;
            default:
                if (mode.isTransit()) {
                    timeInTransit += value;
                }
        }
    }

    protected Map<TraverseMode, Double> createDistanceTraversedInModeMap() {
        return ImmutableMap.of(
                TraverseMode.WALK, distanceInWalk,
                TraverseMode.CAR, distanceInCar,
                TraverseMode.BICYCLE, distanceInBicycle,
                TraverseMode.TRANSIT, distanceInTransit
        ).entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    protected Map<TraverseMode, Integer> createTimeTraversedInModeMap() {
        return ImmutableMap.of(
                TraverseMode.WALK, timeInWalk,
                TraverseMode.CAR, timeInCar,
                TraverseMode.BICYCLE, timeInBicycle,
                TraverseMode.TRANSIT, timeInTransit
        ).entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
