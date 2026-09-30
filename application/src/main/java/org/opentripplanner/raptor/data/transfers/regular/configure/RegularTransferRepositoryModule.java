package org.opentripplanner.raptor.data.transfers.regular.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import org.opentripplanner.place.api.NearbyStop;
import org.opentripplanner.transit.transfer.regular.internal.RegularTransferRepository;

/**
 * Mirrors {@code TransferRepositoryModule}: an empty singleton, populated in place by
 * {@code RaptorDataTransferGenerator} during graph build (or already populated, if deserialized
 * from a saved graph - see {@code SerializedGraphObject.regularTransferRepository}).
 */
@Module
public class RegularTransferRepositoryModule {

  @Provides
  @Singleton
  public RegularTransferRepository<NearbyStop> provideRegularTransferRepository() {
    return new RegularTransferRepository<>();
  }
}
