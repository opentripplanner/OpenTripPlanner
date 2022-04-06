package org.opentripplanner.netex.mapping.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.netex.NetexTestDataSupport.createDatedServiceJourney;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.opentripplanner.netex.index.hierarchy.HierarchicalMapById;
import org.rutebanken.netex.model.DatedServiceJourney;
import org.rutebanken.netex.model.EntityStructure;

class NetexMapperIndexesTest {

  private static final String OP_DAY_1 = "OD-1";
  private static final String OP_DAY_2 = "OD-2";

  private static final String SJ_1 = "SJ-1";
  private static final String SJ_2 = "SJ-2";

  @Test
  public void indexDSJBySJIdWithEmptyInput() {
    assertEquals(
      ArrayListMultimap.create(),
      NetexMapperIndexes.indexDSJBySJId(new HierarchicalMapById<>())
    );
  }

  @Test
  public void indexDSJBySJId() {
    DatedServiceJourney dsj1 = createDatedServiceJourney("DSJ-1", OP_DAY_1, SJ_1);
    DatedServiceJourney dsj2 = createDatedServiceJourney("DSJ-2", OP_DAY_1, SJ_2);
    DatedServiceJourney dsj3 = createDatedServiceJourney("DSJ-3", OP_DAY_2, SJ_1);

    HierarchicalMapById<DatedServiceJourney> input = new HierarchicalMapById<>();
    input.addAll(List.of(dsj1, dsj2, dsj3));

    Multimap<String, DatedServiceJourney> result = NetexMapperIndexes.indexDSJBySJId(input);

    assertEquals("DSJ-1, DSJ-3", listIdsSortedAsStr(result.get(SJ_1)));
    assertEquals("DSJ-2", listIdsSortedAsStr(result.get(SJ_2)));
    assertEquals(Set.of(SJ_1, SJ_2), result.keySet());
  }

  private static String listIdsSortedAsStr(Collection<DatedServiceJourney> list) {
    return list.stream().map(EntityStructure::getId).sorted().collect(Collectors.joining(", "));
  }

  private <T> List<T> sort(Collection<T> input) {
    return input.stream().sorted().collect(Collectors.toList());
  }
}
