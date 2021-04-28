package org.opentripplanner.routing.algorithm.filterchain.filters;

import org.junit.Test;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.PlanTestConstants;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

public class OtpDefaultSortOrderTest implements PlanTestConstants {

    private List<Itinerary> result;


    @Test
    public void sortStreetBeforeTransitThenTime() {
        Itinerary walk = newItinerary(A, 0).walk(5, G).build();
        Itinerary bicycle = newItinerary(B).bicycle(4, 6, G).build();
        Itinerary bus  = newItinerary(C).bus(21, 1, 4, G).build();
        Itinerary rail  = newItinerary(D).rail(21, 3, 7, G).build();

        // Eliminate cost
        walk.generalizedCost = bicycle.generalizedCost = bus.generalizedCost = rail.generalizedCost = 0;

        // Depart-after-sort
        result = departAfterSort().filter(List.of(walk, bicycle, bus, rail));
        assertEquals(toStr(walk, bicycle, bus, rail), toStr(result));

        // Arrive-by-sort
        result = arriveBySort().filter(List.of(walk, bicycle, bus, rail));
        assertEquals(toStr(bicycle, walk, rail, bus), toStr(result));
    }

    @Test
    public void sortOnTime() {
        Itinerary iA = newItinerary(A).bus(21, 1, 5, G).build();
        Itinerary iB = newItinerary(B).bus(21, 0, 5, G).build();
        Itinerary iC = newItinerary(C).bus(21, 1, 6, G).build();
        Itinerary iD = newItinerary(D).bus(21, 0, 6, G).build();

        // Eliminate cost
        iA.generalizedCost = iB.generalizedCost = iC.generalizedCost = iD.generalizedCost = 0;

        // Depart-after-sort
        result = departAfterSort().filter(List.of(iD, iB, iA, iC));
        assertEquals(toStr(iA, iB, iC, iD), toStr(result));

        // Arrive-by-sort
        result = arriveBySort().filter(List.of(iB, iD, iC, iA));
        assertEquals(toStr(iA, iC, iB, iD), toStr(result));
    }

    @Test
    public void sortOnGeneralizedCostVsTime() {
        Itinerary iA = newItinerary(A).bus(21, 0, 20, G).build();
        iA.generalizedCost = 1;

        // Better on arrival-time, but worse on cost
        Itinerary iB = newItinerary(B).bus(21, 0, 10, G).build();
        iB.generalizedCost = 100;

        // Better on departure-time, but worse on cost
        Itinerary iC = newItinerary(C).bus(21, 10, 20, G).build();
        iC.generalizedCost = 100;

        // Verify depart-after sort on arrival-time, then cost
        assertEquals(toStr(iB, iA, iC), toStr(departAfterSort().filter(List.of(iB, iA, iC))));

        // Verify arrive-by sort on departure-time, then cost
        assertEquals(toStr(iC, iA, iB), toStr(arriveBySort().filter(List.of(iB, iA, iC))));
    }

    @Test
    public void sortOnGeneralizedCostVsNumberOfTransfers() {
        // Best cost, 1 transfer
        Itinerary iA = newItinerary(A)
            .bus(11, 0, 20, C)
            .bus(21, 22, 40, G)
            .build();
        iA.generalizedCost = 1;

        // Same cost, more transfers (2 transfers)
        Itinerary iB = newItinerary(B)
            .bus(11, 0, 10, C)
            .bus(21, 12, 20, D)
            .bus(31, 22, 40, G)
            .build();
        iB.generalizedCost = 1;

        // Worse on cost, better on transfers
        Itinerary iC = newItinerary(C).bus(11, 0, 40, G).build();
        iC.generalizedCost = 100;

        // Verify depart-after sort on generalized-cost, then transfers
        assertEquals(toStr(iA, iB, iC), toStr(departAfterSort().filter(List.of(iB, iC, iA))));

        // Verify arrive-by sort on generalized-cost, then transfers
        assertEquals(toStr(iA, iB, iC), toStr(arriveBySort().filter(List.of(iC, iA, iB))));
    }

    @Test
    public void sortOnTransfersVsTime() {
        Itinerary iA = newItinerary(A).bus(21, 0, 20, G).build();
        iA.generalizedCost = 1;

        // Better on arrival-time, but worse on transfers
        Itinerary iB = newItinerary(B)
            .bus(21, 0, 5, B)
            .bus(21, 7, 10, G)
            .build();
        iB.generalizedCost = 100;

        // Better on departure-time, but worse on transfers
        Itinerary iC = newItinerary(A).bus(21, 10, 20, G).build();
        iC.generalizedCost = 100;

        // Verify depart-after sort on arrival-time, then cost
        assertEquals(toStr(iB, iA, iC), toStr(departAfterSort().filter(List.of(iB, iA, iC))));

        // Verify arrive-by sort on departure-time, then cost
        assertEquals(toStr(iC, iA, iB), toStr(arriveBySort().filter(List.of(iB, iA, iC))));
    }

    private String toStr(Itinerary ... list) {
        return Itinerary.toStr(Arrays.asList(list));
    }

    private String toStr(List<Itinerary> list) {
        return Itinerary.toStr(list);
    }

    private OtpDefaultSortOrder arriveBySort() {
        return new OtpDefaultSortOrder(true);
    }

    private OtpDefaultSortOrder departAfterSort() {
        return new OtpDefaultSortOrder(false);
    }
}