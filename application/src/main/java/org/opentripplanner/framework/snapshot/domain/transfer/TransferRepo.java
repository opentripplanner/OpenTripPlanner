package org.opentripplanner.framework.snapshot.domain.transfer;

/**
 * Only save (set) and find (get) methods, NO business logic
 */
public interface TransferRepo {

  int getNumberOfRecalculations();

  void setNumberOfRecalculations(int number);
}
