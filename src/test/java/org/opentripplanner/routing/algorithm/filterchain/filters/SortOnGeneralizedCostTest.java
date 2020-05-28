package org.opentripplanner.routing.algorithm.filterchain.filters;

import org.junit.Test;
import org.opentripplanner.model.plan.Itinerary;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.opentripplanner.model.plan.Itinerary.toStr;
import static org.opentripplanner.model.plan.TestItineraryBuilder.A;
import static org.opentripplanner.model.plan.TestItineraryBuilder.B;
import static org.opentripplanner.model.plan.TestItineraryBuilder.E;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

public class SortOnGeneralizedCostTest {

    public static final int TRANSFER_COST = 300;
    public static final double A_DISTANCE = 3_000d;

    @Test
    public void sortOnCostWorksWithOneOrEmptyList() {
        // Expect sort to no fail on an empty list
        assertEquals(List.of(), new SortOnGeneralizedCost(TRANSFER_COST).filter(List.of()));


        // Given a list with one itinerary
        List<Itinerary> list = List.of(newItinerary(A).bus(31, 0, 30, E).build());

        // Then: expect nothing to happen to it
        assertEquals(toStr(list), toStr(new SortOnGeneralizedCost(TRANSFER_COST).filter(list)));
    }

    @Test
    public void sortOnCost() {
        List<Itinerary> result;

        // Given: a walk(50m), bus(30m) and rail(20m) alternatives without generalizedCost or transfers
        Itinerary walk = newItinerary(A, 0).walk(50, E).build();
        Itinerary bus = newItinerary(A).bus(21, 0, 30, E).build();
        Itinerary rail = newItinerary(A).rail(110, 0, 20, E).build();

        // Add some cost - we prioritize walking
        walk.generalizedCost = 600;
        bus.generalizedCost = 600 + 2;
        rail.generalizedCost = 600 + 1;

        // When: sorting
        result = new SortOnGeneralizedCost(TRANSFER_COST).filter(List.of(walk, bus, rail));

        // Then: expect rail(1/3 of walk time), bus(2/3 of walk time) and walk
        assertEquals(Itinerary.toStr(List.of(walk, rail, bus)), Itinerary.toStr(result));
    }

    @Test
    public void sortOnCostAndNumOfTransfers() {
        List<Itinerary> result;

        // Given: a walk and bus with the same q = duration + C * Transfers (transfer cost is 5 min)
        Itinerary walk = newItinerary(A, 0).walk(50, E).build();
        Itinerary bus = newItinerary(A)
            . bus(21, 0, 10, B)
            .bus(31, 30, 45, E)
            .build();

        // Add some cost - we prioritize bus with the lowest cost
        walk.generalizedCost = 300 + 1;
        bus.generalizedCost = 0;

        // When: sorting
        result = new SortOnGeneralizedCost(TRANSFER_COST).filter(List.of(walk, bus));

        // Then: expect bus to be better than walking
        assertEquals(toStr(List.of(bus, walk)), toStr(result));

        // Add some cost - we prioritize walking with the lowest cost
        walk.generalizedCost = 300;
        bus.generalizedCost = 1;

        // When: sorting
        result = new SortOnGeneralizedCost(TRANSFER_COST).filter(List.of(walk, bus));

        // Then: expect walking to be better than bus
        assertEquals(toStr(List.of(walk, bus)), toStr(result));
    }
}