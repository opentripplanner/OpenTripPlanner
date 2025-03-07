package org.opentripplanner.netex.mapping.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.netex.NetexTestDataSupport.createDatedServiceJourney;
import static org.opentripplanner.netex.NetexTestDataSupport.createOperatingDay;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.opentripplanner.netex.index.hierarchy.HierarchicalMapById;
import org.rutebanken.netex.model.DatedServiceJourney;
import org.rutebanken.netex.model.OperatingDay;
import org.rutebanken.netex.model.ServiceAlterationEnumeration;

public class DatedServiceJourneyMapperTest {

  private static final LocalDate LD1 = LocalDate.of(2020, 11, 1);
  private static final LocalDate LD2 = LocalDate.of(2020, 11, 2);
  private static final LocalDate LD3 = LocalDate.of(2020, 11, 3);

  private static final String OP_DAY_1 = "OD-1";
  private static final String OP_DAY_2 = "OD-2";
  private static final String OP_DAY_3 = "OD-3";

  private static final String SJ_1 = "SJ-1";

  @Test
  public void mapToServiceDatesForEmptyList() {
    assertEquals(List.of(), DatedServiceJourneyMapper.mapToServiceDates(List.of(), null));
  }

  @Test
  public void mapToServiceDates() {
    // Given
    Collection<DatedServiceJourney> dsjList = new ArrayList<>();
    HierarchicalMapById<OperatingDay> opDaysById = new HierarchicalMapById<>();

    dsjList.add(createDatedServiceJourney("ID-A", OP_DAY_1, SJ_1));

    // Date is filtered by ServiceAlteration
    dsjList.add(
      createDatedServiceJourney("ID-C", OP_DAY_3, SJ_1).withServiceAlteration(
        ServiceAlterationEnumeration.CANCELLATION
      )
    );
    dsjList.add(
      createDatedServiceJourney("ID-B", OP_DAY_2, SJ_1).withServiceAlteration(
        ServiceAlterationEnumeration.EXTRA_JOURNEY
      )
    );

    opDaysById.add(createOperatingDay(OP_DAY_1, LD1));
    opDaysById.add(createOperatingDay(OP_DAY_2, LD2));
    opDaysById.add(createOperatingDay(OP_DAY_3, LD3));

    // When
    Collection<LocalDate> result = DatedServiceJourneyMapper.mapToServiceDates(dsjList, opDaysById);

    // Then
    assertEquals(List.of(LD1, LD2), sort(result));
  }

  private <T> List<T> sort(Collection<T> input) {
    return input.stream().sorted().collect(Collectors.toList());
  }
}
