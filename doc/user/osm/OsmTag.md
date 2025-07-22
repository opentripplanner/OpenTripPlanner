# OSM tag mapping

This page is intended to give an overview of which OpenStreetMap(OSM) tags OTP uses to evaluate its
walking and bicycling instructions. If a tag is not part of the documentation on this page
then this tag mapper (profile) does not use it. 

The exception are access permissions and wheelchair accessibility tags like

- `access=no`
- `wheelchair=no`
- `oneway=yes`

These are identical for all mappers and not separately listed on this page.

### Way properties

Way properties set a way's permission and optionally influences its walk and bicycle safety factors.

These factors determine how desirable an OSM way is when routing for cyclists and pedestrians.
Lower safety values make an OSM way more desirable and higher values less desirable.

<!-- props BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

| specifier                                               | permission               | bike safety                   | walk safety |
|---------------------------------------------------------|--------------------------|-------------------------------|-------------|
| `mtb:scale=3`                                           | `NONE`                   |                               |             |
| `mtb:scale=4`                                           | `NONE`                   |                               |             |
| `mtb:scale=5`                                           | `NONE`                   |                               |             |
| `mtb:scale=6`                                           | `NONE`                   |                               |             |
| `highway=bridleway`                                     | `NONE`                   | 1.3                           |             |
| `highway=corridor`                                      | `PEDESTRIAN`             |                               |             |
| `highway=steps`                                         | `PEDESTRIAN`             |                               |             |
| `highway=crossing`                                      | `PEDESTRIAN`             |                               |             |
| `highway=platform`                                      | `PEDESTRIAN`             |                               |             |
| `public_transport=platform`                             | `PEDESTRIAN`             |                               |             |
| `railway=platform`                                      | `PEDESTRIAN`             |                               |             |
| `footway=sidewalk; highway=footway`                     | `PEDESTRIAN`             |                               |             |
| `highway=pedestrian`                                    | `PEDESTRIAN`             | 0.9                           |             |
| `highway=footway`                                       | `PEDESTRIAN`             | 1.1                           |             |
| `mtb:scale=1`                                           | `PEDESTRIAN`             |                               |             |
| `mtb:scale=2`                                           | `PEDESTRIAN`             |                               |             |
| `highway=cycleway`                                      | `BICYCLE`                | 0.6                           |             |
| `mtb:scale=0`                                           | `PEDESTRIAN_AND_BICYCLE` |                               |             |
| `highway=path`                                          | `PEDESTRIAN_AND_BICYCLE` | 0.75                          |             |
| `highway=living_street`                                 | `ALL`                    | 0.9                           |             |
| `highway=unclassified`                                  | `ALL`                    |                               |             |
| `highway=road`                                          | `ALL`                    |                               |             |
| `highway=byway`                                         | `ALL`                    | 1.3                           |             |
| `highway=track`                                         | `ALL`                    | 1.3                           |             |
| `highway=service`                                       | `ALL`                    | 1.1                           |             |
| `highway=residential`                                   | `ALL`                    | 0.98                          |             |
| `highway=residential_link`                              | `ALL`                    | 0.98                          |             |
| `highway=tertiary`                                      | `ALL`                    |                               |             |
| `highway=tertiary_link`                                 | `ALL`                    |                               |             |
| `highway=secondary`                                     | `ALL`                    | 1.5                           |             |
| `highway=secondary_link`                                | `ALL`                    | 1.5                           |             |
| `highway=primary`                                       | `ALL`                    | 2.06                          |             |
| `highway=primary_link`                                  | `ALL`                    | 2.06                          |             |
| `highway=trunk`                                         | `ALL`                    | 7.47                          |             |
| `highway=trunk_link`                                    | `ALL`                    | 2.06                          |             |
| `highway=motorway_link`                                 | `CAR`                    | 2.06                          |             |
| `highway=motorway`                                      | `CAR`                    | 8.0                           |             |
| `motorroad=yes`                                         | `CAR`                    |                               |             |
| `present(highway); cycleway=lane`                       | `PEDESTRIAN_AND_BICYCLE` | 0.87                          |             |
| `highway=service; cycleway=lane`                        | `ALL`                    | 0.77                          |             |
| `highway=residential; cycleway=lane`                    | `ALL`                    | 0.77                          |             |
| `highway=residential_link; cycleway=lane`               | `ALL`                    | 0.77                          |             |
| `highway=tertiary; cycleway=lane`                       | `ALL`                    | 0.87                          |             |
| `highway=tertiary_link; cycleway=lane`                  | `ALL`                    | 0.87                          |             |
| `highway=secondary; cycleway=lane`                      | `ALL`                    | 0.96                          |             |
| `highway=secondary_link; cycleway=lane`                 | `ALL`                    | 0.96                          |             |
| `highway=primary; cycleway=lane`                        | `ALL`                    | 1.15                          |             |
| `highway=primary_link; cycleway=lane`                   | `ALL`                    | 1.15                          |             |
| `highway=trunk; cycleway=lane`                          | `ALL`                    | 1.5                           |             |
| `highway=trunk_link; cycleway=lane`                     | `ALL`                    | 1.15                          |             |
| `highway=motorway; cycleway=lane`                       | `BICYCLE_AND_CAR`        | 2.0                           |             |
| `highway=motorway_link; cycleway=lane`                  | `BICYCLE_AND_CAR`        | 1.15                          |             |
| `present(highway); cycleway=share_busway`               | `PEDESTRIAN_AND_BICYCLE` | 0.92                          |             |
| `highway=service; cycleway=share_busway`                | `ALL`                    | 0.85                          |             |
| `highway=residential; cycleway=share_busway`            | `ALL`                    | 0.85                          |             |
| `highway=residential_link; cycleway=share_busway`       | `ALL`                    | 0.85                          |             |
| `highway=tertiary; cycleway=share_busway`               | `ALL`                    | 0.92                          |             |
| `highway=tertiary_link; cycleway=share_busway`          | `ALL`                    | 0.92                          |             |
| `highway=secondary; cycleway=share_busway`              | `ALL`                    | 0.99                          |             |
| `highway=secondary_link; cycleway=share_busway`         | `ALL`                    | 0.99                          |             |
| `highway=primary; cycleway=share_busway`                | `ALL`                    | 1.25                          |             |
| `highway=primary_link; cycleway=share_busway`           | `ALL`                    | 1.25                          |             |
| `highway=trunk; cycleway=share_busway`                  | `BICYCLE_AND_CAR`        | 1.75                          |             |
| `highway=trunk_link; cycleway=share_busway`             | `BICYCLE_AND_CAR`        | 1.25                          |             |
| `highway=motorway; cycleway=share_busway`               | `BICYCLE_AND_CAR`        | 2.5                           |             |
| `highway=motorway_link; cycleway=share_busway`          | `BICYCLE_AND_CAR`        | 1.25                          |             |
| `present(highway); cycleway=opposite_lane`              | `PEDESTRIAN_AND_BICYCLE` | forward: 1.0 <br> back: 0.87  |             |
| `highway=service; cycleway=opposite_lane`               | `ALL`                    | forward: 1.1 <br> back: 0.77  |             |
| `highway=residential; cycleway=opposite_lane`           | `ALL`                    | forward: 0.98 <br> back: 0.77 |             |
| `highway=residential_link; cycleway=opposite_lane`      | `ALL`                    | forward: 0.98 <br> back: 0.77 |             |
| `highway=tertiary; cycleway=opposite_lane`              | `ALL`                    | forward: 1.0 <br> back: 0.87  |             |
| `highway=tertiary_link; cycleway=opposite_lane`         | `ALL`                    | forward: 1.0 <br> back: 0.87  |             |
| `highway=secondary; cycleway=opposite_lane`             | `ALL`                    | forward: 1.5 <br> back: 0.96  |             |
| `highway=secondary_link; cycleway=opposite_lane`        | `ALL`                    | forward: 1.5 <br> back: 0.96  |             |
| `highway=primary; cycleway=opposite_lane`               | `ALL`                    | forward: 2.06 <br> back: 1.15 |             |
| `highway=primary_link; cycleway=opposite_lane`          | `ALL`                    | forward: 2.06 <br> back: 1.15 |             |
| `highway=trunk; cycleway=opposite_lane`                 | `BICYCLE_AND_CAR`        | forward: 7.47 <br> back: 1.5  |             |
| `highway=trunk_link; cycleway=opposite_lane`            | `BICYCLE_AND_CAR`        | forward: 2.06 <br> back: 1.15 |             |
| `present(highway); cycleway=track`                      | `PEDESTRIAN_AND_BICYCLE` | 0.75                          |             |
| `highway=service; cycleway=track`                       | `ALL`                    | 0.65                          |             |
| `highway=residential; cycleway=track`                   | `ALL`                    | 0.65                          |             |
| `highway=residential_link; cycleway=track`              | `ALL`                    | 0.65                          |             |
| `highway=tertiary; cycleway=track`                      | `ALL`                    | 0.75                          |             |
| `highway=tertiary_link; cycleway=track`                 | `ALL`                    | 0.75                          |             |
| `highway=secondary; cycleway=track`                     | `ALL`                    | 0.8                           |             |
| `highway=secondary_link; cycleway=track`                | `ALL`                    | 0.8                           |             |
| `highway=primary; cycleway=track`                       | `ALL`                    | 0.85                          |             |
| `highway=primary_link; cycleway=track`                  | `ALL`                    | 0.85                          |             |
| `highway=trunk; cycleway=track`                         | `BICYCLE_AND_CAR`        | 0.95                          |             |
| `highway=trunk_link; cycleway=track`                    | `BICYCLE_AND_CAR`        | 0.85                          |             |
| `present(highway); cycleway=opposite_track`             | `PEDESTRIAN_AND_BICYCLE` | forward: 1.0 <br> back: 0.75  |             |
| `highway=service; cycleway=opposite_track`              | `ALL`                    | forward: 1.1 <br> back: 0.65  |             |
| `highway=residential; cycleway=opposite_track`          | `ALL`                    | forward: 0.98 <br> back: 0.65 |             |
| `highway=residential_link; cycleway=opposite_track`     | `ALL`                    | forward: 0.98 <br> back: 0.65 |             |
| `highway=tertiary; cycleway=opposite_track`             | `ALL`                    | forward: 1.0 <br> back: 0.75  |             |
| `highway=tertiary_link; cycleway=opposite_track`        | `ALL`                    | forward: 1.0 <br> back: 0.75  |             |
| `highway=secondary; cycleway=opposite_track`            | `ALL`                    | forward: 1.5 <br> back: 0.8   |             |
| `highway=secondary_link; cycleway=opposite_track`       | `ALL`                    | forward: 1.5 <br> back: 0.8   |             |
| `highway=primary; cycleway=opposite_track`              | `ALL`                    | forward: 2.06 <br> back: 0.85 |             |
| `highway=primary_link; cycleway=opposite_track`         | `ALL`                    | forward: 2.06 <br> back: 0.85 |             |
| `highway=trunk; cycleway=opposite_track`                | `BICYCLE_AND_CAR`        | forward: 7.47 <br> back: 0.95 |             |
| `highway=trunk_link; cycleway=opposite_track`           | `BICYCLE_AND_CAR`        | forward: 2.06 <br> back: 0.85 |             |
| `present(highway); cycleway=shared_lane`                | `PEDESTRIAN_AND_BICYCLE` | 0.77                          |             |
| `highway=service; cycleway=shared_lane`                 | `ALL`                    | 0.73                          |             |
| `highway=residential; cycleway=shared_lane`             | `ALL`                    | 0.77                          |             |
| `highway=residential_link; cycleway=shared_lane`        | `ALL`                    | 0.77                          |             |
| `highway=tertiary; cycleway=shared_lane`                | `ALL`                    | 0.83                          |             |
| `highway=tertiary_link; cycleway=shared_lane`           | `ALL`                    | 0.83                          |             |
| `highway=secondary; cycleway=shared_lane`               | `ALL`                    | 1.25                          |             |
| `highway=secondary_link; cycleway=shared_lane`          | `ALL`                    | 1.25                          |             |
| `highway=primary; cycleway=shared_lane`                 | `ALL`                    | 1.75                          |             |
| `highway=primary_link; cycleway=shared_lane`            | `ALL`                    | 1.75                          |             |
| `present(highway); cycleway=opposite`                   | `PEDESTRIAN_AND_BICYCLE` | forward: 1.0 <br> back: 1.4   |             |
| `highway=service; cycleway=opposite`                    | `ALL`                    | 1.1                           |             |
| `highway=residential; cycleway=opposite`                | `ALL`                    | 0.98                          |             |
| `highway=residential_link; cycleway=opposite`           | `ALL`                    | 0.98                          |             |
| `highway=tertiary; cycleway=opposite`                   | `ALL`                    |                               |             |
| `highway=tertiary_link; cycleway=opposite`              | `ALL`                    |                               |             |
| `highway=secondary; cycleway=opposite`                  | `ALL`                    | forward: 1.5 <br> back: 1.71  |             |
| `highway=secondary_link; cycleway=opposite`             | `ALL`                    | forward: 1.5 <br> back: 1.71  |             |
| `highway=primary; cycleway=opposite`                    | `ALL`                    | forward: 2.06 <br> back: 2.99 |             |
| `highway=primary_link; cycleway=opposite`               | `ALL`                    | forward: 2.06 <br> back: 2.99 |             |
| `highway=path; bicycle=designated`                      | `PEDESTRIAN_AND_BICYCLE` | 0.6                           |             |
| `highway=footway; bicycle=designated`                   | `PEDESTRIAN_AND_BICYCLE` | 0.75                          |             |
| `highway=footway; bicycle=yes; area=yes`                | `PEDESTRIAN_AND_BICYCLE` | 0.9                           |             |
| `highway=pedestrian; bicycle=designated`                | `PEDESTRIAN_AND_BICYCLE` | 0.75                          |             |
| `footway=sidewalk; highway=footway; bicycle=yes`        | `PEDESTRIAN_AND_BICYCLE` | 2.5                           |             |
| `footway=sidewalk; highway=footway; bicycle=designated` | `PEDESTRIAN_AND_BICYCLE` | 1.1                           |             |
| `highway=footway; footway=crossing`                     | `PEDESTRIAN`             | 2.5                           |             |
| `highway=footway; footway=crossing; bicycle=designated` | `PEDESTRIAN_AND_BICYCLE` | 1.1                           |             |
| `highway=track; bicycle=yes`                            | `PEDESTRIAN_AND_BICYCLE` | 1.18                          |             |
| `highway=track; bicycle=designated`                     | `PEDESTRIAN_AND_BICYCLE` | 0.99                          |             |
| `highway=track; bicycle=yes; present(surface)`          | `PEDESTRIAN_AND_BICYCLE` | 1.18                          |             |
| `highway=track; bicycle=designated; present(surface)`   | `PEDESTRIAN_AND_BICYCLE` | 0.99                          |             |
| `highway=track; present(surface)`                       | `PEDESTRIAN_AND_BICYCLE` | 1.3                           |             |
| `present(highway); bicycle=designated`                  | `BICYCLE`                | 0.97                          |             |
| `highway=footway; bicycle=designated`                   | `PEDESTRIAN_AND_BICYCLE` | 0.8                           |             |
| `highway=cycleway; bicycle=designated`                  | `BICYCLE`                | 0.6                           |             |
| `highway=bridleway; bicycle=designated`                 | `BICYCLE`                | 0.8                           |             |
| `highway=service; bicycle=designated`                   | `ALL`                    | 0.84                          |             |
| `highway=residential; bicycle=designated`               | `ALL`                    | 0.95                          |             |
| `highway=unclassified; bicycle=designated`              | `ALL`                    | 0.95                          |             |
| `highway=residential_link; bicycle=designated`          | `ALL`                    | 0.95                          |             |
| `highway=tertiary; bicycle=designated`                  | `ALL`                    | 0.97                          |             |
| `highway=tertiary_link; bicycle=designated`             | `ALL`                    | 0.97                          |             |
| `highway=secondary; bicycle=designated`                 | `ALL`                    | 1.46                          |             |
| `highway=secondary_link; bicycle=designated`            | `ALL`                    | 1.46                          |             |
| `highway=primary; bicycle=designated`                   | `ALL`                    | 2.0                           |             |
| `highway=primary_link; bicycle=designated`              | `ALL`                    | 2.0                           |             |
| `highway=trunk; bicycle=designated`                     | `BICYCLE_AND_CAR`        | 7.25                          |             |
| `highway=trunk_link; bicycle=designated`                | `BICYCLE_AND_CAR`        | 2.0                           |             |
| `highway=motorway; bicycle=designated`                  | `BICYCLE_AND_CAR`        | 7.76                          |             |
| `highway=motorway_link; bicycle=designated`             | `BICYCLE_AND_CAR`        | 2.0                           |             |

<!-- props END -->

### Safety mixins

Mixins are selectors that have only an effect on the bicycle and walk safety factors but not on the
permission of an OSM way. Their safety values are multiplied with the base values from the selected
way properties. Multiple mixins can apply to the same way and their effects compound.

<!-- mixins BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

| matcher                                                    | bicycle safety | walk safety |
|------------------------------------------------------------|----------------|-------------|
| `lcn=yes¦rcn=yes¦ncn=yes¦bicycle_road=yes¦cyclestreet=yes` | 0.7            |             |
| `surface=unpaved`                                          | 1.18           |             |
| `surface=compacted`                                        | 1.18           |             |
| `surface=wood`                                             | 1.18           |             |
| `surface=cobblestone`                                      | 1.3            |             |
| `surface=sett`                                             | 1.3            |             |
| `surface=unhewn_cobblestone`                               | 1.5            |             |
| `surface=grass_paver`                                      | 1.3            |             |
| `surface=pebblestone`                                      | 1.3            |             |
| `surface=metal`                                            | 1.3            |             |
| `surface=ground`                                           | 1.5            |             |
| `surface=dirt`                                             | 1.5            |             |
| `surface=earth`                                            | 1.5            |             |
| `surface=grass`                                            | 1.5            |             |
| `surface=mud`                                              | 1.5            |             |
| `surface=woodchip`                                         | 1.5            |             |
| `surface=gravel`                                           | 1.5            |             |
| `surface=artifical_turf`                                   | 1.5            |             |
| `surface=sand`                                             | 100.0          |             |
| `foot=discouraged`                                         |                | 3.0         |
| `bicycle=discouraged`                                      | 3.0            |             |
| `foot=use_sidepath`                                        |                | 5.0         |
| `bicycle=use_sidepath`                                     | 5.0            |             |

<!-- mixins END -->
