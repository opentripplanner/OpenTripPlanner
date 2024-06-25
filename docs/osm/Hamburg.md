## OSM tag mapping

<!-- props BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

| matcher                                                 | permission             | safety |
|---------------------------------------------------------|------------------------|--------|
| `highway=track`                                         | PEDESTRIAN_AND_BICYCLE |        |
| `highway=track; present(surface)`                       | PEDESTRIAN_AND_BICYCLE |        |
| `highway=residential; junction=roundabout`              | ALL                    | 🚴     |
| `present(highway); junction=roundabout`                 | BICYCLE_AND_CAR        |        |
| `highway=pedestrian`                                    | PEDESTRIAN             |        |
| `highway=residential; maxspeed=30`                      | ALL                    | 🚴     |
| `highway=footway; bicycle=yes`                          | PEDESTRIAN_AND_BICYCLE | 🚴     |
| `footway=sidewalk; highway=footway; bicycle=yes`        | PEDESTRIAN_AND_BICYCLE | 🚴     |
| `highway=unclassified; cycleway=lane`                   | ALL                    | 🚴     |
| `mtb:scale=3`                                           | NONE                   |        |
| `mtb:scale=4`                                           | NONE                   |        |
| `mtb:scale=5`                                           | NONE                   |        |
| `mtb:scale=6`                                           | NONE                   |        |
| `highway=corridor`                                      | PEDESTRIAN             |        |
| `highway=steps`                                         | PEDESTRIAN             |        |
| `highway=crossing`                                      | PEDESTRIAN             |        |
| `highway=platform`                                      | PEDESTRIAN             |        |
| `public_transport=platform`                             | PEDESTRIAN             |        |
| `railway=platform`                                      | PEDESTRIAN             |        |
| `footway=sidewalk; highway=footway`                     | PEDESTRIAN             |        |
| `mtb:scale=1`                                           | PEDESTRIAN             |        |
| `mtb:scale=2`                                           | PEDESTRIAN             |        |
| `mtb:scale=0`                                           | PEDESTRIAN_AND_BICYCLE |        |
| `highway=cycleway`                                      | PEDESTRIAN_AND_BICYCLE | 🚴     |
| `highway=path`                                          | PEDESTRIAN_AND_BICYCLE | 🚴     |
| `highway=pedestrian`                                    | PEDESTRIAN_AND_BICYCLE | 🚴     |
| `highway=footway`                                       | PEDESTRIAN_AND_BICYCLE | 🚴     |
| `highway=bridleway`                                     | PEDESTRIAN_AND_BICYCLE | 🚴     |
| `highway=living_street`                                 | ALL                    | 🚴     |
| `highway=unclassified`                                  | ALL                    |        |
| `highway=road`                                          | ALL                    |        |
| `highway=byway`                                         | ALL                    | 🚴     |
| `highway=track`                                         | ALL                    | 🚴     |
| `highway=service`                                       | ALL                    | 🚴     |
| `highway=residential`                                   | ALL                    | 🚴     |
| `highway=residential_link`                              | ALL                    | 🚴     |
| `highway=tertiary`                                      | ALL                    |        |
| `highway=tertiary_link`                                 | ALL                    |        |
| `highway=secondary`                                     | ALL                    | 🚴     |
| `highway=secondary_link`                                | ALL                    | 🚴     |
| `highway=primary`                                       | ALL                    | 🚴     |
| `highway=primary_link`                                  | ALL                    | 🚴     |
| `highway=trunk_link`                                    | CAR                    | 🚴     |
| `highway=motorway_link`                                 | CAR                    | 🚴     |
| `highway=trunk`                                         | CAR                    | 🚴     |
| `highway=motorway`                                      | CAR                    | 🚴     |
| `present(highway); cycleway=lane`                       | PEDESTRIAN_AND_BICYCLE | 🚴     |
| `highway=service; cycleway=lane`                        | ALL                    | 🚴     |
| `highway=residential; cycleway=lane`                    | ALL                    | 🚴     |
| `highway=residential_link; cycleway=lane`               | ALL                    | 🚴     |
| `highway=tertiary; cycleway=lane`                       | ALL                    | 🚴     |
| `highway=tertiary_link; cycleway=lane`                  | ALL                    | 🚴     |
| `highway=secondary; cycleway=lane`                      | ALL                    | 🚴     |
| `highway=secondary_link; cycleway=lane`                 | ALL                    | 🚴     |
| `highway=primary; cycleway=lane`                        | ALL                    | 🚴     |
| `highway=primary_link; cycleway=lane`                   | ALL                    | 🚴     |
| `highway=trunk; cycleway=lane`                          | BICYCLE_AND_CAR        | 🚴     |
| `highway=trunk_link; cycleway=lane`                     | BICYCLE_AND_CAR        | 🚴     |
| `highway=motorway; cycleway=lane`                       | BICYCLE_AND_CAR        | 🚴     |
| `highway=motorway_link; cycleway=lane`                  | BICYCLE_AND_CAR        | 🚴     |
| `present(highway); cycleway=share_busway`               | PEDESTRIAN_AND_BICYCLE | 🚴     |
| `highway=service; cycleway=share_busway`                | ALL                    | 🚴     |
| `highway=residential; cycleway=share_busway`            | ALL                    | 🚴     |
| `highway=residential_link; cycleway=share_busway`       | ALL                    | 🚴     |
| `highway=tertiary; cycleway=share_busway`               | ALL                    | 🚴     |
| `highway=tertiary_link; cycleway=share_busway`          | ALL                    | 🚴     |
| `highway=secondary; cycleway=share_busway`              | ALL                    | 🚴     |
| `highway=secondary_link; cycleway=share_busway`         | ALL                    | 🚴     |
| `highway=primary; cycleway=share_busway`                | ALL                    | 🚴     |
| `highway=primary_link; cycleway=share_busway`           | ALL                    | 🚴     |
| `highway=trunk; cycleway=share_busway`                  | BICYCLE_AND_CAR        | 🚴     |
| `highway=trunk_link; cycleway=share_busway`             | BICYCLE_AND_CAR        | 🚴     |
| `highway=motorway; cycleway=share_busway`               | BICYCLE_AND_CAR        | 🚴     |
| `highway=motorway_link; cycleway=share_busway`          | BICYCLE_AND_CAR        | 🚴     |
| `present(highway); cycleway=opposite_lane`              | PEDESTRIAN_AND_BICYCLE | 🚴     |
| `highway=service; cycleway=opposite_lane`               | ALL                    | 🚴     |
| `highway=residential; cycleway=opposite_lane`           | ALL                    | 🚴     |
| `highway=residential_link; cycleway=opposite_lane`      | ALL                    | 🚴     |
| `highway=tertiary; cycleway=opposite_lane`              | ALL                    | 🚴     |
| `highway=tertiary_link; cycleway=opposite_lane`         | ALL                    | 🚴     |
| `highway=secondary; cycleway=opposite_lane`             | ALL                    | 🚴     |
| `highway=secondary_link; cycleway=opposite_lane`        | ALL                    | 🚴     |
| `highway=primary; cycleway=opposite_lane`               | ALL                    | 🚴     |
| `highway=primary_link; cycleway=opposite_lane`          | ALL                    | 🚴     |
| `highway=trunk; cycleway=opposite_lane`                 | BICYCLE_AND_CAR        | 🚴     |
| `highway=trunk_link; cycleway=opposite_lane`            | BICYCLE_AND_CAR        | 🚴     |
| `present(highway); cycleway=track`                      | PEDESTRIAN_AND_BICYCLE | 🚴     |
| `highway=service; cycleway=track`                       | ALL                    | 🚴     |
| `highway=residential; cycleway=track`                   | ALL                    | 🚴     |
| `highway=residential_link; cycleway=track`              | ALL                    | 🚴     |
| `highway=tertiary; cycleway=track`                      | ALL                    | 🚴     |
| `highway=tertiary_link; cycleway=track`                 | ALL                    | 🚴     |
| `highway=secondary; cycleway=track`                     | ALL                    | 🚴     |
| `highway=secondary_link; cycleway=track`                | ALL                    | 🚴     |
| `highway=primary; cycleway=track`                       | ALL                    | 🚴     |
| `highway=primary_link; cycleway=track`                  | ALL                    | 🚴     |
| `highway=trunk; cycleway=track`                         | BICYCLE_AND_CAR        | 🚴     |
| `highway=trunk_link; cycleway=track`                    | BICYCLE_AND_CAR        | 🚴     |
| `present(highway); cycleway=opposite_track`             | PEDESTRIAN_AND_BICYCLE | 🚴     |
| `highway=service; cycleway=opposite_track`              | ALL                    | 🚴     |
| `highway=residential; cycleway=opposite_track`          | ALL                    | 🚴     |
| `highway=residential_link; cycleway=opposite_track`     | ALL                    | 🚴     |
| `highway=tertiary; cycleway=opposite_track`             | ALL                    | 🚴     |
| `highway=tertiary_link; cycleway=opposite_track`        | ALL                    | 🚴     |
| `highway=secondary; cycleway=opposite_track`            | ALL                    | 🚴     |
| `highway=secondary_link; cycleway=opposite_track`       | ALL                    | 🚴     |
| `highway=primary; cycleway=opposite_track`              | ALL                    | 🚴     |
| `highway=primary_link; cycleway=opposite_track`         | ALL                    | 🚴     |
| `highway=trunk; cycleway=opposite_track`                | BICYCLE_AND_CAR        | 🚴     |
| `highway=trunk_link; cycleway=opposite_track`           | BICYCLE_AND_CAR        | 🚴     |
| `present(highway); cycleway=shared_lane`                | PEDESTRIAN_AND_BICYCLE | 🚴     |
| `highway=service; cycleway=shared_lane`                 | ALL                    | 🚴     |
| `highway=residential; cycleway=shared_lane`             | ALL                    | 🚴     |
| `highway=residential_link; cycleway=shared_lane`        | ALL                    | 🚴     |
| `highway=tertiary; cycleway=shared_lane`                | ALL                    | 🚴     |
| `highway=tertiary_link; cycleway=shared_lane`           | ALL                    | 🚴     |
| `highway=secondary; cycleway=shared_lane`               | ALL                    | 🚴     |
| `highway=secondary_link; cycleway=shared_lane`          | ALL                    | 🚴     |
| `highway=primary; cycleway=shared_lane`                 | ALL                    | 🚴     |
| `highway=primary_link; cycleway=shared_lane`            | ALL                    | 🚴     |
| `present(highway); cycleway=opposite`                   | PEDESTRIAN_AND_BICYCLE | 🚴     |
| `highway=service; cycleway=opposite`                    | ALL                    | 🚴     |
| `highway=residential; cycleway=opposite`                | ALL                    | 🚴     |
| `highway=residential_link; cycleway=opposite`           | ALL                    | 🚴     |
| `highway=tertiary; cycleway=opposite`                   | ALL                    |        |
| `highway=tertiary_link; cycleway=opposite`              | ALL                    |        |
| `highway=secondary; cycleway=opposite`                  | ALL                    | 🚴     |
| `highway=secondary_link; cycleway=opposite`             | ALL                    | 🚴     |
| `highway=primary; cycleway=opposite`                    | ALL                    | 🚴     |
| `highway=primary_link; cycleway=opposite`               | ALL                    | 🚴     |
| `highway=path; bicycle=designated`                      | PEDESTRIAN_AND_BICYCLE | 🚴     |
| `highway=footway; bicycle=designated`                   | PEDESTRIAN_AND_BICYCLE | 🚴     |
| `highway=footway; bicycle=yes; area=yes`                | PEDESTRIAN_AND_BICYCLE | 🚴     |
| `highway=pedestrian; bicycle=designated`                | PEDESTRIAN_AND_BICYCLE | 🚴     |
| `footway=sidewalk; highway=footway; bicycle=yes`        | PEDESTRIAN_AND_BICYCLE | 🚴     |
| `footway=sidewalk; highway=footway; bicycle=designated` | PEDESTRIAN_AND_BICYCLE | 🚴     |
| `highway=footway; footway=crossing`                     | PEDESTRIAN_AND_BICYCLE | 🚴     |
| `highway=footway; footway=crossing; bicycle=designated` | PEDESTRIAN_AND_BICYCLE | 🚴     |
| `highway=track; bicycle=yes`                            | PEDESTRIAN_AND_BICYCLE | 🚴     |
| `highway=track; bicycle=designated`                     | PEDESTRIAN_AND_BICYCLE | 🚴     |
| `highway=track; bicycle=yes; present(surface)`          | PEDESTRIAN_AND_BICYCLE | 🚴     |
| `highway=track; bicycle=designated; present(surface)`   | PEDESTRIAN_AND_BICYCLE | 🚴     |
| `highway=track; present(surface)`                       | PEDESTRIAN_AND_BICYCLE | 🚴     |
| `present(highway); bicycle=designated`                  | ALL                    | 🚴     |
| `highway=service; bicycle=designated`                   | ALL                    | 🚴     |
| `highway=residential; bicycle=designated`               | ALL                    | 🚴     |
| `highway=unclassified; bicycle=designated`              | ALL                    | 🚴     |
| `highway=residential_link; bicycle=designated`          | ALL                    | 🚴     |
| `highway=tertiary; bicycle=designated`                  | ALL                    | 🚴     |
| `highway=tertiary_link; bicycle=designated`             | ALL                    | 🚴     |
| `highway=secondary; bicycle=designated`                 | ALL                    | 🚴     |
| `highway=secondary_link; bicycle=designated`            | ALL                    | 🚴     |
| `highway=primary; bicycle=designated`                   | ALL                    | 🚴     |
| `highway=primary_link; bicycle=designated`              | ALL                    | 🚴     |
| `highway=trunk; bicycle=designated`                     | BICYCLE_AND_CAR        | 🚴     |
| `highway=trunk_link; bicycle=designated`                | BICYCLE_AND_CAR        | 🚴     |
| `highway=motorway; bicycle=designated`                  | BICYCLE_AND_CAR        | 🚴     |
| `highway=motorway_link; bicycle=designated`             | BICYCLE_AND_CAR        | 🚴     |

<!-- props END -->

### Bicycle and walking safety mixins

<!-- mixins BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

| matcher                                                    | modifications |
|------------------------------------------------------------|---------------|
| `highway=tertiary`                                         | 🚴            |
| `maxspeed=70`                                              | 🚴            |
| `maxspeed=80`                                              | 🚴            |
| `maxspeed=90`                                              | 🚴            |
| `maxspeed=100`                                             | 🚴            |
| `tracktype=grade1`                                         |               |
| `tracktype=grade2`                                         | 🚴            |
| `tracktype=grade3`                                         | 🚴            |
| `tracktype=grade4`                                         | 🚴            |
| `tracktype=grade5`                                         | 🚴            |
| `lit=no`                                                   | 🚴            |
| `lcn=yes¦rcn=yes¦ncn=yes¦bicycle_road=yes¦cyclestreet=yes` | 🚴            |
| `surface=unpaved`                                          | 🚴            |
| `surface=compacted`                                        | 🚴            |
| `surface=wood`                                             | 🚴            |
| `surface=cobblestone`                                      | 🚴            |
| `surface=sett`                                             | 🚴            |
| `surface=unhewn_cobblestone`                               | 🚴            |
| `surface=grass_paver`                                      | 🚴            |
| `surface=pebblestone`                                      | 🚴            |
| `surface=metal`                                            | 🚴            |
| `surface=ground`                                           | 🚴            |
| `surface=dirt`                                             | 🚴            |
| `surface=earth`                                            | 🚴            |
| `surface=grass`                                            | 🚴            |
| `surface=mud`                                              | 🚴            |
| `surface=woodchip`                                         | 🚴            |
| `surface=gravel`                                           | 🚴            |
| `surface=artifical_turf`                                   | 🚴            |
| `surface=sand`                                             | 🚴            |
| `rlis:bicycle=caution_area`                                | 🚴            |
| `rlis:bicycle:right=caution_area`                          | 🚴            |
| `rlis:bicycle:left=caution_area`                           | 🚴            |
| `ccgis:bicycle=caution_area`                               | 🚴            |
| `ccgis:bicycle:right=caution_area`                         | 🚴            |
| `ccgis:bicycle:left=caution_area`                          | 🚴            |
| `foot=discouraged`                                         | 🚶            |
| `bicycle=discouraged`                                      | 🚴            |
| `foot=use_sidepath`                                        | 🚶            |
| `bicycle=use_sidepath`                                     | 🚴            |

<!-- mixins END -->
