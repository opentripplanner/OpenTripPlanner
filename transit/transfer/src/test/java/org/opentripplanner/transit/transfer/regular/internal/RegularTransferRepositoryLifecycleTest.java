package org.opentripplanner.transit.transfer.regular.internal;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.transit.transfer.regular.parameters.TransferProfileType.WALK;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.framework.transaction.internal.TransactionFactory;
import org.opentripplanner.core.model.transaction.RepositoryHandle;
import org.opentripplanner.core.model.transaction.RepositoryRegistry;
import org.opentripplanner.core.model.transaction.UpdateManager;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepository;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepositorySnapshot;

/**
 * Exercise the lifecycle through the real transaction framework: a write is visible only to
 * requests started after the commit, and a failed write is rolled back.
 */
class RegularTransferRepositoryLifecycleTest {

  private static final int A = 0;
  private static final int B = 1;
  private static final int C = 2;

  private static final String PATH_AB = "A-B";
  private static final String PATH_AC = "A-C";
  private static final String PATH_BC = "B-C";

  private DefaultRegularTransferBuildRepository<String> buildRepository;
  private RepositoryRegistry registry;
  private UpdateManager updateManager;
  private RepositoryHandle<
    RegularTransferRepositorySnapshot<String>,
    RegularTransferRepository<String>
  > handle;

  @BeforeEach
  void setUp() {
    buildRepository = new DefaultRegularTransferBuildRepository<>();
    buildRepository.setPaths(WALK, A, Map.of(B, PATH_AB));

    var lifecycle = new RegularTransferRepositoryLifecycle<String>();
    registry = TransactionFactory.createRepositoryRegistry();
    handle = registry.registerRepositorySnapshot(
      buildRepository.createInitialSnapshot(),
      lifecycle
    );
    updateManager = TransactionFactory.createUpdateManagerWithAtomicCommits(
      "test",
      registry,
      Thread.ofPlatform().factory()
    );
  }

  @AfterEach
  void tearDown() {
    updateManager.shutdown();
  }

  @Test
  void initialSnapshotContainsTheGraphBuildPaths() {
    assertThat(snapshot().findPath(WALK, A, B)).isEqualTo(PATH_AB);
  }

  @Test
  void writeIsVisibleOnlyToScopesCreatedAfterTheCommit() throws Exception {
    var before = snapshot();

    updateManager.submit(ctx -> ctx.repository(handle).setPath(WALK, A, C, PATH_AC)).get();

    var after = snapshot();
    assertThat(before.findPath(WALK, A, C)).isNull();
    assertThat(after.findPath(WALK, A, C)).isEqualTo(PATH_AC);
    assertThat(after.findPath(WALK, A, B)).isEqualTo(PATH_AB);
  }

  @Test
  void writesAreNotVisibleInTheBuildRepository() throws Exception {
    updateManager.submit(ctx -> ctx.repository(handle).setPath(WALK, A, C, PATH_AC)).get();

    assertThat(buildRepository.pathsFor(WALK)).hasSize(1);
    assertThat(buildRepository.calculateNumberOfTransferPaths()).isEqualTo(1);
  }

  @Test
  void failedWriteIsRolledBack() throws Exception {
    var failed = updateManager.submit(ctx -> {
      ctx.repository(handle).setPath(WALK, A, C, PATH_AC);
      throw new IllegalStateException("Expected failure");
    });
    assertThrows(ExecutionException.class, failed::get);

    // The next transaction starts from the last committed snapshot, not the failed write.
    updateManager.submit(ctx -> ctx.repository(handle).setPath(WALK, B, C, PATH_BC)).get();

    var snapshot = snapshot();
    assertThat(snapshot.findPath(WALK, A, C)).isNull();
    assertThat(snapshot.findPath(WALK, B, C)).isEqualTo(PATH_BC);
  }

  private RegularTransferRepositorySnapshot<String> snapshot() {
    return handle.repositorySnapshot(registry.scope());
  }
}
