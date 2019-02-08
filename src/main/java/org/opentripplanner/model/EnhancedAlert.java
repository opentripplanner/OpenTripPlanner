package org.opentripplanner.model;

import org.opentripplanner.routing.alertpatch.AlertPatch;
import java.util.List;

public class EnhancedAlert {
    public enum AffectedActivity {
        BOARD,
        EXIT,
        RIDE,
        USING_ESCALATOR,
        BRINGING_BIKE,
        PARK_CAR,
        STORE_BIKE,
        USING_WHEELCHAIR
    }

    private FeedScopedId route;
    private FeedScopedId stop;
    private FeedScopedId trip;
    private List<AffectedActivity> activities;

    public FeedScopedId getRoute() {
        return route;
    }

    public void setRoute(FeedScopedId route) {
        this.route = route;
    }

    public FeedScopedId getStop() {
        return stop;
    }

    public void setStop(FeedScopedId stop) {
        this.stop = stop;
    }

    public FeedScopedId getTrip() {
        return trip;
    }

    public void setTrip(FeedScopedId trip) {
        this.trip = trip;
    }

    public List<AffectedActivity> getActivities() {
        return activities;
    }

    public void setActivities(List<AffectedActivity> activities) {
        this.activities = activities;
    }

    public EnhancedAlert() {}

    public boolean appliesTo(AlertPatch alertPatch) {
        if (route != null && !route.equals(alertPatch.getRoute())) {
            return false;
        } else if (stop != null && !stop.equals(alertPatch.getStop())) {
            return false;
        } else if (trip != null && !trip.equals(alertPatch.getTrip())) {
            return false;
        }

        return true;
    }

    public boolean cannotBoard() {
        return activities.contains(AffectedActivity.BOARD);
    }

    public boolean cannotAlight() {
        return activities.contains(AffectedActivity.EXIT);
    }

    public boolean cannotRideThrough() {
        return activities.contains(AffectedActivity.RIDE);
    }
}

