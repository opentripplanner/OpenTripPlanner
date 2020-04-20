package org.opentripplanner.netex.loader.mapping;

import org.junit.Test;
import org.opentripplanner.model.TransferCostPriority;
import org.rutebanken.netex.model.InterchangeWeightingEnumeration;

import static org.junit.Assert.*;

public class TransferCostPriorityMapperTest {

  @SuppressWarnings("ConstantConditions")
  @Test
  public void mapToDomain() {

    assertNull(TransferCostPriorityMapper.mapToDomain(null));

    assertEquals(
        TransferCostPriority.FORBIDDEN,
        TransferCostPriorityMapper.mapToDomain(InterchangeWeightingEnumeration.NO_INTERCHANGE)
    );
    assertEquals(
        TransferCostPriority.ALLOWED,
        TransferCostPriorityMapper.mapToDomain(InterchangeWeightingEnumeration.INTERCHANGE_ALLOWED)
    );
    assertEquals(
        TransferCostPriority.PREFERRED,
        TransferCostPriorityMapper.mapToDomain(InterchangeWeightingEnumeration.PREFERRED_INTERCHANGE)
    );
    assertEquals(
        TransferCostPriority.RECOMMENDED,
        TransferCostPriorityMapper.mapToDomain(InterchangeWeightingEnumeration.RECOMMENDED_INTERCHANGE)
    );
  }
}