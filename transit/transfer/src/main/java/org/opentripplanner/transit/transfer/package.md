# Transfers

## Regular Transfers

### Core concepts

| Term                                                                                                                     | Meaning in this module                                                                                                                                                                                                                                                                                                                        |
| ------------------------------------------------------------------------------------------------------------------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Regular Transfer**                                                                                                     | The generic concept, for example a walk (or other street) connection between two regular stops, used by Raptor. It excludes flex and stop-to-stop transfers defined in the transit feed.                                                                                                                                                      |
| **Street Path (`P`)** (~~Path / path template; `NearbyStop` in the application module~~)                                 | The street path of a transfer produced by AStar. It is opaque to this module.                                                                                                                                                                                                                                                                 |
| **Routing Transfer**                                                                                                     | A compact, routing-time representation of a transfer: `(toStop, c1, durationInSeconds)`. It is used by Raptor, and a set is cached for each set of User Preferences.                                                                                                                                                                          |
| **User Preferences**                                                                                                     | The traveler's street preferences, such as speed and reluctance for a given street mode. Some properties are shared and some are mode specific.                                                                                                                                                                                               |
| **User Preference Distance** (~~normalizedDistance, "closest profile"~~)                                                 | Two instances can be matched by distance - a sum of all property distances. Each property distance is weighted and the delta may be on a linear or logarithmic scale.                                                                                                                                                                         |
| **Transfer Profile**                                                                                                     | A configured profile used to generate a set of Transfer Templates for a given set of User Preferences.                                                                                                                                                                                                                                        |
| **Parent profile** (~~Deduplication profile~~)                                                                           | The profile another profile (its child) is based on. The child inherits the routing request and the user preferences of the parent, and is generated after it. A path of the child that costs about the same as the path of the parent, see Deduplication limit, is the path of the parent: the same object is stored once for both profiles. |
| **Deduplication limit**                                                                                                  | The tolerance, a linear function of the cost, that decides if the path of a parent is equivalent to the best path of a child. The cost is compared with the preferences of the child. It applies to cost only, not duration.                                                                                                                  |
| **Transfer Template** (~~no type; the code stores only `P` as "path template", keyed by `(profile, fromStop, toStop)`~~) | The street path, from/to stop information needed to generate a RaptorTransfer. This is generated at build time and should be updated by realtime updates if needed. E.g. New stops used or elevator opened/closed.                                                                                                                            |
| **Routing Transfer Cache** (~~RegularTransferServiceCache~~)                                                             | For every transit planning request, the User Preferences are used to look up or compute the Routing Transfers for that request. The set is cached, to avoid recomputation. It is a bounded LRU cache keyed on User Preferences.                                                                                                               |
| **From/To Stop Id/Index**                                                                                                | The stop each transfer goes from(start) and to(end). In Raptor an `int` index is used for performance, while we use the `id : FeedScopedId` elsewhere. Be explicit use the suffix: id/index                                                                                                                                                   |
| ? **Transfer Path**                                                                                                      | The `(fromStop?, toStopId, streetPath, routingTransferCriteria)`.                                                                                                                                                                                                                                                                             |
| **Routing Transfer Criteria** (~~PathCriteria~~)                                                                         | The pair `(c1, durationInSeconds)` of a path under given preferences.                                                                                                                                                                                                                                                                         |
| **Routing Request (`R`)** (~~Request Parameters~~)                                                                       | The request type of the application, opaque to this module. Each profile is configured with a base request, the User Preferences of the profile are applied on top of it to search for and re-cost the paths. `TransferRequestMapper` maps between a request and User Preferences.                                                            |

### Storage and lifecycle

| Term                                                                            | Meaning                                                                                        |
| ------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------- |
| **Import Repository** (~~Build repository~~) (`RegularTransferBuildRepository`) | Graph-build and serialized form. It is sealed when the runtime repository is created.          |
| **Runtime repository** (`RegularTransferRepository`)                            | Mutable, writer-thread only, inside the transaction framework.                                 |
| **Snapshot** (`RegularTransferRepositorySnapshot`)                              | Immutable read view, published on each commit. A request resolves one for its whole duration.  |
| **Seal**                                                                        | After sealing, writes to the build repository throw.                                           |
| ? **Base / overlay** (`OverlaidTemplatePaths`)                                  | The base holds the paths generated at graph build. The overlay holds the runtime replacements. |
| **Leaf**                                                                        | All paths from one `fromStop` under one profile. It is the unit of change.                     |
| **Replace / reset**                                                             | `replaceTransfersFrom` swaps a leaf, and `resetTransfersFrom` restores the base leaf.          |
| ? **StoredPath**                                                                | The record `(fromStop, toStop, path)`.                                                         |
| **TemplatePathsByUserProfile / ByStop**                                         | The two key levels of the base: profile, then `fromStop`, then `toStop`.                       |

### Caching

| Term                              | Meaning                                                                                                                         |
| --------------------------------- | ------------------------------------------------------------------------------------------------------------------------------- |
| **Single-flight**                 | Concurrent requests for the same key wait for one creation.                                                                     |
| **Carry forward**                 | A new snapshot's cache is derived from the previous one.                                                                        |
| **Stale entry**                   | An entry whose source paths changed. It is patched instead of rebuilt.                                                          |
| **Patch vs. create from scratch** | Patching is used when at most `MAX_PATCHED_STOPS_SHARE` (5%) of stops changed. Memory locality is the reason for the threshold. |

### Realtime updates

The regular transfers are generated at graph build time, but the module is designed to change them
at runtime. There are two reasons to do so:

- **Elevators.** An elevator that is out of order should not be used for wheelchair transfers, see
  [#6533](https://github.com/opentripplanner/OpenTripPlanner/issues/6533).
- **Changed trip patterns.** Realtime updates can use stops that have no transfers, for example a
  new stop in an extra journey. Today this is solved by generating more transfers than necessary at
  graph build time, see [#7074](https://github.com/opentripplanner/OpenTripPlanner/issues/7074).

The remaining work is tracked in
[#8061](https://github.com/opentripplanner/OpenTripPlanner/issues/8061).

**What is supported today.** The storage and the cache are prepared for updates, but nothing
generates them yet:

- The **runtime repository** replaces the transfers of one leaf (all paths from one `fromStop` under
  one profile) with `replaceTransfersFrom`, and restores the graph-build paths of a leaf with
  `resetTransfersFrom`. The paths generated at graph build are never copied: they are shared as the
  base, and the replaced leaves are kept in an overlay.
- The repository is updated in a transaction on the transaction framework. Each commit publishes a
  new immutable **snapshot**. A request resolves one snapshot at its start, so the transfers used
  for routing and the paths used for the itineraries always come from the same snapshot, even if an
  update is committed while the request runs.
- The **Routing Transfer Cache** is carried forward from one snapshot to the next. A cached value is
  reused if the paths it is derived from did not change. If some did, only the transfers from the
  changed stops are re-costed, as long as at most 5% of the stops changed. Above that, the value is
  created from scratch.

**What is not implemented yet.** `TransferRealtimeUpdater` defines the use cases: a new stop is
used, and an elevator or other vertex is opened or closed. It has no implementation, and no updater
writes to the repository. An implementation is expected to run the same profile search as the graph
build for the affected stops, and to write the result with `replaceTransfersFrom` in a transaction.

## Constrained Transfers

TODO: We are moving these here later - currently in the application module.
