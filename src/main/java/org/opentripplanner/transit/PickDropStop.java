package org.opentripplanner.transit;

public class PickDropStop {

    int stop;
    PickDropType pickupType;
    PickDropType dropoffType;

    public PickDropStop(int stop, int pickupType, int dropoffType) {
        this.stop = stop;
        this.pickupType = PickDropType.forGtfsCode(pickupType);
        this.dropoffType = PickDropType.forGtfsCode(dropoffType);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PickDropStop that = (PickDropStop) o;

        if (stop != that.stop) return false;
        if (dropoffType != that.dropoffType) return false;
        if (pickupType != that.pickupType) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = stop;
        result = 31 * result + (pickupType != null ? pickupType.hashCode() : 0);
        result = 31 * result + (dropoffType != null ? dropoffType.hashCode() : 0);
        return result;
    }

}
