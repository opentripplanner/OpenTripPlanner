package org.opentripplanner.routing.algorithm.raptor.transit.request;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.transfer.Transfer;
import org.opentripplanner.model.transfer.TransferService;
import org.opentripplanner.routing.algorithm.raptor.transit.TripSchedule;
import org.opentripplanner.transit.raptor.util.AvgTimer;

public class TransferIndexGenerator {
    private final static AvgTimer GENERATE_TRANSFERS_TIMER = AvgTimer.timerMilliSec("TransferIndexGenerator:generateTransfers");

    private final TransferService transferService;
    private final Map<Trip, TripPatternForDates> patternByTrip = new HashMap<>();

    private TransferIndexGenerator(TransferService transferService) {
        this.transferService = transferService;
    }


    public static void generateTransfers(
            TransferService transferService,
            List<TripPatternForDates> tripPatterns
    ) {
        GENERATE_TRANSFERS_TIMER.time(() -> {
            var generator = new TransferIndexGenerator(transferService);

            generator.setupPatternByTripIndex(tripPatterns);

            for (TripPatternForDates pattern : tripPatterns) {
                generator.generateTransfers(pattern);
            }
        });
    }

    private void setupPatternByTripIndex(List<TripPatternForDates> tripPatterns) {
        for (TripPatternForDates pattern : tripPatterns) {
            for (int i=0; i<pattern.numberOfTripSchedules(); ++i) {
                Trip trip = pattern.getTripSchedule(i).getOriginalTripTimes().trip;
                patternByTrip.put(trip, pattern);
            }
        }
    }

    private void generateTransfers(TripPatternForDates pattern) {
        for (int i=0; i<pattern.numberOfTripSchedules(); ++i) {
            TripSchedule tripSchedule = pattern.getTripSchedule(i);
            var trip = tripSchedule.getOriginalTripTimes().trip;
            for (int stopPos=0; stopPos < pattern.numberOfStopsInPattern(); ++stopPos) {
                var transfers= transferService.listGuaranteedTransfersTo(trip, stopPos);
                for (Transfer tx : transfers) {
                    if(tx.isGuaranteed() || tx.isStaySeated()) {
                        var fromTrip = tx.getFrom().getTrip();
                        var toTrip = tx.getTo().getTrip();
                        if (fromTrip != null && toTrip != null) {
                            var fromPattern = patternByTrip.get(fromTrip);
                            if (fromPattern != null) {
                                pattern.addGuaranteedTransfersTo(tx);
                                fromPattern.addGuaranteedTransferFrom(tx);
                            }
                        }
                    }
                }
            }
        }
    }
}
