package org.opentripplanner.ext.transmodelapi.model;

import org.junit.Assert;
import org.junit.Test;
import org.opentripplanner.routing.core.TraverseMode;

import java.util.List;
import java.util.Map;

public class TransportModeSlackTest {

    @Test
    public void mapToApiList() {
        // Given
        Map<TraverseMode, Integer> domain = Map.of(
                TraverseMode.FUNICULAR, 600,
                TraverseMode.CABLE_CAR, 600,
                TraverseMode.RAIL, 1800,
                TraverseMode.AIRPLANE, 3600
        );

        // When
        List<TransportModeSlack> result = TransportModeSlack.mapToApiList(domain);

        Assert.assertEquals(600, result.get(0).slack);
        Assert.assertTrue(result.get(0).modes.contains(TraverseMode.CABLE_CAR));
        Assert.assertTrue(result.get(0).modes.contains(TraverseMode.FUNICULAR));

        Assert.assertEquals(1800, result.get(1).slack);
        Assert.assertTrue(result.get(1).modes.contains(TraverseMode.RAIL));

        Assert.assertEquals(3600, result.get(2).slack);
        Assert.assertTrue(result.get(2).modes.contains(TraverseMode.AIRPLANE));
    }

    @Test
    public void mapToDomain() {
        // Given
        List<Object> apiSlackInput = List.of(
                Map.of(
                        "slack", 600,
                        "modes", List.of(TraverseMode.FUNICULAR, TraverseMode.CABLE_CAR)
                ),
                Map.of(
                        "slack", 1800,
                        "modes", List.of(TraverseMode.RAIL)
                ),
                Map.of(
                        "slack", 3600,
                        "modes", List.of(TraverseMode.AIRPLANE)
                )
        );


        Map<TraverseMode, Integer> result;

        // When
        result = TransportModeSlack.mapToDomain(apiSlackInput);

        // Then
        Assert.assertNull(result.get(TraverseMode.BUS)  );
        Assert.assertEquals(Integer.valueOf(600), result.get(TraverseMode.FUNICULAR));
        Assert.assertEquals(Integer.valueOf(600), result.get(TraverseMode.CABLE_CAR));
        Assert.assertEquals(Integer.valueOf(1800), result.get(TraverseMode.RAIL));
        Assert.assertEquals(Integer.valueOf(3600), result.get(TraverseMode.AIRPLANE));
    }
}