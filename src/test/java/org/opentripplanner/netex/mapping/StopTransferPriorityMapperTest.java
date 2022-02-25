package org.opentripplanner.netex.mapping;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.opentripplanner.model.StopTransferPriority;
import org.rutebanken.netex.model.InterchangeWeightingEnumeration;

public class StopTransferPriorityMapperTest {

  @SuppressWarnings("ConstantConditions")
  @Test
  public void mapToDomain() {

    assertNull(StopTransferPriorityMapper.mapToDomain(null));

    assertEquals(
        StopTransferPriority.DISCOURAGED,
        StopTransferPriorityMapper.mapToDomain(InterchangeWeightingEnumeration.NO_INTERCHANGE)
    );
    assertEquals(
        StopTransferPriority.ALLOWED,
        StopTransferPriorityMapper.mapToDomain(InterchangeWeightingEnumeration.INTERCHANGE_ALLOWED)
    );
    assertEquals(
        StopTransferPriority.PREFERRED,
        StopTransferPriorityMapper.mapToDomain(InterchangeWeightingEnumeration.PREFERRED_INTERCHANGE)
    );
    assertEquals(
        StopTransferPriority.RECOMMENDED,
        StopTransferPriorityMapper.mapToDomain(InterchangeWeightingEnumeration.RECOMMENDED_INTERCHANGE)
    );
  }
}