package org.opentripplanner.raptor.data.transfers.regular.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import org.opentripplanner.place.api.NearbyStop;
import org.opentripplanner.transit.transfer.regular.RegularTransferBuildRepository;
import org.opentripplanner.transit.transfer.regular.internal.DefaultRegularTransferBuildRepository;

/**
 * Mirrors {@code TransferRepositoryModule}: an empty singleton, populated in place by
 * {@code RegularTransitTransferGenerator} during graph build (or already populated, if deserialized
 * from a saved graph - see {@code SerializedGraphObject.regularTransferBuildRepository}). At runtime
 * it is converted into the repository, see {@code RegularTransferServiceModule}.
 */
@Module
public class RegularTransferBuildRepositoryModule {

  @Provides
  @Singleton
  public RegularTransferBuildRepository<NearbyStop> provideRegularTransferBuildRepository() {
    return new DefaultRegularTransferBuildRepository<>();
  }
}
