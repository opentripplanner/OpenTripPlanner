package org.opentripplanner.transfer.regular.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import org.opentripplanner.transfer.regular.RegularTransferService;
import org.opentripplanner.transfer.regular.TransferRepository;
import org.opentripplanner.transfer.regular.internal.DefaultTransferService;

@Module
public class TransferServiceModule {

  @Provides
  @Singleton
  public RegularTransferService provideTransferService(TransferRepository transferRepository) {
    return new DefaultTransferService(transferRepository);
  }
}
