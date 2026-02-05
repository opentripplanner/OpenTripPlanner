package org.opentripplanner.framework.snapshot.persistence.transfer;

public class TransferConfig {

  public static ImmutableTransferRepo provideTransferRepo() {
    return new ImmutableTransferRepo(0);
  }

}
