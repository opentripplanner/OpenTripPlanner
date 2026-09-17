package org.opentripplanner.framework.transaction.moduletest.candyshop.base;

import java.util.Collection;

/**
 * This interface is used to make a generic test. Both repository snapshots implement it, and
 * the test has a generic assertion which then works on both repositories. It is unnecessary
 * to implement the spi of the transaction framework.
 */
public interface EntityIdProvider {
  Collection<Integer> listIds();
}
