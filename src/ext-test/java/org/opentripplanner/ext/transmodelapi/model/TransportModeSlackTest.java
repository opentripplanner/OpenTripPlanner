package org.opentripplanner.ext.transmodelapi.model;

import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.opentripplanner.model.TransitMode;

public class TransportModeSlackTest {

    @Test
    public void mapToApiList() {
        // Given
        Map<TransitMode, Integer> domain = Map.of(
                TransitMode.FUNICULAR, 600,
                TransitMode.CABLE_CAR, 600,
                TransitMode.RAIL, 1800,
                TransitMode.AIRPLANE, 3600
        );

        // When
        List<TransportModeSlack> result = TransportModeSlack.mapToApiList(domain);

        Assert.assertEquals(600, result.get(0).slack);
        Assert.assertTrue(result.get(0).modes.contains(TransitMode.CABLE_CAR));
        Assert.assertTrue(result.get(0).modes.contains(TransitMode.FUNICULAR));

        Assert.assertEquals(1800, result.get(1).slack);
        Assert.assertTrue(result.get(1).modes.contains(TransitMode.RAIL));

        Assert.assertEquals(3600, result.get(2).slack);
        Assert.assertTrue(result.get(2).modes.contains(TransitMode.AIRPLANE));
    }

    @Test
    public void mapToDomain() {
        // Given
        List<Object> apiSlackInput = List.of(
                Map.of(
                        "slack", 600,
                        "modes", List.of(TransitMode.FUNICULAR, TransitMode.CABLE_CAR)
                ),
                Map.of(
                        "slack", 1800,
                        "modes", List.of(TransitMode.RAIL)
                ),
                Map.of(
                        "slack", 3600,
                        "modes", List.of(TransitMode.AIRPLANE)
                )
        );


        Map<TransitMode, Integer> result;

        // When
        result = TransportModeSlack.mapToDomain(apiSlackInput);

        // Then
        Assert.assertNull(result.get(TransitMode.BUS)  );
        Assert.assertEquals(Integer.valueOf(600), result.get(TransitMode.FUNICULAR));
        Assert.assertEquals(Integer.valueOf(600), result.get(TransitMode.CABLE_CAR));
        Assert.assertEquals(Integer.valueOf(1800), result.get(TransitMode.RAIL));
        Assert.assertEquals(Integer.valueOf(3600), result.get(TransitMode.AIRPLANE));
    }
}