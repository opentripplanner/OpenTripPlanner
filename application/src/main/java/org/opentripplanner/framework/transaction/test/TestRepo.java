package org.opentripplanner.framework.transaction.test;

import java.util.function.Supplier;
import org.opentripplanner.framework.transaction.RepositoryTransactionManager;
import org.opentripplanner.framework.transaction.TransactionalRepository;
import org.opentripplanner.framework.transaction.internal.DefaultRepositoryTransactionManager;
import org.opentripplanner.framework.transaction.internal.DefaultTransactionalRepository;

public class TestRepo {


  public static void main(String[] args) {
    RepositoryTransactionManager repoManager = new DefaultRepositoryTransactionManager();
    TransactionalRepository<ReadOnlyTestSnapshot, MutableTestSnapshot> repo = new DefaultTransactionalRepository<>(
      new ReadOnlyTestSnapshot("start"),
      new TestSnapshotLifecycle(),
      (DefaultRepositoryTransactionManager) repoManager
    );

    var tx = repoManager.requestScopedTransaction();

    var serviceA = new ServiceRequestScope("A", repo.snapshot(tx));
    System.out.println("-------------------------------------------------- [ service A created ]");
    serviceA.doWork();
    System.out.println("------------------------------------------------------------- [ commit ]");
    repoManager.commit();
    serviceA.doWork();

    System.out.println("----------------- [ service B & C created, B old and C new transaction ]");
    var serviceB = new ServiceRequestScope("B", repo.snapshot(tx));
    var serviceC = new ServiceRequestScope("C", repo.snapshot(repoManager.requestScopedTransaction()));
    serviceA.doWork();
    serviceB.doWork();
    serviceC.doWork();

    System.out.println("------------------------------------ [ Create Updator and do an update ]");
    var updator = new Updater(repo.mutableSnapshot());
    updator.update();
    serviceA.doWork();
    serviceB.doWork();
    serviceC.doWork();

    System.out.println("------------------------------------------------------------- [ commit ]");
    repoManager.commit();
    serviceA.doWork();
    serviceB.doWork();
    serviceC.doWork();

    System.out.println("--------------------------------- [ service D created, new transaction ]");
    var serviceD = new ServiceRequestScope("D", repo.snapshot(repoManager.requestScopedTransaction()));
    serviceA.doWork();
    serviceB.doWork();
    serviceC.doWork();
    serviceD.doWork();
  }

  static class ServiceRequestScope {
    private final String name;
    private ReadOnlyTestSnapshot repo;

    public ServiceRequestScope(String name, ReadOnlyTestSnapshot repo) {
      this.name = name;
      this.repo = repo;
    }

    public void doWork() {
      System.out.println(name + ": " + repo.state());
    }
  }

  static class Updater {
    private int counter = 0;
    private Supplier<MutableTestSnapshot> repo;

    public Updater(Supplier<MutableTestSnapshot> repo) {
      this.repo = repo;
    }

    public void update() {
      repo.get().setState("New State " + (++counter));
    }
  }

}
