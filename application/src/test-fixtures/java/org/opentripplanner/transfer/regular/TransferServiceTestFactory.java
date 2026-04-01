package org.opentripplanner.transfer.regular;

import org.opentripplanner.ext.flex.FlexTransferIndex;
import org.opentripplanner.transfer.regular.internal.DefaultTransferRepository;
import org.opentripplanner.transfer.regular.internal.DefaultTransferService;
import org.opentripplanner.transfer.regular.internal.TransferIndex;

public class TransferServiceTestFactory {

  public static RegularTransferService defaultTransferService() {
    return new DefaultTransferService(defaultTransferRepository());
  }

  public static RegularTransferService transferService(TransferRepository transferRepository) {
    return new DefaultTransferService(transferRepository);
  }

  public static TransferRepository defaultTransferRepository() {
    return new DefaultTransferRepository(new TransferIndex());
  }

  public static TransferRepository withFlex() {
    return new DefaultTransferRepository(new FlexTransferIndex());
  }
}
