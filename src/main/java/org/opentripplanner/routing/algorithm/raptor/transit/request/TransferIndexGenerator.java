package org.opentripplanner.routing.algorithm.raptor.transit.request;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.transfer.Transfer;
import org.opentripplanner.model.transfer.TransferService;
import org.opentripplanner.routing.algorithm.raptor.transit.TripSchedule;
import org.opentripplanner.transit.raptor.api.transit.GuaranteedTransfer;

public class TransferIndexGenerator {
    private final TransferService transferService;
    private final Map<Trip, TripSchedule> tripScheduleIndex;

    private TransferIndexGenerator(
            TransferService transferService,
            Map<Trip, TripSchedule> tripScheduleIndex
    ) {
        this.transferService = transferService;
        this.tripScheduleIndex = tripScheduleIndex;
    }

    public static void generateTransfers(
            TransferService transferService,
            List<TripPatternForDates> tripPatterns
    ) {
        var index = createTripScheduleIndex(tripPatterns);
        var generator = new TransferIndexGenerator(transferService, index);
        for (TripPatternForDates pattern : tripPatterns) {
            generator.generateTransfers(pattern);
        }
    }

    private void generateTransfers(TripPatternForDates pattern) {
        for (var tripSchedule : pattern.listTripSchedules()) {
            var trip = tripSchedule.getOriginalTripTimes().trip;
            for (int stopPos=0; stopPos < pattern.numberOfStopsInPattern(); ++stopPos) {
                var transfers= transferService.listGuaranteedTransfersTo(trip, stopPos);
                for (Transfer transfer : transfers) {
                    TripSchedule fromTrip = tripScheduleIndex.get(transfer.getFrom().getTrip());
                    TripSchedule toTrip = tripScheduleIndex.get(transfer.getTo().getTrip());
                    if(fromTrip != null && toTrip != null) {
                        var tx = new GuaranteedTransfer<>(
                                fromTrip, transfer.getFrom().getStopPosition(),
                                toTrip,   transfer.getTo().getStopPosition()
                        );
                        pattern.addTransfersTo(tx);
                        ((TripPatternForDates) tx.getFromTrip().pattern()).addTransferFrom(tx);
                    }
                }
            }
        }
    }

    private static Map<Trip, TripSchedule> createTripScheduleIndex(List<TripPatternForDates> tripPatterns) {
        Map<Trip, TripSchedule> index = new HashMap<>();
        for (TripPatternForDates pattern : tripPatterns) {
            for (int i=0; i< pattern.numberOfTripSchedules(); i++) {
                var schedule = pattern.getTripSchedule(i);
                index.put(schedule.getOriginalTripTimes().trip, schedule);
            }
        }
        return index;
    }
}
