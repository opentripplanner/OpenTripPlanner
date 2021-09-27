package org.opentripplanner.routing.algorithm.transferoptimization.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.raptor._data.RaptorTestConstants;
import org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase;

class OptimizedPathTest implements RaptorTestConstants {

    @Test
    void copyBasicPath() {
        // Given: a wrapped basic path
        var path = new OptimizedPath<>(BasicPathTestCase.basicTripAsPath());

        // Verify all costs
        assertEquals(BasicPathTestCase.TOTAL_COST, path.generalizedCost());
        assertEquals(0, path.breakTieCost());
        assertEquals(0, path.waitTimeOptimizedCost());
        assertEquals(0, path.transferPriorityCost());

        // And toString is the same (transfer priority cost added)
        assertEquals(
                BasicPathTestCase.BASIC_PATH_AS_STRING,
                path.toString(this::stopIndexToName)
        );

        // Verify details
        assertEquals(
                BasicPathTestCase.BASIC_PATH_AS_DETAILED_STRING,
                path.toStringDetailed(this::stopIndexToName)
        );

        // Make sure the toString do not throw an exception or return null
        assertNotNull(path.toString());
    }


    @Test
    void copyBasicPathWithCostsAndVerifyCosts() {
        var orgPath = BasicPathTestCase.basicTripAsPath();
        var accessLeg = orgPath.accessLeg();

        // Define som constants
        final int generalizedCost = 881100;
        final int transferPriorityCost = 120100;
        final int waitTimeOptimizedCost = 130200;
        final int breakTieCost = 140300;

        var path = new OptimizedPath<>(
                accessLeg,
                orgPath.rangeRaptorIterationDepartureTime(),
                generalizedCost,
                transferPriorityCost,
                waitTimeOptimizedCost,
                breakTieCost
        );

        assertEquals(generalizedCost, path.generalizedCost());
        assertEquals(breakTieCost, path.breakTieCost());
        assertEquals(waitTimeOptimizedCost, path.waitTimeOptimizedCost());
        assertEquals(transferPriorityCost, path.transferPriorityCost());

        var exp = BasicPathTestCase.BASIC_PATH_AS_STRING
                .replace("$8184]", "$8811 $1201pri $1302wtc]");

        assertEquals(exp, path.toString(this::stopIndexToName));
    }
}