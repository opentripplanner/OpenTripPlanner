package org.opentripplanner.framework.transaction.configure;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import org.opentripplanner.framework.transaction.RepositoryRegistry;
import org.opentripplanner.framework.transaction.TimetableSnapshotParameters;
import org.opentripplanner.framework.transaction.UpdateManager;
import org.opentripplanner.framework.transaction.internal.TransactionFactory;

@Module
public abstract class TransactionModule {

  @Provides
  @Singleton
  public static RepositoryRegistry repositoryRegistry() {
    return TransactionFactory.createRepositoryRegistry();
  }

  @Provides
  @Singleton
  public static UpdateManager updateManager(
    RepositoryRegistry repositoryRegistry,
    TimetableSnapshotParameters timetableSnapshotParameters
  ) {
    var threadFactory = new ThreadFactoryBuilder().setNameFormat("autoCommit").build();
    return TransactionFactory.createUpdateManagerWithPeriodicCommits(
      "",
      repositoryRegistry,
      threadFactory,
      timetableSnapshotParameters.maxSnapshotFrequency()
    );
  }
}
