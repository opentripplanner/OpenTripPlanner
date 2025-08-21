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

| specifier                   | permission               | bike safety | walk safety |
|-----------------------------|--------------------------|-------------|-------------|
| `mtb:scale=3`               | `NONE`                   |             |             |
| `mtb:scale=4`               | `NONE`                   |             |             |
| `mtb:scale=5`               | `NONE`                   |             |             |
| `mtb:scale=6`               | `NONE`                   |             |             |
| `highway=bridleway`         | `NONE`                   | 1.3         | 1.3         |
| `highway=corridor`          | `PEDESTRIAN`             |             | 0.9         |
| `highway=steps`             | `PEDESTRIAN`             |             |             |
| `highway=crossing`          | `PEDESTRIAN`             |             | 0.9         |
| `highway=platform`          | `PEDESTRIAN`             |             |             |
| `public_transport=platform` | `PEDESTRIAN`             |             |             |
| `railway=platform`          | `PEDESTRIAN`             |             |             |
| `highway=pedestrian`        | `PEDESTRIAN`             | 0.9         | 0.8         |
| `highway=footway`           | `PEDESTRIAN`             | 1.1         | 0.8         |
| `mtb:scale=1`               | `PEDESTRIAN`             | 1.5         | 1.5         |
| `mtb:scale=2`               | `PEDESTRIAN`             | 3.0         | 3.0         |
| `highway=cycleway`          | `BICYCLE`                | 0.6         | 2.0         |
| `mtb:scale=0`               | `PEDESTRIAN_AND_BICYCLE` |             |             |
| `highway=path`              | `PEDESTRIAN_AND_BICYCLE` | 0.75        | 0.85        |
| `highway=living_street`     | `ALL`                    | 0.9         | 0.95        |
| `highway=unclassified`      | `ALL`                    |             |             |
| `highway=road`              | `ALL`                    |             |             |
| `highway=byway`             | `ALL`                    | 1.3         | 1.3         |
| `highway=track`             | `ALL`                    | 1.3         | 1.3         |
| `highway=service`           | `ALL`                    | 1.1         | 1.1         |
| `highway=residential`       | `ALL`                    | 0.98        | 0.98        |
| `highway=residential_link`  | `ALL`                    | 0.98        | 0.98        |
| `highway=tertiary`          | `ALL`                    |             |             |
| `highway=tertiary_link`     | `ALL`                    |             |             |
| `highway=secondary`         | `ALL`                    | 1.5         | 1.5         |
| `highway=secondary_link`    | `ALL`                    | 1.5         | 1.5         |
| `highway=primary`           | `ALL`                    | 2.06        | 2.06        |
| `highway=primary_link`      | `ALL`                    | 2.06        | 2.06        |
| `highway=trunk`             | `ALL`                    | 7.47        | 7.47        |
| `highway=trunk_link`        | `ALL`                    | 2.06        | 7.47        |
| `highway=motorway_link`     | `CAR`                    | 2.06        |             |
| `highway=motorway`          | `CAR`                    | 8.0         |             |

<!-- props END -->

### Safety mixins

Mixins are selectors that have only an effect on the bicycle and walk safety factors but not on the
permission of an OSM way. Their safety values are multiplied with the base values from the selected
way properties. Multiple mixins can apply to the same way and their effects compound.

<!-- mixins BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

| matcher                                                                                                                                                                                                                                                                                   | add permission    | remove permission      | bicycle safety                                      | walk safety |
|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------|------------------------|-----------------------------------------------------|-------------|
| `motorroad=yes`                                                                                                                                                                                                                                                                           |                   | PEDESTRIAN_AND_BICYCLE |                                                     |             |
| `cycleway=lane; not(highway=cycleway)`                                                                                                                                                                                                                                                    | BICYCLE           |                        | 0.87                                                |             |
| `cycleway=share_busway; not(highway=cycleway)`                                                                                                                                                                                                                                            | BICYCLE           |                        | 0.92                                                |             |
| `cycleway=opposite_lane; not(highway=cycleway)`                                                                                                                                                                                                                                           | backward: BICYCLE |                        | no direction: 1.0 <br> forward: 1.0 <br> back: 0.87 |             |
| `cycleway=track; not(highway=cycleway)`                                                                                                                                                                                                                                                   | BICYCLE           |                        | 0.75                                                |             |
| `cycleway=opposite_track; not(highway=cycleway)`                                                                                                                                                                                                                                          | backward: BICYCLE |                        | no direction: 1.0 <br> forward: 1.0 <br> back: 0.75 |             |
| `cycleway=shared_lane; not(highway=cycleway)`                                                                                                                                                                                                                                             | BICYCLE           |                        | 0.77                                                |             |
| `cycleway=opposite; not(highway=cycleway)`                                                                                                                                                                                                                                                | backward: BICYCLE |                        | no direction: 1.0 <br> forward: 1.0 <br> back: 1.4  |             |
| `foot=designated; not(highway=footway); not(highway=pedestrian); not(highway=path)`                                                                                                                                                                                                       |                   |                        |                                                     | 0.9         |
| `footway=sidewalk`                                                                                                                                                                                                                                                                        |                   |                        | 2.5                                                 |             |
| `footway=crossing`                                                                                                                                                                                                                                                                        |                   |                        | 2.5                                                 |             |
| `bicycle=designated; cycleway not one of [no, none] or absent; not(highway=cycleway); not(lcn=yes); not(rcn=yes); not(ncn=yes); not(bicycle_road=yes); not(cyclestreet=yes)`                                                                                                              |                   |                        | 0.8                                                 |             |
| `lcn=yes¦rcn=yes¦ncn=yes¦bicycle_road=yes¦cyclestreet=yes`                                                                                                                                                                                                                                |                   |                        | 0.7                                                 |             |
| `sidewalk=yes; not(highway one of [footway, pedestrian, path, trunk])¦sidewalk=left; not(highway one of [footway, pedestrian, path, trunk])¦sidewalk=right; not(highway one of [footway, pedestrian, path, trunk])¦sidewalk=both; not(highway one of [footway, pedestrian, path, trunk])` |                   |                        |                                                     | 0.9         |
| `highway=trunk; sidewalk=yes¦highway=trunk; sidewalk=left¦highway=trunk; sidewalk=right¦highway=trunk; sidewalk=both¦highway=trunk_link; sidewalk=yes¦highway=trunk_link; sidewalk=left¦highway=trunk_link; sidewalk=right¦highway=trunk_link; sidewalk=both`                             |                   |                        |                                                     | 0.4         |
| `sidewalk=lane; not(highway one of [footway, pedestrian, path])`                                                                                                                                                                                                                          |                   |                        |                                                     | 0.95        |
| `surface=unpaved`                                                                                                                                                                                                                                                                         |                   |                        | 1.18                                                |             |
| `surface=compacted`                                                                                                                                                                                                                                                                       |                   |                        | 1.18                                                |             |
| `surface=wood`                                                                                                                                                                                                                                                                            |                   |                        | 1.18                                                |             |
| `surface=cobblestone`                                                                                                                                                                                                                                                                     |                   |                        | 1.3                                                 |             |
| `surface=sett`                                                                                                                                                                                                                                                                            |                   |                        | 1.3                                                 |             |
| `surface=unhewn_cobblestone`                                                                                                                                                                                                                                                              |                   |                        | 1.5                                                 |             |
| `surface=grass_paver`                                                                                                                                                                                                                                                                     |                   |                        | 1.3                                                 |             |
| `surface=pebblestone`                                                                                                                                                                                                                                                                     |                   |                        | 1.3                                                 |             |
| `surface=metal`                                                                                                                                                                                                                                                                           |                   |                        | 1.3                                                 |             |
| `surface=ground`                                                                                                                                                                                                                                                                          |                   |                        | 1.5                                                 |             |
| `surface=dirt`                                                                                                                                                                                                                                                                            |                   |                        | 1.5                                                 |             |
| `surface=earth`                                                                                                                                                                                                                                                                           |                   |                        | 1.5                                                 |             |
| `surface=grass`                                                                                                                                                                                                                                                                           |                   |                        | 1.5                                                 |             |
| `surface=mud`                                                                                                                                                                                                                                                                             |                   |                        | 1.5                                                 |             |
| `surface=woodchip`                                                                                                                                                                                                                                                                        |                   |                        | 1.5                                                 |             |
| `surface=gravel`                                                                                                                                                                                                                                                                          |                   |                        | 1.5                                                 |             |
| `surface=artifical_turf`                                                                                                                                                                                                                                                                  |                   |                        | 1.5                                                 |             |
| `surface=sand`                                                                                                                                                                                                                                                                            |                   |                        | 100.0                                               |             |
| `foot=discouraged`                                                                                                                                                                                                                                                                        |                   |                        |                                                     | 3.0         |
| `bicycle=discouraged`                                                                                                                                                                                                                                                                     |                   |                        | 3.0                                                 |             |
| `foot=use_sidepath`                                                                                                                                                                                                                                                                       |                   |                        |                                                     | 5.0         |
| `bicycle=use_sidepath`                                                                                                                                                                                                                                                                    |                   |                        | 5.0                                                 |             |

<!-- mixins END -->
