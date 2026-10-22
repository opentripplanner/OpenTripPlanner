package org.opentripplanner.transit.transfer.regular.internal;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.framework.transaction.internal.TransactionFactory;
import org.opentripplanner.core.model.transaction.RepositoryHandle;
import org.opentripplanner.core.model.transaction.RepositoryRegistry;
import org.opentripplanner.core.model.transaction.UpdateManager;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepository;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepositorySnapshot;
import org.opentripplanner.transit.transfer.regular.api.WalkPreferences;

/**
 * Exercise the lifecycle through the real transaction framework: a write is visible only to
 * requests started after the commit, all transactions write to the same buffer, and a failed
 * write is rolled back.
 */
class RegularTransferRepositoryLifecycleTest {

  private static final WalkPreferences WALK = WalkPreferences.DEFAULT;

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

    registry = TransactionFactory.createRepositoryRegistry();
    var repository = buildRepository.createRuntimeRepository();
    handle = registry.registerRepository(repository, TestLifecycles.pathsOnly(repository));
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

    submit(repository -> repository.replaceTransfersFrom(WALK, A, Map.of(C, PATH_AC)));

    var after = snapshot();
    assertThat(before.findPath(WALK, A, C)).isNull();
    assertThat(before.findPath(WALK, A, B)).isEqualTo(PATH_AB);
    assertThat(after.findPath(WALK, A, C)).isEqualTo(PATH_AC);
    assertThat(after.findPath(WALK, A, B)).isNull();
  }

  @Test
  void allTransactionsWriteToTheSameBuffer() throws Exception {
    var first = new AtomicReference<RegularTransferRepository<String>>();
    var second = new AtomicReference<RegularTransferRepository<String>>();

    submit(repository -> {
      first.set(repository);
      repository.replaceTransfersFrom(WALK, A, Map.of(C, PATH_AC));
    });
    submit(repository -> {
      second.set(repository);
      repository.replaceTransfersFrom(WALK, B, Map.of(C, PATH_BC));
    });

    assertThat(second.get()).isSameInstanceAs(first.get());
    var snapshot = snapshot();
    assertThat(snapshot.findPath(WALK, A, C)).isEqualTo(PATH_AC);
    assertThat(snapshot.findPath(WALK, B, C)).isEqualTo(PATH_BC);
  }

  @Test
  void writesAreNotVisibleInTheBuildRepository() throws Exception {
    submit(repository -> repository.replaceTransfersFrom(WALK, A, Map.of(C, PATH_AC)));

    assertThat(buildRepository.calculateNumberOfTransferPaths(WALK)).isEqualTo(1);
  }

  @Test
  void failedWriteIsRolledBack() throws Exception {
    submit(repository -> repository.replaceTransfersFrom(WALK, A, Map.of(C, PATH_AC)));
    var failed = updateManager.submit(ctx -> {
      var repository = ctx.repository(handle);
      repository.resetTransfersFrom(WALK, A);
      repository.replaceTransfersFrom(WALK, B, Map.of(C, PATH_BC));
      throw new IllegalStateException("Expected failure");
    });
    assertThrows(ExecutionException.class, failed::get);

    // The next transaction continues from the last committed snapshot, not the failed write.
    submit(repository -> repository.replaceTransfersFrom(WALK, C, Map.of()));

    var snapshot = snapshot();
    assertThat(snapshot.findPath(WALK, A, C)).isEqualTo(PATH_AC);
    assertThat(snapshot.findPath(WALK, A, B)).isNull();
    assertThat(snapshot.findPath(WALK, B, C)).isNull();
  }

  @Test
  void onlyTheBufferOfTheLifecycleCanBeFrozen() {
    var other = new DefaultRegularTransferBuildRepository<String>().createRuntimeRepository();
    var lifecycle = TestLifecycles.pathsOnly(buildRepositoryWithPaths().createRuntimeRepository());

    assertThrows(IllegalArgumentException.class, () -> lifecycle.freeze(other));
  }

  private static DefaultRegularTransferBuildRepository<String> buildRepositoryWithPaths() {
    var buildRepository = new DefaultRegularTransferBuildRepository<String>();
    buildRepository.setPaths(WALK, A, Map.of(B, PATH_AB));
    return buildRepository;
  }

  private void submit(Consumer<RegularTransferRepository<String>> write) throws Exception {
    updateManager.submit(ctx -> write.accept(ctx.repository(handle))).get();
  }

  private RegularTransferRepositorySnapshot<String> snapshot() {
    return handle.repositorySnapshot(registry.scope());
  }
}
