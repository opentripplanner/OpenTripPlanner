package org.opentripplanner.transfer.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import org.opentripplanner.ext.flex.FlexTransferIndex;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.transfer.TransferRepository;
import org.opentripplanner.transfer.internal.DefaultTransferRepository;
import org.opentripplanner.transfer.internal.TransferIndex;

@Module
public class TransferRepositoryModule {

  @Provides
  @Singleton
  public TransferRepository provideTransferRepository() {
    TransferIndex index;
    if (OTPFeature.FlexRouting.isOn()) {
      index = new FlexTransferIndex();
    } else {
      index = new TransferIndex();
    }
    return new DefaultTransferRepository(index);
  }
}
