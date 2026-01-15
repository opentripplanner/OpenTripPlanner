package org.opentripplanner.transfer.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import org.opentripplanner.transfer.TransferRepository;
import org.opentripplanner.transfer.TransferService;
import org.opentripplanner.transfer.internal.DefaultTransferService;

@Module
public class TransferServiceModule {

  @Provides
  @Singleton
  public TransferService provideTransferService(TransferRepository transferRepository) {
    return new DefaultTransferService(transferRepository);
  }
}
