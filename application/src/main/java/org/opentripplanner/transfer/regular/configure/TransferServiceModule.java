package org.opentripplanner.transfer.regular.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import org.opentripplanner.transfer.regular.FlexTransferService;
import org.opentripplanner.transfer.regular.TransferRepository;
import org.opentripplanner.transfer.regular.internal.DefaultFlexTransferService;

@Module
public class TransferServiceModule {

  @Provides
  @Singleton
  public FlexTransferService provideTransferService(TransferRepository transferRepository) {
    return new DefaultFlexTransferService(transferRepository);
  }
}
