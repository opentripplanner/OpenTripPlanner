# Basic Snapshop Implementation

This is a very basic implementation without any tweaks for performance improvements:

- immutable repositories
- every update copies previous data (this can be optimized by sharing data structures between copies)

This should be the least complex solution and therefore easiest to maintain. I think we should only deviate from this if we have a good reason:

- performance not fast enough
- important functionality missing

We can then try to tweak this to fix the specific issue.

**Simplifications**:
- no need to maintain only one update thread, the while loop ensures that each update is always applied to the expected previous snapshot version
- no need to implement a rollback functionality (if we do a commit for separate repositories, then the changes have to be rolled back whenever one repository fails)
- no need to have a central point holding all currently used snapshots

**possible performance bottlenecks:**
- too many and/or too expensive updates might result in excessive update retries
- using only immutable repositories could slow down applying updates

## Encapsulation of Updates

Every update to the transit data is its own class. The update needs to know about the repositories in the `TransitSnapshot`, since it has to decide which of those repositories need to be updated. One update could also hold some sort of batch update, like modifying a list of trips and not only one trip.

The `TransitSnapshot apply(TransitSnapshot snapshot) {...}` method has to be a pure function, since it might be called multiple times for the same update.

## Workflow

### Read Operation for Request
1. Call `SnapshotManager.snapshot()` to get the current transit data
2. Use respective repository from the snapshot whenever necessary to instantiate data fetchers

### Write Operation for Updaters
1. Create instance of specific update implementing `SnapshotUpdate`
2. Call `SnapshotManager.apply(update)`

## Dependencies
The code is located in framework at the moment, but the dependency is the 'wrong' way for a framework: The snapshot mechanic depends on the repositories and not the other way around. I think the implementation is easier this way and more straight forward. I don't think we have a strong reason to flip the dependencies and write a more generic snapshot implementation, in my opinion we won't gain much but only complicate things. We can discuss on where to best locate this code.