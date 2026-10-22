# Transfers

## Regular Transfers

### Core concepts

| Term                                                                                                                     | Meaning in this module                                                                                                                                                                                             |
| ------------------------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **Regular Transfer**                                                                                                     | The generic concept, for example a walk (or other street) connection between two regular stops, used by Raptor. It excludes flex and stop-to-stop transfers defined in the transit feed.                           |
| **Street Path (`P`)** (~~Path / path template; `NearbyStop` in the application module~~)                                 | The street path of a transfer produced by AStar. It is opaque to this module.                                                                                                                                      |
| **Raptor Transfer**                                                                                                      | A compact, routing-time representation of a transfer: `(toStop, c1, durationInSeconds)`. It is used by Raptor, and a set is cached for each set of User Preferences.                                               |
| **User Preferences**                                                                                                     | The traveler's street preferences, such as speed and reluctance for a given street mode. Some properties are shared and some are mode specific.                                                                    |
| **User Preference Distance** (~~normalizedDistance, "closest profile"~~)                                                 | Two instances can be matched by distance - a sum of all property distances. Each property distance is weighted and the delta may be on a linear or logarithmic scale.                                              |
| **Transfer Profile**                                                                                                     | A configured profile used to generate a set of Transfer Templates for a given set of User Preferences.                                                                                                             |
| **Deduplication profile**                                                                                                | A profile whose paths a dependent profile is compared against. A path equivalent under both is not stored twice.                                                                                                   |
| **Transfer Template** (~~no type; the code stores only `P` as "path template", keyed by `(profile, fromStop, toStop)`~~) | The street path, from/to stop information needed to generate a RaptorTransfer. This is generated at build time and should be updated by realtime updates if needed. E.g. New stops used or elevator opened/closed. |
| **Raptor Transfer Cache** (~~RegularTransferServiceCache~~)                                                              | For every transit planning request, the User Preferences are used to look up or compute the Raptor Transfers for that request. The set is cached, to avoid recomputation.                                          |
| **From/To Stop Id/Index**                                                                                                | The stop each transfer goes from(start) and to(end). In Raptor an `int` index is used for performance, while we use the `id : FeedScopedId` elsewhere. Be explicit use the suffix: id/index                        |
| ? **Transfer Path**                                                                                                      | The `(fromStop?, toStopId, streetPath, raptorTransferCriteria)`.                                                                                                                                                   |
| **Raptor Transfer Criteria** (~~PathCriteria~~)                                                                          | The pair `(c1, durationInSeconds)` of a path under given preferences.                                                                                                                                              |
| **Request Parameters (`R`)**                                                                                             | The configured parameters used to compute the templates. They will also be used for updating templates and recalculating Raptor Transfer Criteria.                                                                 |

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

| Term                                              | Meaning                                                                                                                         |
| ------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------- |
| **Service cache** (`RegularTransferServiceCache`) | A bounded LRU cache of the Raptor transfers, keyed on user preferences.                                                         |
| **Single-flight**                                 | Concurrent requests for the same key wait for one creation.                                                                     |
| **Carry forward**                                 | A new snapshot's cache is derived from the previous one.                                                                        |
| **Stale entry**                                   | An entry whose source paths changed. It is patched instead of rebuilt.                                                          |
| **Patch vs. create from scratch**                 | Patching is used when at most `MAX_PATCHED_STOPS_SHARE` (5%) of stops changed. Memory locality is the reason for the threshold. |

## Constrained Transfers

TODO: We are moving these here later - currently in the application module.
