package org.opentripplanner.transit.raptor.rangeraptor.standard.stoparrivals.view;

import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.view.ArrivalView;
import org.opentripplanner.transit.raptor.rangeraptor.standard.stoparrivals.StopArrivalState;


/**
 * Implement the {@link ArrivalView}.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
abstract class StopArrivalViewAdapter<T extends RaptorTripSchedule> implements ArrivalView<T> {
    private final int round;
    private final int stop;

    private StopArrivalViewAdapter(int round, int stop) {
        this.round = round;
        this.stop = stop;
    }

    @Override
    public int stop() {
        return stop;
    }

    @Override
    public int round() {
        return round;
    }


    static final class Access<T extends RaptorTripSchedule> extends StopArrivalViewAdapter<T> {
        private final int departureTime;
        private final int arrivalTime;
        private final RaptorTransfer access;

        Access(int stop, int departureTime, int arrivalTime, RaptorTransfer access) {
            super(0, stop);
            this.departureTime = departureTime;
            this.arrivalTime = arrivalTime;
            this.access = access;
        }

        @Override
        public int arrivalTime() {
            return arrivalTime;
        }

        @Override
        public int departureTime() {
            return departureTime;
        }

        @Override
        public RaptorTransfer accessEgress() {
            return access;
        }

        @Override
        public boolean arrivedByAccessLeg() {
            return true;
        }

        @Override
        public ArrivalView<T> previous() {
            throw new UnsupportedOperationException("Access arrival is the first leg.");
        }
    }

    static final class Transit<T extends RaptorTripSchedule> extends StopArrivalViewAdapter<T> {
        private final StopArrivalState<T> arrival;
        private final StopsCursor<T> cursor;

        Transit(int round, int stop, StopArrivalState<T> arrival, StopsCursor<T> cursor) {
            super(round, stop);
            this.arrival = arrival;
            this.cursor = cursor;
        }

        @Override
        public int arrivalTime() {
            return arrival.transitTime();
        }

        @Override
        public int departureTime() {
            return arrival.boardTime();
        }

        @Override
        public int boardStop() {
            return arrival.boardStop();
        }

        @Override
        public T trip() {
            return arrival.trip();
        }

        @Override
        public RaptorTransfer accessEgress() {
            throw new UnsupportedOperationException("No accessEgress for transit stop arrival");
        }

        @Override
        public boolean arrivedByTransit() {
            return true;
        }

        @Override
        public ArrivalView<T> previous() {
            return round() == 1
                    ? cursor.access(boardStop(), this)
                    : cursor.stop(round()-1, boardStop());
        }
    }

    static final class Transfer<T extends RaptorTripSchedule> extends StopArrivalViewAdapter<T> {
        private final StopArrivalState<T> arrival;
        private final StopsCursor<T> cursor;

        Transfer(int round, int stop, StopArrivalState<T> arrival, StopsCursor<T> cursor) {
            super(round, stop);
            this.arrival = arrival;
            this.cursor = cursor;
        }

        @Override
        public int transferFromStop() {
            return arrival.transferFromStop();
        }

        @Override
        public int arrivalTime() {
            return arrival.time();
        }

        @Override
        public int departureTime() {
            return cursor.departureTime(arrivalTime(), arrival.transferDuration());
        }

        @Override
        public RaptorTransfer accessEgress() {
            throw new UnsupportedOperationException("No accessEgress for transfer stop arrival");
        }

        @Override
        public boolean arrivedByTransfer() {
            return true;
        }

        @Override
        public ArrivalView<T> previous() {
            return cursor.transit(round(), transferFromStop());
        }
    }
}