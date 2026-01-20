package org.opentripplanner.framework.graphql;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.apis.support.graphql.DataFetchingSupport;

class SimpleCountedListConnectionTest {

  @Test
  void testWithEmptyList() {
    var environment = getEnvironment(Map.of());
    var values = List.of();
    var connection = new SimpleCountedListConnection<>(values).get(environment);
    assertEquals(0, connection.getTotalCount());
    assertThat(connection.getEdges()).isEmpty();
    var pageInfo = connection.getPageInfo();
    assertNull(pageInfo.getStartCursor());
    assertNull(pageInfo.getEndCursor());
    assertFalse(pageInfo.isHasNextPage());
    assertFalse(pageInfo.isHasPreviousPage());
  }

  @Test
  void testWithContents() {
    Map<String, Object> arguments = Map.of("first", 2);
    var environment = getEnvironment(arguments);
    var values = List.of("A", "B", "C");
    var connection = new SimpleCountedListConnection<>(values).get(environment);
    assertEquals(3, connection.getTotalCount());
    assertThat(connection.getEdges()).hasSize(2);
    var pageInfo = connection.getPageInfo();
    assertEquals("c2ltcGxlLWN1cnNvcjA=", pageInfo.getStartCursor().getValue());
    assertEquals("c2ltcGxlLWN1cnNvcjE=", pageInfo.getEndCursor().getValue());
    assertTrue(pageInfo.isHasNextPage());
    assertFalse(pageInfo.isHasPreviousPage());
  }

  private DataFetchingEnvironment getEnvironment(Map<String, Object> arguments) {
    var executionContext = DataFetchingSupport.executionContext();
    return DataFetchingEnvironmentImpl.newDataFetchingEnvironment(executionContext)
      .arguments(arguments)
      .build();
  }
}
