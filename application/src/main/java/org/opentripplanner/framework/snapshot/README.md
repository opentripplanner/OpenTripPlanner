# Transaction Framework

This package provides a snapshot-based transaction framework for managing consistent reads and serialized writes across application repositories.

The framework uses a copy-on-write strategy where:
- Reads are served from immutable snapshots
- A single writer thread serializes all updates
- Each commit mints a new `Transaction` token
- Request-scoped readers capture a consistent view by holding a strong reference to a `Transaction`

## Core Concepts

- **`Transaction`**: An opaque identity token (not an ACID transaction) that acts as a `WeakHashMap` key to pin snapshots in memory. A new token is minted on every commit.
- **`RepositoryHandle`**: Application-scoped typed reference to a repository, held for the application lifetime and used to access the repository.
- **`RepositoryScope`**: Request-scoped object that captures a `Transaction` at creation time and resolves consistent read-only snapshots for that request.
- **`RepositoryLifecycle`**: Defines the copy-on-write strategy (`copyOnWrite`, `freeze`) for a repository's snapshot types.
- **`UpdateManager`**: Manages a single-writer thread; after each submitted task, commits all changes atomically.

## Performing Updates

Write operations are performed through the `UpdateManager` which serializes all writes on a single thread:

```java
updateManager.submit(ctx -> {
  var mutableSnapshot = ctx.mutable(timetableRepo).get();
  mutableSnapshot.addTrip("T1");
});
```

- The `WriteContext` provides access to mutable snapshots via `ctx.mutable(handle)`
- Copy-on-write is performed lazily on first access
- The operation is automatically committed when the lambda returns

## Request-Scoped Read Operations

To perform consistent reads within a request:

```java
var scope = repositoryRegistry.scope();
var snapshot = scope.snapshot(timetableRepo);
return new TimetableService(snapshot);
```

- `repositoryRegistry.scope()` captures the current `Transaction`
- The `RepositoryScope` holds a strong reference to the `Transaction`, keeping snapshots alive
- All `scope.snapshot()` calls within the same scope resolve against the same transaction view

## Adding a New Repository

To add a new repository:

1. Create a `ReadOnly<X>Snapshot` class (immutable, defensive copies)
2. Create a `Mutable<X>Snapshot` class (simple mutable container)
3. Implement `RepositoryLifecycle<ReadOnly<X>Snapshot, Mutable<X>Snapshot>` with `copyOnWrite` and `freeze`
4. Register at wiring time: `repositoryRegistry.register(initialSnapshot, lifecycle)`
5. Retain and inject the returned `RepositoryHandle<S, T>`

Example wiring from `TimetableConfig`:

```java
public static RepositoryHandle<ReadOnlyTimetableSnapshot, MutableTimetableSnapshot> createRepo(
  RepositoryRegistry repositoryRegistry
) {
  return repositoryRegistry.register(
    new ReadOnlyTimetableSnapshot(List.of()),
    new TimetableSnapshotLifecycle()
  );
}
```

## Domain Event Integration

The framework works with a basic synchronous domain event system. Events published during a write operation (via `WriteContext.publish(event)`) are dispatched to registered handlers within the same transaction context. This enables further decoupling of business logic while maintaining consistency guarantees.