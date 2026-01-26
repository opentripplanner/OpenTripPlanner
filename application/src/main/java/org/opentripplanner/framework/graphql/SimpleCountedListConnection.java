package org.opentripplanner.framework.graphql;

import graphql.TrivialDataFetcher;
import graphql.relay.SimpleListConnection;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.List;
import java.util.Objects;

/**
 * This class is a version of the {@link SimpleListConnection} that returns a
 * {@link CountedConnection}.
 */
public class SimpleCountedListConnection<T>
  implements DataFetcher<CountedConnection<T>>, TrivialDataFetcher<CountedConnection<T>> {

  private final List<T> data;

  public SimpleCountedListConnection(List<T> data) {
    this.data = Objects.requireNonNull(data);
  }

  @Override
  public CountedConnection<T> get(DataFetchingEnvironment environment) {
    var simpleListConnection = new SimpleListConnection<>(data);
    var connection = simpleListConnection.get(environment);
    return new DefaultCountedConnection<>(
      connection.getEdges(),
      connection.getPageInfo(),
      data.size()
    );
  }
}
