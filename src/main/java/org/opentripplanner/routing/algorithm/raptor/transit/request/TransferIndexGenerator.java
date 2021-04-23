package org.opentripplanner.routing.algorithm.raptor.transit.request;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.transfer.Transfer;
import org.opentripplanner.model.transfer.TransferService;
import org.opentripplanner.routing.algorithm.raptor.transit.TripSchedule;
import org.opentripplanner.transit.raptor.util.AvgTimer;

public class TransferIndexGenerator {
    private final TransferService transferService;
    private final Map<LocalDate, Map<Trip, TripSchedule>> tripScheduleIndex;

    private TransferIndexGenerator(
            TransferService transferService,
            Map<LocalDate, Map<Trip, TripSchedule>> tripScheduleIndex
    ) {
        this.transferService = transferService;
        this.tripScheduleIndex = tripScheduleIndex;
    }

    private static AvgTimer timerGenTx = AvgTimer.timerMilliSec("a_generateTransfers");


    public static void generateTransfers(
            TransferService transferService,
            List<TripPatternForDates> tripPatterns
    ) {
        timerGenTx.time(() -> {
            var index = createTripScheduleIndex(tripPatterns);
            var generator = new TransferIndexGenerator(transferService, index);
            for (TripPatternForDates pattern : tripPatterns) {
                generator.generateTransfers(pattern);
            }
        });
    }

    private void generateTransfers(TripPatternForDates pattern) {
        for (int i=0; i<pattern.numberOfTripSchedules(); ++i) {
            TripSchedule tripSchedule = pattern.getTripSchedule(i);
            Map<Trip, TripSchedule> index = tripScheduleIndex.get(tripSchedule.getServiceDate());
            var trip = tripSchedule.getOriginalTripTimes().trip;
            for (int stopPos=0; stopPos < pattern.numberOfStopsInPattern(); ++stopPos) {
                var transfers= transferService.listGuaranteedTransfersTo(trip, stopPos);
                for (Transfer transfer : transfers) {
                    var fromTrip = index.get(transfer.getFrom().getTrip());
                    var toTrip = index.get(transfer.getTo().getTrip());
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

    private static Map<LocalDate, Map<Trip, TripSchedule>> createTripScheduleIndex(List<TripPatternForDates> tripPatterns) {
        Map<LocalDate, Map<Trip, TripSchedule>> index = new HashMap<>();

        for (TripPatternForDates pattern : tripPatterns) {
            for (int i=0; i< pattern.numberOfTripSchedules(); i++) {
                var schedule = pattern.getTripSchedule(i);
                var indexForDay = index.computeIfAbsent(schedule.getServiceDate(), d -> new HashMap<>());
                indexForDay.put(schedule.getOriginalTripTimes().trip, schedule);
            }
        }
        return index;
    }
}
