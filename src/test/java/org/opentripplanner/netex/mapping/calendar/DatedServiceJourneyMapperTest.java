package org.opentripplanner.netex.mapping.calendar;

import org.junit.Test;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.netex.index.hierarchy.HierarchicalMapById;
import org.rutebanken.netex.model.DatedServiceJourney;
import org.rutebanken.netex.model.EntityStructure;
import org.rutebanken.netex.model.OperatingDay;
import org.rutebanken.netex.model.ServiceAlterationEnumeration;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.opentripplanner.netex.NetexTestDataSupport.createDatedServiceJourney;
import static org.opentripplanner.netex.NetexTestDataSupport.createOperatingDay;

public class DatedServiceJourneyMapperTest {
  private static final LocalDate LD1 = LocalDate.of(2020, 11, 1);
  private static final LocalDate LD2 = LocalDate.of(2020, 11, 2);
  private static final LocalDate LD3 = LocalDate.of(2020, 11, 3);

  private final static ServiceDate SD1 = new ServiceDate(LD1);
  private final static ServiceDate SD2 = new ServiceDate(LD2);
  private final static ServiceDate SD3 = new ServiceDate(LD3);

  private static final String OP_DAY_1 = "OD-1";
  private static final String OP_DAY_2 = "OD-2";
  private static final String OP_DAY_3 = "OD-3";

  private static final String SJ_1 = "SJ-1";
  private static final String SJ_2 = "SJ-2";


  @Test
  public void indexDSJBySJIdWithEmptyInput() {
    assertEquals(Map.of(), DatedServiceJourneyMapper.indexDSJBySJId(new HierarchicalMapById<>()));
  }

  @Test
  public void indexDSJBySJId() {
    DatedServiceJourney dsj1 = createDatedServiceJourney("DSJ-1", OP_DAY_1, SJ_1);
    DatedServiceJourney dsj2 = createDatedServiceJourney("DSJ-2", OP_DAY_1, SJ_2);
    DatedServiceJourney dsj3 = createDatedServiceJourney("DSJ-3", OP_DAY_2, SJ_1);

    HierarchicalMapById<DatedServiceJourney> input = new HierarchicalMapById<>();
    input.addAll(List.of(dsj1, dsj2, dsj3));

    Map<String, List<DatedServiceJourney>> result = DatedServiceJourneyMapper.indexDSJBySJId(input);

    assertEquals("DSJ-1, DSJ-3", listIdsSortedAsStr(result.get(SJ_1)));
    assertEquals("DSJ-2", listIdsSortedAsStr(result.get(SJ_2)));
    assertEquals(Set.of(SJ_1, SJ_2), result.keySet());
  }

  @Test
  public void mapToServiceDatesForEmptyList() {
    assertEquals(List.of(), DatedServiceJourneyMapper.mapToServiceDates(List.of(), null));
  }

  @Test
  public void mapToServiceDates() {
    // Given
    Collection<DatedServiceJourney> dsjList= new ArrayList<>();
    HierarchicalMapById<OperatingDay> opDaysById = new HierarchicalMapById<>();

    dsjList.add(createDatedServiceJourney("ID-A", OP_DAY_1, SJ_1));

    // Date is filtered by ServiceAlteration
    dsjList.add(
        createDatedServiceJourney("ID-C", OP_DAY_3, SJ_1)
            .withServiceAlteration(ServiceAlterationEnumeration.CANCELLATION)
    );
    dsjList.add(
        createDatedServiceJourney("ID-B", OP_DAY_2, SJ_1)
            .withServiceAlteration(ServiceAlterationEnumeration.EXTRA_JOURNEY)
    );


    opDaysById.add(createOperatingDay(OP_DAY_1, LD1));
    opDaysById.add(createOperatingDay(OP_DAY_2, LD2));
    opDaysById.add(createOperatingDay(OP_DAY_3, LD3));

    // When
    Collection<ServiceDate> result = DatedServiceJourneyMapper.mapToServiceDates(dsjList, opDaysById);

    // Then
    assertEquals(List.of(SD1, SD2), sort(result));
  }

  private static String listIdsSortedAsStr(List<DatedServiceJourney> list) {
    return list.stream()
        .map(EntityStructure::getId)
        .sorted()
        .collect(Collectors.joining(", "));
  }

  private <T> List<T> sort(Collection<T> input) {
    return input.stream().sorted().collect(Collectors.toList());
  }
}