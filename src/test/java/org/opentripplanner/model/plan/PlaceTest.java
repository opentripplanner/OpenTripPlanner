package org.opentripplanner.model.plan;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.WgsCoordinate;
import org.opentripplanner.util.NonLocalizedString;

public class PlaceTest {

    @Test
    public void sameLocationBasedOnInstance() {
        Place aPlace = Place.normal(60.0, 10.0, new NonLocalizedString("A Place"));
        assertTrue("same instance", aPlace.sameLocation(aPlace));
    }

    @Test
    public void sameLocationBasedOnCoordinates() {
        Place aPlace = Place.normal(60.0, 10.0, new NonLocalizedString("A Place"));
        Place samePlace = Place.normal(60.000000000001, 10.0000000000001, new NonLocalizedString("Same Place"));
        Place otherPlace = Place.normal(65.0, 14.0, new NonLocalizedString("Other Place"));

        assertTrue("same place", aPlace.sameLocation(samePlace));
        assertTrue("same place(symmetric)", samePlace.sameLocation(aPlace));
        assertFalse("other place", aPlace.sameLocation(otherPlace));
        assertFalse("other place(symmetric)", otherPlace.sameLocation(aPlace));
    }

    @Test
    public void sameLocationBasedOnStopId() {
        var s1 = stop("1", 1.0, 1.0);
        var s2 = stop("2", 1.0, 2.0);

        Place aPlace = place(s1);
        Place samePlace = place(s1);
        Place otherPlace = place(s2);

        assertTrue("same place", aPlace.sameLocation(samePlace));
        assertTrue("same place(symmetric)", samePlace.sameLocation(aPlace));
        assertFalse("other place", aPlace.sameLocation(otherPlace));
        assertFalse("other place(symmetric)", otherPlace.sameLocation(aPlace));
    }

    @Test
    public void acceptsNullCoordinates() {
        var p = Place.normal(null, null, new NonLocalizedString("Test"));
        assertNull(p.coordinate);
    }

    private static Stop stop(String stopId, double lat, double lon) {
       return new Stop(
                new FeedScopedId("S", stopId),
                null,
                null,
                null,
                WgsCoordinate.creatOptionalCoordinate(lat, lon),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private static Place place(Stop stop) {
        return Place.forStop(stop);
    }
}