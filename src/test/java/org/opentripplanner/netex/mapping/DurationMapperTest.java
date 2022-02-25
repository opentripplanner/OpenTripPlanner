package org.opentripplanner.netex.mapping;

import org.junit.Test;

import java.time.Duration;

import static org.junit.Assert.assertEquals;

public class DurationMapperTest {

    @Test
    public void mapDurationToSec() {
        assertEquals(10, DurationMapper.mapDurationToSec(Duration.ofSeconds(10), -1));
        assertEquals(-1, DurationMapper.mapDurationToSec(null, -1));
    }
}