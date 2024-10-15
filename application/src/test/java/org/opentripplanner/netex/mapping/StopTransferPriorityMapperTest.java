package org.opentripplanner.netex.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.site.StopTransferPriority;
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
      StopTransferPriorityMapper.mapToDomain(
        InterchangeWeightingEnumeration.RECOMMENDED_INTERCHANGE
      )
    );
  }
}
