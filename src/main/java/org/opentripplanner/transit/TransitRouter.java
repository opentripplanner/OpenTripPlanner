package org.opentripplanner.transit;

import gnu.trove.TIntCollection;
import gnu.trove.list.TIntList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 *  RAPTOR
 *  This just searches to the entire network, no goal direction or pruning except a max travel time.
 */
public class TransitRouter {

    private static Logger LOG = LoggerFactory.getLogger(TransitRouter.class);

    private TransitLayer transitLayer;

    public int maxTransfers = 4;

    public int boardSlackSeconds = 60;

    private BitSet servicesActive = new BitSet();

    private List<TIntIntMap> roundData = new ArrayList<>();

    TIntIntMap previousRound = null;

    TIntIntMap currentRound = null;

    TIntIntMap bestTimeForStop = new TIntIntHashMap(1000, 0.5f, -1, Integer.MAX_VALUE);

    BitSet touchedStops = new BitSet();

    BitSet touchedPatterns = new BitSet();

    public int maxTravelTimeSeconds = 60 * 60 * 2;

    private int departureTime; // seconds after midnight

    public TransitRouter (TransitLayer transitLayer) {
        this.transitLayer = transitLayer;
        newRound();
    }

    public void reset () {
        roundData.clear();
        touchedStops.clear();
        touchedPatterns.clear();
        bestTimeForStop.clear();
        newRound();
    }

    private void newRound () {
        if (currentRound != null) {
            markPatternsForStops();
            previousRound = currentRound;
        }
        currentRound = new TIntIntHashMap(transitLayer.getStopCount(), 0.5f, -1, Integer.MAX_VALUE);
        roundData.add(currentRound);
    }

    // TODO use LocalDateTime
    // public void setDepartureTime (int secondsAfterMidnight) {

    public void setOrigins (TIntIntMap distanceForStopIndex, int secondsAfterMidnight) {
        this.departureTime = secondsAfterMidnight;
        distanceForStopIndex.forEachEntry((originStopIndex, distanceMeters) -> {
            // TODO apply walking speeds properly
            currentRound.put(originStopIndex, distanceMeters + secondsAfterMidnight);
            return true;
        });
    }

    /**
     * For every stop that has been updated in the current round, mark all patterns that pass through that stop.
     */
    private void markPatternsForStops () {
        touchedPatterns.clear();
        // For each stop that has been updated in this round,
        currentRound.keySet().forEach(stopIndex -> {
            // Mark every pattern that passes through that stop,
            transitLayer.patternsForStop.get(stopIndex).forEach(patternIndex -> {
                // As long as the pattern has any active trips on it.
                TripPattern pattern = transitLayer.tripPatterns.get(patternIndex);
                boolean active = pattern.servicesActive.intersects(servicesActive);
                // LOG.info("Pattern {} has active services: {}", patternIndex, active);
                if (active) {
                    touchedPatterns.set(patternIndex);
                }
                return true;
            });
            return true;
        });
    }


    // Add time/date parameter
    public void route () {

        LOG.debug("Begin raptor routing. Initial state: {}", currentRound);

        // Determine which services are active to avoid exploring those that are not running on this day.
        LocalDate searchDate = new LocalDate(2015, 8, 6);
        servicesActive = transitLayer.getActiveServicesForDate(searchDate);
        final int latestAcceptableTime = departureTime + maxTravelTimeSeconds;

        while (true) {
            newRound();
            // For each pattern passing through a stop updated in the previous round,
            for (int p = touchedPatterns.nextSetBit(0); p >= 0; p = touchedPatterns.nextSetBit(p + 1)) {
                TripPattern tripPattern = transitLayer.tripPatterns.get(p);
                TripSchedule currentTrip = null;
                // Iterate over all the stops in that pattern.
                for (int s = 0; s < tripPattern.stops.length; s++) {
                    int stop = tripPattern.stops[s];
                    if (currentTrip == null) {
                        // We are not yet on-board the vehicle.
                        // Skip down the route to a stop that has been updated in the last round.
                        if (previousRound.keySet().contains(stop)) {
                            // This stop was updated in the last round. Attempt to board.
                            int time = previousRound.get(stop);
                            // Find a trip within the pattern. Might return null if boarding is impossible.
                            currentTrip = tripPattern.findNextDeparture(time + boardSlackSeconds, s);
                        }
                        continue;
                    }
                    // At this point we are necessarily on board a vehicle.
                    // Move down the pattern updating arrival times.
                    if (previousRound.containsKey(stop)) {
                        // Attempt re-board at stops that might allow an earlier trip.
                        // TODO efficient step-backward reboarding (with sorted stoptimes)... THE CURRENT METHOD IS PROBABLY VERY SLOW!
                        int prevRoundTime = previousRound.get(stop);
                        if (prevRoundTime + boardSlackSeconds < currentTrip.arrivals[s]) {
                            currentTrip = tripPattern.findNextDeparture(prevRoundTime + boardSlackSeconds, s);
                        }
                    }
                    int arrivalAtStop = currentTrip.arrivals[s];
                    if (arrivalAtStop < latestAcceptableTime) {
                        if (arrivalAtStop < bestTimeForStop.get(stop)) {
                            currentRound.put(stop, arrivalAtStop);
                            bestTimeForStop.put(stop, arrivalAtStop);
                        }
                    }
                }
            }
            LOG.debug("Round {} updated {} stops.", roundData.size() - 1, currentRound.size());
            if (currentRound.isEmpty()) {
                LOG.debug("Nothing was updated. Routing is finished.");
                break;
            } else if (roundData.size() > maxTransfers) {
                LOG.debug("Hit maximum number of transfers {}. Routing is finished.", maxTransfers);
                break;
            }
            doTransfers();
        }
    }

    // Really, this should create an additional round... if we're trying to reconstruct paths.
    private void doTransfers () {
        // For every stop that was updated,
        currentRound.forEachEntry((stopIndex, arrivalTime) -> {
            TIntList transfers = transitLayer.transfersForStop.get(stopIndex);
            for (int i = 0; i < transfers.size(); ) {
                int targetStopIndex = transfers.get(i++);
                int targetStopDistace = transfers.get(i++);
                int timeAtTarget = arrivalTime + targetStopDistace;
                if (timeAtTarget < bestTimeForStop.get(targetStopIndex)) {
                    if (timeAtTarget < currentRound.get(targetStopIndex)) {
                        currentRound.put(targetStopIndex, timeAtTarget);
                    }
                }
            }
            return true;
        });
    }


}
