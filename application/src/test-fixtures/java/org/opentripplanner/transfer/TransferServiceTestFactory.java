package org.opentripplanner.transfer;

import org.opentripplanner.ext.flex.FlexTransferIndex;
import org.opentripplanner.transfer.internal.DefaultTransferRepository;
import org.opentripplanner.transfer.internal.DefaultTransferService;
import org.opentripplanner.transfer.internal.TransferIndex;

public class TransferServiceTestFactory {

  public static TransferService defaultTransferService() {
    return new DefaultTransferService(defaultTransferRepository());
  }

  public static TransferService transferService(TransferRepository transferRepository) {
    return new DefaultTransferService(transferRepository);
  }

  public static TransferRepository defaultTransferRepository() {
    return new DefaultTransferRepository(new TransferIndex());
  }

  public static TransferRepository withFlex() {
    return new DefaultTransferRepository(new FlexTransferIndex());
  }
}
