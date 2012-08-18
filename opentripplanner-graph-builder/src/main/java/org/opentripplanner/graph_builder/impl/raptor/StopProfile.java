package org.opentripplanner.graph_builder.impl.raptor;

import java.sql.Time;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.edgetype.PatternBoard;
import org.opentripplanner.routing.edgetype.TableTripPattern;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.raptor.RaptorRoute;

/**
 * A profile of trip duration/walk distance for a given stop
 * 
 * @author novalis
 * 
 */

class CostsAtTime {
    int time;

    ArrayBag<TimeAndDistance> costs;

    public CostsAtTime(int time) {
        this.time = time;
        costs = new ArrayBag<TimeAndDistance>();
    }

    public void forceAdd(int duration, double distance) {
        /*
         * if (distance < 0 || duration < 0) { System.out.println("PANIC"); } if (!costs.isEmpty()
         * && (costs.item().duration == duration || Math.abs(costs.item().distance - distance) <
         * 0.001)) { System.out.println("Doom!"); }
         * 
         * if (costs.size() > 50) { System.out.println("DOOM"); }
         */
        costs.add(new TimeAndDistance(duration, distance));
    }

    public void forceAdd(TimeAndDistance cost) {
        costs.add(cost);
    }

    public boolean add(int duration, double distance) {
        for (Iterator<TimeAndDistance> it = costs.iterator(); it.hasNext();) {
            TimeAndDistance existing = it.next();
            if (existing.duration <= duration && existing.distance <= distance * 1.05) {
                return false;
            } else if (existing.duration >= duration && existing.distance * 1.05 >= distance) {
                it.remove();
            }
        }
        costs.add(new TimeAndDistance(duration, distance));
        // forceAdd(duration, distance);
        return true;
    }

    public boolean codominant(int duration, double distance) {
        for (TimeAndDistance existing : costs) {
            if (existing.duration <= duration && existing.distance <= distance * 1.05) {
                return false;
            }
            if (existing.duration > duration && existing.distance * 1.05 > distance) {
                return true;
            }
        }
        return true;
    }

    public String toString() {
        String costStr = "";
        int i = 1;
        for (TimeAndDistance cost : costs) {
            costStr += cost;
            costStr += "@" + (cost.duration + time);
            if (i++ < costs.size()) {
                if (i > 30) {
                    costStr += "...";
                    break;
                }
                costStr += ", ";
            }
        }
        return "[@" + time + ": " + costStr + "]";
    }
}

class DurationAndDistanceAtTimeBagComparator implements Comparator<Object> {

    @Override
    public int compare(Object arg0, Object arg1) {
        int t0, t1;
        if (arg0 instanceof Integer) {
            t0 = (Integer) arg0;
        } else if (arg0 instanceof CostsAtTime) {
            t0 = ((CostsAtTime) arg0).time;
        } else {
            throw new UnsupportedOperationException();
        }

        if (arg1 instanceof Integer) {
            t1 = (Integer) arg1;
        } else if (arg1 instanceof CostsAtTime) {
            t1 = ((CostsAtTime) arg1).time;
        } else {
            throw new UnsupportedOperationException();
        }
        if (t0 > t1) {
            return 1;
        } else if (t0 < t1) {
            return -1;
        } else {
            return 0;
        }
    }

}

public class StopProfile {
    ArrayList<CostsAtTime> profile;

    Vertex vertex;

    private Comparator<? super Object> comparator = new DurationAndDistanceAtTimeBagComparator();

    boolean atDestination;

    public StopProfile(Vertex vertex) {
        this(vertex, false);
    }

    public StopProfile(Vertex vertex, boolean atDestination) {
        this.vertex = vertex;
        profile = new ArrayList<CostsAtTime>();
        this.atDestination = atDestination;
    }

    public boolean transitTo(StopProfile destination, RaptorRoute route, int originStopIndex,
            int destinationStopIndex, ArrayList<ServiceDay> days, long startTime) {

        boolean anyBetter = false;
        for (PatternBoard board : route.boards[originStopIndex]) {
            anyBetter |= transitTo(destination, board.getPattern(), originStopIndex,
                    destinationStopIndex, days, startTime);
        }
        return anyBetter;
    }

    /**
     * Fill in this stop profile with data about trips from the origin stop to the destination stop.
     * This profile is associated with the origin stop.
     */
    public boolean transitTo(StopProfile destination, TableTripPattern pattern,
            int originStopIndex, int destinationStopIndex, ArrayList<ServiceDay> days,
            long startTime) {
        final int nTrips = pattern.getTrips().size();

        ArrayList<ServiceDay> runningList = new ArrayList<ServiceDay>();
        for (ServiceDay day : days) {
            if (day.serviceIdRunning(pattern.getServiceId()))
                runningList.add(day);
        }
        ServiceDay[] runningDays = runningList.toArray(new ServiceDay[0]);
        boolean anyBetter = false;
        for (int trip = nTrips - 1; trip >= 0; --trip) {
            int departureSecondsSinceMidnight = pattern.getDepartureTime(originStopIndex, trip);
            int arrivalSecondsSinceMidnight = pattern
                    .getArrivalTime(destinationStopIndex - 1, trip);
            final int duration = arrivalSecondsSinceMidnight - departureSecondsSinceMidnight;

            for (ServiceDay day : runningDays) {
                int arrivalTime = (int) (day.time(arrivalSecondsSinceMidnight) - startTime);
                int departureTime = arrivalTime - duration;

                if (departureTime > 86400 * 2) {
                    continue;
                }

                // find the costs for a trip arriving at arrivalTime

                CostsAtTime costsAtDestination;
                if (destination.atDestination) {
                    costsAtDestination = new CostsAtTime(arrivalTime);
                    costsAtDestination.forceAdd(0, 0.0);
                } else {
                    int arrivalTimeInsertionPoint = Collections.binarySearch(destination.profile,
                            arrivalTime, comparator);
                    if (arrivalTimeInsertionPoint < 0) {
                        arrivalTimeInsertionPoint = -arrivalTimeInsertionPoint - 1;
                    }
                    if (arrivalTimeInsertionPoint == destination.profile.size()) {
                        // this trip actually arrives after the last thing we know about
                        // at the destination.
                        /*
                         * System.out.println(
                         * "Unexpectedly found end of destination list; I think it is safe" +
                         * " to throw this away");
                         */
                        continue;
                    }
                    costsAtDestination = destination.profile.get(arrivalTimeInsertionPoint);
                }

                // find when a trip departing at departureTime would fit in this profile
                int originIndex = Collections.binarySearch(profile, departureTime, comparator);
                if (originIndex < 0) {
                    originIndex = -originIndex - 1;
                }
                // System.out.println("At  " + nextDestination.time);

                // find the next (or same-time) departure from here

                if (originIndex == profile.size()) {
                    CostsAtTime toInsert = new CostsAtTime(departureTime);
                    for (TimeAndDistance atDestination : costsAtDestination.costs) {
                        int walkDuration = atDestination.duration + duration;
                        double walkDistance = atDestination.distance;
                        toInsert.forceAdd(walkDuration, walkDistance);
                        anyBetter = true;
                    }
                    profile.add(toInsert);
                } else {
                    CostsAtTime costsAtNextTime = profile.get(originIndex);
                    if (costsAtNextTime.time >= departureTime) {
                        if (!addIfNotDominated(costsAtDestination, departureTime, originIndex,
                                duration, 0)) {
                            continue;
                        }
                        anyBetter = true;
                    }
                }

                if (originIndex > 0)
                    dominatePreviousDepartures(originIndex);

            }
        }
        return anyBetter;
    }

    /**
     * Walk to a stop distance/time away, with the profile destination.
     */
    public boolean walkTo(StopProfile destination, int duration, double distance) {
        if (profile.size() > 1000) {
            // System.out.println("large profile");
        }

        boolean anyBetter = false;

        Iterator<CostsAtTime> destinationIt = destination.profile.iterator();

        // System.out.println("Walk from " + destination.vertex + " to " + vertex);
        while (destinationIt.hasNext()) {
            // we need to check previous origin costs (if any) to see if we dominate them; if so, we
            // will remove them.
            CostsAtTime nextDestination = destinationIt.next();
            // System.out.println("At  " + nextDestination.time);
            int timeAtOrigin = nextDestination.time - duration;

            // find the next (or same-time) departure from here

            int originIndex = Collections.binarySearch(profile, timeAtOrigin, comparator);
            if (originIndex < 0) {
                originIndex = -originIndex - 1;
            }

            if (originIndex == profile.size()) {
                CostsAtTime toInsert = new CostsAtTime(timeAtOrigin);
                for (TimeAndDistance atDestination : nextDestination.costs) {
                    int walkDuration = atDestination.duration + duration;
                    double walkDistance = atDestination.distance + distance;
                    if (walkDistance > 3218)
                        continue;
                    toInsert.forceAdd(walkDuration, walkDistance);
                }
                profile.add(toInsert);
            } else {

                CostsAtTime costsAtNextTime = profile.get(originIndex);

                if (costsAtNextTime.time >= timeAtOrigin) {
                    if (!addIfNotDominated(nextDestination, timeAtOrigin, originIndex, duration,
                            distance)) {
                        continue;
                    }
                }
            }

            if (originIndex > 0)
                dominatePreviousDepartures(originIndex);
        }

        return anyBetter;
    }

    private void dominatePreviousDepartures(int originIndex) {
        CostsAtTime inserted = profile.get(originIndex);
        originIndex -= 1;

        int timeAtOrigin = inserted.time;

        int startRemove = -1;
        boolean shouldContinue = false;
        while (originIndex >= 0) {
            CostsAtTime costsAtPrevTime = profile.get(originIndex);
            int waitTime = timeAtOrigin - costsAtPrevTime.time;
            ArrayList<TimeAndDistance> toAdd = new ArrayList<TimeAndDistance>();
            for (Iterator<TimeAndDistance> costsIt = inserted.costs.iterator(); costsIt.hasNext();) {
                TimeAndDistance costs = costsIt.next();
                int duration = costs.duration + waitTime;
                boolean add = false;
                for (Iterator<TimeAndDistance> it = costsAtPrevTime.costs.iterator(); it.hasNext();) {
                    TimeAndDistance prevCost = it.next();
                    if (prevCost.duration >= duration && prevCost.distance * 1.001 >= costs.distance) {
                        // this walk totally dominates the old way
                        it.remove();
                        add = true;
                    } else if (prevCost.duration > duration
                            || prevCost.distance > costs.distance * 1.001) {
                        // this walk at least partially dominates the old way
                        add = true;
                    } else {
                        //the old way completely dominates this walk 
                        add = false;
                        break;
                    }
                }
                if (costsAtPrevTime.costs.isEmpty()) {
                    // fixme might optimize this
                    if (startRemove == -1)
                        startRemove = originIndex;
                    shouldContinue = true;
                    toAdd.clear();
                } else if (add) {
                    if (startRemove != -1) {
                        profile.subList(originIndex + 1, startRemove + 1).clear();
                        startRemove = -1;
                    }
                    toAdd.add(new TimeAndDistance(duration, costs.distance));
                    shouldContinue = true;
                    // System.out.println("Add to older");
                }
            }
            for (TimeAndDistance cost : toAdd) {
                costsAtPrevTime.forceAdd(cost);
            }
            if (shouldContinue) {
                originIndex--;
            } else {
                break;
            }
        }
        if (startRemove != -1) {
            profile.subList(originIndex + 1, startRemove + 1).clear();
        }
    }

    private boolean addIfNotDominated(CostsAtTime destinationCosts, int timeAtOrigin,
            int originIndex, int duration, double distance) {

        CostsAtTime costsAtNextTime = profile.get(originIndex);
        int waitTime = costsAtNextTime.time - timeAtOrigin;

        CostsAtTime toInsert = new CostsAtTime(timeAtOrigin);
        for (TimeAndDistance atDestination : destinationCosts.costs) {
            double walkDistance = distance + atDestination.distance;
            if (walkDistance > 3218)
                continue;
            int walkDuration = duration + atDestination.duration;
            toInsert.forceAdd(walkDuration, walkDistance);
        }

        for (Iterator<TimeAndDistance> costsIt = toInsert.costs.iterator; costsIt.hasNext();) {
            TimeAndDistance costs = costsIt.next();
            if (!costsAtNextTime.codominant(costs.duration + waitTime, costs.distance)) {
                costsIt.remove();
            }
        }

        if (toInsert.costs.isEmpty()) {
            return false;
        }
        if (waitTime == 0) {
            costsAtNextTime = profile.get(originIndex);
            for (TimeAndDistance costs : toInsert.costs) {
                costsAtNextTime.add(costs.duration, costs.distance);
            }
            return true;
        } else {
            profile.add(originIndex, toInsert);
            return true;
        }

    }

    public int getMaxDuration(int startTime, int endTime) {
        int worstDuration = 0;
        int lastTime = 0;
        for (CostsAtTime costs : profile) {

            // consider only arrivals within the one-day period
            if (costs.time < startTime)
                continue;
            if (costs.time >= endTime)
                break;

            int bestDuration = Integer.MAX_VALUE;
            for (TimeAndDistance departure : costs.costs) {
                final int time = departure.duration + costs.time - lastTime;
                if (time < bestDuration) {
                    bestDuration = time;
                }
            }
            if (bestDuration > worstDuration) {
                worstDuration = bestDuration;
            }
            lastTime = costs.time;
        }
        return worstDuration;
    }
}
