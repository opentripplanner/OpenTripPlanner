package org.opentripplanner.netex.loader.mapping;

import org.junit.Test;
import org.opentripplanner.model.TransferPriority;
import org.rutebanken.netex.model.InterchangeWeightingEnumeration;

import static org.junit.Assert.*;

public class TransferPriorityMapperTest {

  @SuppressWarnings("ConstantConditions")
  @Test
  public void mapToDomain() {

    assertNull(TransferPriorityMapper.mapToDomain(null));

    assertEquals(
        TransferPriority.DISCOURAGED,
        TransferPriorityMapper.mapToDomain(InterchangeWeightingEnumeration.NO_INTERCHANGE)
    );
    assertEquals(
        TransferPriority.ALLOWED,
        TransferPriorityMapper.mapToDomain(InterchangeWeightingEnumeration.INTERCHANGE_ALLOWED)
    );
    assertEquals(
        TransferPriority.PREFERRED,
        TransferPriorityMapper.mapToDomain(InterchangeWeightingEnumeration.PREFERRED_INTERCHANGE)
    );
    assertEquals(
        TransferPriority.RECOMMENDED,
        TransferPriorityMapper.mapToDomain(InterchangeWeightingEnumeration.RECOMMENDED_INTERCHANGE)
    );
  }
}