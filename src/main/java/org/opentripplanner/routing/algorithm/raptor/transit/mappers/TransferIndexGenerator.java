package org.opentripplanner.routing.algorithm.raptor.transit.mappers;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.transfer.Transfer;
import org.opentripplanner.model.transfer.TransferService;
import org.opentripplanner.routing.algorithm.raptor.transit.TripPatternWithRaptorStopIndexes;

public class TransferIndexGenerator {
    private final TransferService transferService;
    private final Map<Trip, TripPatternWithRaptorStopIndexes> patternByTrip = new HashMap<>();

    private TransferIndexGenerator(TransferService transferService) {
        this.transferService = transferService;
    }

    public static void generateTransfers(
            TransferService transferService,
            Collection<TripPatternWithRaptorStopIndexes> tripPatterns
    ) {
        var generator = new TransferIndexGenerator(transferService);

        generator.setupPatternByTripIndex(tripPatterns);

        for (TripPatternWithRaptorStopIndexes pattern : tripPatterns) {
            generator.generateTransfers(pattern);
        }
    }

    private void setupPatternByTripIndex(Collection<TripPatternWithRaptorStopIndexes> tripPatterns) {
        for (TripPatternWithRaptorStopIndexes pattern : tripPatterns) {
            for (Trip trip : pattern.getPattern().getTrips()) {
                patternByTrip.put(trip, pattern);
            }
        }
    }

    private void generateTransfers(TripPatternWithRaptorStopIndexes pattern) {
        for (Trip trip : pattern.getPattern().getTrips()) {
            int nStops = pattern.getPattern().getStops().size();
            for (int stopPos=0; stopPos < nStops; ++stopPos) {
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
