package org.opentripplanner.transfer.regular;

import org.opentripplanner.ext.flex.FlexTransferIndex;
import org.opentripplanner.transfer.regular.internal.DefaultFlexTransferService;
import org.opentripplanner.transfer.regular.internal.DefaultTransferRepository;
import org.opentripplanner.transfer.regular.internal.TransferIndex;

/**
 * <pre>
 * TODO Refactor this into a builder so that one instance hold ONE repository and the
 *      defaultTransferService() and defaultTransferRepository() uses the same instance.
 * </pre>
 */
public class TransferServiceTestFactory {

  public static FlexTransferService defaultTransferService() {
    return new DefaultFlexTransferService(defaultTransferRepository());
  }

  public static FlexTransferService transferService(TransferRepository transferRepository) {
    return new DefaultFlexTransferService(transferRepository);
  }

  public static TransferRepository defaultTransferRepository() {
    return new DefaultTransferRepository(new TransferIndex());
  }

  public static TransferRepository withFlex() {
    return new DefaultTransferRepository(new FlexTransferIndex());
  }
}
