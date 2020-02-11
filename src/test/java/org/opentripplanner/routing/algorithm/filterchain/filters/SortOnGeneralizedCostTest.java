package org.opentripplanner.routing.algorithm.filterchain.filters;

import org.junit.Test;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.core.TraverseMode;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.opentripplanner.routing.algorithm.filterchain.FilterChainTestData.A;
import static org.opentripplanner.routing.algorithm.filterchain.FilterChainTestData.B;
import static org.opentripplanner.routing.algorithm.filterchain.FilterChainTestData.E;
import static org.opentripplanner.routing.algorithm.filterchain.FilterChainTestData.itinerary;
import static org.opentripplanner.routing.algorithm.filterchain.FilterChainTestData.leg;
import static org.opentripplanner.routing.algorithm.filterchain.FilterChainTestData.toStr;

public class SortOnGeneralizedCostTest {

    public static final int TRANSFER_COST = 300;
    public static final double A_DISTANCE = 3_000d;

    @Test
    public void sortOnCost() {
        List<Itinerary> result;

        // Given: a walk(50m), bus(30m) and rail(20m) alternatives without generalizedCost or transfers
        Itinerary walk = itinerary(leg(A, E, 0, 50, A_DISTANCE, TraverseMode.WALK));
        Itinerary bus = itinerary(leg(A, E, 0, 30, A_DISTANCE, TraverseMode.BUS));
        Itinerary rail = itinerary(leg(A, E, 0, 20, A_DISTANCE, TraverseMode.RAIL));

        // Add some cost - we prioritize walking
        walk.generalizedCost = 600;
        bus.generalizedCost = 600 + 2;
        rail.generalizedCost = 600 + 1;

        // When: sorting
        result = new SortOnGeneralizedCost(TRANSFER_COST).filter(List.of(walk, bus, rail));

        // Then: expect rail(1/3 of walk time), bus(2/3 of walk time) and walk
        assertEquals(toStr(result), toStr(List.of(walk, rail, bus)), toStr(result));
    }

    @Test
    public void sortOnCostAndNumOfTransfers() {
        List<Itinerary> result;

        // Given: a walk and bus with the same q = duration + C * Transfers (transfer cost is 5 min)
        Itinerary walk = itinerary(leg(A, E, 0, 50, 3_000d, TraverseMode.WALK));
        Itinerary bus = itinerary(
                leg(A, B, 0, 10, A_DISTANCE, TraverseMode.BUS),
                leg(B, E, 30, 45, A_DISTANCE, TraverseMode.BUS)
        );

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