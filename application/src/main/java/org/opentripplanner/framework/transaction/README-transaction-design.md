# Transaction framework design

This document describes the design of the `org.opentripplanner.framework.transaction` package and its sub-packages.

## Goals

- Provide a simple, framework-local transaction concept that is independent of any particular database or storage technology.
- Allow services to work with **consistent, immutable snapshots** of a repository during a request.
- Allow updates to be built up against an **isolated mutable view**, and then atomically published to all readers on commit.
- Keep the API small and easy to test.

## Overview

The transaction framework is built around three main concepts:

1. **Transaction identity** (`Transaction`)
2. **Transactional repository abstraction** (`TransactionalRepository` and `RepositoryLifecycle`)
3. **Transaction manager** (`RepositoryTransactionManager` and default implementations)

Test utilities in the `transaction.test` package illustrate how these pieces fit together.

### High-level workflow

At a high level, the workflow looks like this:

1. A `RepositoryTransactionManager` owns the **current transaction**.
2. Read-only services ask a `TransactionalRepository` for a **snapshot** bound to a specific `Transaction`.
3. Writers obtain a **mutable snapshot supplier**, make changes, and then call `commit()` on the `RepositoryTransactionManager`.
4. On commit, a new transaction is created and all registered repositories either:
   - freeze their mutable snapshot into a new immutable snapshot, or
   - carry forward the previous snapshot if no changes were made.
5. Future readers use the new transaction and see the updated state, while existing readers keep their old snapshot.

This provides copy-on-write semantics with clear transaction boundaries and predictable snapshot behaviour.

## Public API

The public API for this framework lives in `org.opentripplanner.framework.transaction`.

### `Transaction`

```java
public interface Transaction {
}
```

`Transaction` is a **marker interface** that represents a transaction identity. It does not define behaviour; instead, it allows different implementations to be substituted (for example, the default in-memory implementation or a future database-backed implementation).

### `RepositoryLifecycle<S, T>`

```java
public interface RepositoryLifecycle<T, U> {
  U copyOnWrite(T readOnlyRepository);
  T freeze(U editableReopsitory);
}
```

`RepositoryLifecycle` defines how a particular repository type participates in the transactional model:

- `copyOnWrite(T readOnlyRepository)`
  - Takes a read-only snapshot (`T`) and produces a **mutable copy** (`U`).
  - This is called when a writer first needs to modify state in the current transaction.

- `freeze(U editableRepository)`
  - Takes the mutable instance (`U`) and converts it back into an **immutable snapshot** (`T`).
  - This is called during commit when changes should be made visible in the next transaction.

This separation between `T` (read-only type) and `U` (mutable type) makes it explicit where mutation is allowed.

### `TransactionalRepository<S, T>`

```java
public interface TransactionalRepository<S, T> {

  S snapshot(Transaction transaction);

  Supplier<T> mutableSnapshot();
}
```

`TransactionalRepository` is the main entry point for application code that needs to read or modify repository state.

- `snapshot(Transaction transaction)`
  - Returns an immutable snapshot (`S`) that is **tied to the given transaction**.
  - Multiple calls with the same `Transaction` return the same logical snapshot, ensuring consistency for that transaction.

- `mutableSnapshot()`
  - Returns a `Supplier<T>` that lazily creates or returns the current **mutable snapshot** (`T`) for the active transaction.
  - Callers can obtain a mutable instance, apply changes, and rely on the transaction manager to freeze and publish it on commit.

### `RepositoryTransactionManager`

```java
public interface RepositoryTransactionManager {

  Transaction requestScopedTransaction();

  void commit();
}
```

`RepositoryTransactionManager` defines how transactions are managed at the application level:

- `requestScopedTransaction()`
  - Returns the **current transaction identity** for the scope (for example, the current request or operation).
  - Readers use this to obtain consistent snapshots from repositories.

- `commit()`
  - Creates a new transaction and coordinates a commit across all registered repositories.
  - Each repository is responsible for freezing any mutable snapshot and associating the resulting immutable snapshot with the new transaction.

## Default implementations (internal)

The `org.opentripplanner.framework.transaction.internal` package contains an in-memory implementation suitable for application-level state that needs transactional semantics but does not rely on an external database.

### `DefaultTransaction`

```java
final class DefaultTransaction implements Transaction {
  private static final AtomicLong ID_SEQUENCE = new AtomicLong(0);
  private final long id;

  private DefaultTransaction() {
    this.id = ID_SEQUENCE.incrementAndGet();
  }

  static DefaultTransaction next() { ... }

  @Override
  public boolean equals(Object o) { ... }

  @Override
  public int hashCode() { ... }
}
```

`DefaultTransaction` is a simple in-memory implementation of `Transaction`:

- Each instance is identified by a monotonically increasing `long`.
- Equality and hash code are based solely on this id.
- Instances are created using the static factory method `next()`.

This implementation is intentionally package-private and is only used by the default manager and repositories.

### `DefaultRepositoryTransactionManager`

```java
public class DefaultRepositoryTransactionManager implements RepositoryTransactionManager {

  private final AtomicReference<DefaultTransaction> currentTransaction =
    new AtomicReference<>(DefaultTransaction.next());

  private final List<DefaultTransactionalRepository<?, ?>> repositories = new ArrayList<>();

  @Override
  public Transaction requestScopedTransaction() {
    return currentTransaction.get();
  }

  @Override
  public void commit() {
    var currentTx = currentTransaction.get();
    var nextTx = currentTx.next();

    for (var repository : repositories) {
      repository.commit(currentTx, nextTx);
    }
    currentTransaction.set(nextTx);
  }

  void register(DefaultTransactionalRepository<?, ?> repository) {
    repositories.add(repository);
  }

  Supplier<DefaultTransaction> currentTransaction() {
    return () -> currentTransaction.get();
  }
}
```

Responsibilities:

- Track the **current transaction** using an `AtomicReference<DefaultTransaction>`.
- Provide the current `Transaction` to callers via `requestScopedTransaction()`.
- Maintain a list of all `DefaultTransactionalRepository` instances that participate in commits.
- On `commit()`:
  - Create a new transaction (`nextTx`).
  - Ask each repository to commit from `currentTx` to `nextTx`.
  - Set the new transaction as current.

### `DefaultTransactionalRepository<S, T>`

```java
public class DefaultTransactionalRepository<S, T> implements TransactionalRepository<S, T> {

  private final RepositoryLifecycle<S, T> lifecycle;
  private final Supplier<DefaultTransaction> transactionProvider;
  private final Map<DefaultTransaction, S> snapshotsCache = new WeakHashMap<>();
  private T mutableSnapshot;

  public DefaultTransactionalRepository(
    S initialSnapshot,
    RepositoryLifecycle<S, T> lifecycle,
    DefaultRepositoryTransactionManager manager
  ) { ... }

  @Override
  public S snapshot(Transaction transaction) { ... }

  @Override
  public Supplier<T> mutableSnapshot() { ... }

  void commit(DefaultTransaction currentTransaction, DefaultTransaction nextTransaction) { ... }

  private T currentMutableSnapshot() { ... }

  private void setSnapshot(S snapshot, DefaultTransaction transaction) { ... }
}
```

Key design points:

- **Snapshots per transaction**
  - The `snapshotsCache` map associates a `DefaultTransaction` with an immutable snapshot (`S`).
  - A `WeakHashMap` is used so that transactions and their snapshots can be garbage-collected when no longer referenced.

- **Lazy mutable snapshot**
  - `mutableSnapshot` is a single mutable instance for the current transaction.
  - It is created on first write by calling `lifecycle.copyOnWrite(...)` on the current immutable snapshot.
  - This avoids unnecessary copying for read-only transactions.

- **Commit behaviour**
  - If a mutable snapshot exists:
    - `lifecycle.freeze(mutableSnapshot)` is called to produce a new immutable snapshot for the next transaction.
  - If no mutable snapshot exists:
    - The previous snapshot is carried forward to the next transaction without change.
  - After commit, the `mutableSnapshot` field is cleared.

- **Thread-safety**
  - Access to `snapshotsCache` is synchronized to ensure consistency when multiple threads access snapshots concurrently.
  - The transaction identity itself is managed atomically by `DefaultRepositoryTransactionManager`.

## Test utilities

The `org.opentripplanner.framework.transaction.test` package provides a simple example repository and lifecycle used for manual testing and demonstration.

- `ReadOnlyTestSnapshot` and `MutableTestSnapshot`
  - Represent read-only and mutable views of a simple string-based state.

- `TestSnapshotLifecycle`
  - Implements `RepositoryLifecycle<ReadOnlyTestSnapshot, MutableTestSnapshot>`.
  - Defines how to copy and freeze the test snapshot.

- `TestRepo`
  - A small `main` program that wires together a `DefaultRepositoryTransactionManager` and a `DefaultTransactionalRepository`.
  - Demonstrates:
    - Creating services bound to specific transactions.
    - Observing how different services see different snapshots as commits happen.
    - Using an `Updater` that works with the mutable snapshot supplier.

This test package is not intended for production use but is helpful for understanding and verifying the transaction behaviour.

## Usage guidelines

- Use `RepositoryTransactionManager` to obtain the current `Transaction` for a request or operation.
- Use `TransactionalRepository.snapshot(transaction)` for **read-only** access tied to that transaction.
- Use the `mutableSnapshot()` supplier only in code that is responsible for performing updates.
- Call `commit()` on `RepositoryTransactionManager` when you want to publish all accumulated changes as a new transaction.

## Extensibility

The design intentionally separates the public interfaces from the default implementations:

- You can provide alternative `Transaction` implementations if you want to integrate with an external transaction system.
- You can implement your own `RepositoryTransactionManager` that, for example, binds transactions to a web request scope or delegates to a database transaction.
- You can create additional `TransactionalRepository` implementations that follow the same lifecycle but store data in different forms.

This allows the framework to be used for a variety of in-memory or hybrid transactional use cases without locking the application into a specific storage technology.

