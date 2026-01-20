package org.opentripplanner.framework.graphql;

import graphql.relay.Connection;

/**
 * This interface extends the standard GraphQL Connection interface with a method to get the total
 * number of items available in the underlying collection.
 */
public interface CountedConnection<T> extends Connection<T> {
  /**
   * The total number of items available in the underlying collection
   */
  int getTotalCount();
}
