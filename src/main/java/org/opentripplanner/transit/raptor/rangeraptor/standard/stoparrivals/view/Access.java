package org.opentripplanner.transit.raptor.rangeraptor.standard.stoparrivals.view;

import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.view.AccessPathView;
import org.opentripplanner.transit.raptor.api.view.ArrivalView;

final class Access<T extends RaptorTripSchedule>
    extends StopArrivalViewAdapter<T>
    implements AccessPathView
{
    private final int arrivalTime;
    private final RaptorTransfer access;

    Access(int round, int arrivalTime, RaptorTransfer access) {
        super(round, access.stop());
        this.arrivalTime = arrivalTime;
        this.access = access;
    }

    @Override
    public int arrivalTime() {
        return arrivalTime;
    }

    @Override
    public boolean arrivedByAccess() {
        return true;
    }

    @Override
    public AccessPathView accessPath() {
        return this;
    }

    @Override
    public RaptorTransfer access() {
        return access;
    }

    @Override
    public ArrivalView<T> previous() {
        throw new UnsupportedOperationException("Access path arrival is the first path.");
    }
}
