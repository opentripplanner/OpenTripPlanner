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
Lower safety values make an OSM way more desirable and higher values less desirable. They are
applied as a multiplier to the traversal cost when safety is enabled. As the router minimizes the
total cost of traversal, this results in choosing a path with lower safety values, however if the
only paths available are of high safety values, it acts as a further reluctance for walking and
cycling compared to taking transit when safety is enabled.

How the safety values work was changed between versions 2.8 and 2.9. Before the change, a safety
normalizer would multiply the values set by the mapper such that the most desirable way had a safety
factor of 1.0, so that the use of safety would only further increase, but not decrease. the reluctance. 
This was found to be problematic because of two reasons:

- Cycling was seen to be more undesirable than expected, even on a quiet residential way, because
there are certain combinations of tags in the map which produced exceptionally low safety values.
For more details, see [#6775](https://github.com/opentripplanner/OpenTripPlanner/issues/6775).
- The effect of the values set in the tag mappers were unpredictable, as it depended on one single
way which could be anywhere in the map. Increasing or decreasing the map coverage could change the
effect on the same way.

The normalizer has been removed in version 2.9, so the safety values are applied directly without
change. As a result, some of the values in the tag mappers have also changed to compensate for the 
effect that they will not be multiplied further.

For details, see [#6782](https://github.com/opentripplanner/OpenTripPlanner/pull/6782).

<!-- props BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

| specifier                   | permission               | bike safety | walk safety |
|-----------------------------|--------------------------|-------------|-------------|
| `mtb:scale=3`               | `NONE`                   |             |             |
| `mtb:scale=4`               | `NONE`                   |             |             |
| `mtb:scale=5`               | `NONE`                   |             |             |
| `mtb:scale=6`               | `NONE`                   |             |             |
| `highway=bridleway`         | `NONE`                   | 1.3         |             |
| `highway=corridor`          | `PEDESTRIAN`             |             |             |
| `highway=steps`             | `PEDESTRIAN`             |             |             |
| `highway=elevator`          | `PEDESTRIAN`             |             |             |
| `highway=crossing`          | `PEDESTRIAN`             |             |             |
| `highway=platform`          | `PEDESTRIAN`             |             |             |
| `public_transport=platform` | `PEDESTRIAN`             |             |             |
| `railway=platform`          | `PEDESTRIAN`             |             |             |
| `highway=pedestrian`        | `PEDESTRIAN`             | 0.9         |             |
| `highway=footway`           | `PEDESTRIAN`             | 1.1         |             |
| `mtb:scale=1`               | `PEDESTRIAN`             | 1.5         |             |
| `mtb:scale=2`               | `PEDESTRIAN`             | 3.0         |             |
| `indoor=area`               | `PEDESTRIAN`             |             |             |
| `indoor=corridor`           | `PEDESTRIAN`             |             |             |
| `highway=cycleway`          | `BICYCLE`                | 0.6         |             |
| `mtb:scale=0`               | `PEDESTRIAN_AND_BICYCLE` |             |             |
| `highway=path`              | `PEDESTRIAN_AND_BICYCLE` | 0.75        |             |
| `highway=living_street`     | `ALL`                    | 0.9         |             |
| `highway=unclassified`      | `ALL`                    |             |             |
| `highway=road`              | `ALL`                    |             |             |
| `highway=byway`             | `ALL`                    | 1.3         |             |
| `highway=track`             | `ALL`                    | 1.3         |             |
| `highway=service`           | `ALL`                    | 1.1         |             |
| `highway=residential`       | `ALL`                    | 0.98        |             |
| `highway=residential_link`  | `ALL`                    | 0.98        |             |
| `highway=tertiary`          | `ALL`                    |             |             |
| `highway=tertiary_link`     | `ALL`                    |             |             |
| `highway=secondary`         | `ALL`                    | 1.5         |             |
| `highway=secondary_link`    | `ALL`                    | 1.5         |             |
| `highway=primary`           | `ALL`                    | 2.06        |             |
| `highway=primary_link`      | `ALL`                    | 2.06        |             |
| `highway=trunk`             | `ALL`                    | 7.47        | 7.47        |
| `highway=trunk_link`        | `ALL`                    | 2.06        | 7.47        |
| `highway=motorway_link`     | `CAR`                    | 2.06        |             |
| `highway=motorway`          | `CAR`                    | 8.0         |             |

<!-- props END -->

### Safety mixins

Mixins are selectors that have an effect on the bicycle and walk safety factors. 
Their safety values are multiplied with the base values from the selected way properties.

Mixins can also add or remove permissions on an OSM way, which will be further overridden with
explicitly set permission tags. If two mixins add and remove the same permission on the same way,
the behavior is unspecified which usually indicates a tagging error on the way.

Multiple mixins can apply to the same way and their effects compound.

<!-- mixins BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

| matcher                                                                                                                                                                      | add permission    | remove permission      | bicycle safety                                      | walk safety |
|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------|------------------------|-----------------------------------------------------|-------------|
| `motorroad=yes`                                                                                                                                                              |                   | PEDESTRIAN_AND_BICYCLE |                                                     |             |
| `cycleway=lane; not(highway=cycleway)`                                                                                                                                       | BICYCLE           |                        | 0.87                                                |             |
| `cycleway=share_busway; not(highway=cycleway)`                                                                                                                               | BICYCLE           |                        | 0.92                                                |             |
| `cycleway=opposite_lane; not(highway=cycleway)`                                                                                                                              | backward: BICYCLE |                        | no direction: 1.0 <br> forward: 1.0 <br> back: 0.87 |             |
| `cycleway=track; not(highway=cycleway)`                                                                                                                                      | BICYCLE           |                        | 0.75                                                |             |
| `cycleway=opposite_track; not(highway=cycleway)`                                                                                                                             | backward: BICYCLE |                        | no direction: 1.0 <br> forward: 1.0 <br> back: 0.75 |             |
| `cycleway=shared_lane; not(highway=cycleway)`                                                                                                                                | BICYCLE           |                        | 0.77                                                |             |
| `cycleway=opposite; not(highway=cycleway)`                                                                                                                                   | backward: BICYCLE |                        | no direction: 1.0 <br> forward: 1.0 <br> back: 1.4  |             |
| `footway=sidewalk`                                                                                                                                                           |                   |                        | 2.5                                                 |             |
| `footway=crossing`                                                                                                                                                           |                   |                        | 1.5                                                 |             |
| `bicycle=designated; cycleway not one of [no, none] or absent; not(highway=cycleway); not(lcn=yes); not(rcn=yes); not(ncn=yes); not(bicycle_road=yes); not(cyclestreet=yes)` |                   |                        | 0.8                                                 |             |
| `lcn=yes¦rcn=yes¦ncn=yes¦bicycle_road=yes¦cyclestreet=yes`                                                                                                                   |                   |                        | 0.7                                                 |             |
| `highway=trunk; sidewalk=yes¦highway=trunk; sidewalk=left¦highway=trunk; sidewalk=right¦highway=trunk; sidewalk=both`                                                        |                   |                        |                                                     | 0.25        |
| `highway=trunk; sidewalk=lane`                                                                                                                                               |                   |                        |                                                     | 0.6         |
| `surface=unpaved`                                                                                                                                                            |                   |                        | 1.18                                                |             |
| `surface=compacted`                                                                                                                                                          |                   |                        | 1.18                                                |             |
| `surface=wood`                                                                                                                                                               |                   |                        | 1.18                                                |             |
| `surface=cobblestone`                                                                                                                                                        |                   |                        | 1.3                                                 |             |
| `surface=sett`                                                                                                                                                               |                   |                        | 1.3                                                 |             |
| `surface=unhewn_cobblestone`                                                                                                                                                 |                   |                        | 1.5                                                 |             |
| `surface=grass_paver`                                                                                                                                                        |                   |                        | 1.3                                                 |             |
| `surface=pebblestone`                                                                                                                                                        |                   |                        | 1.3                                                 |             |
| `surface=metal`                                                                                                                                                              |                   |                        | 1.3                                                 |             |
| `surface=ground`                                                                                                                                                             |                   |                        | 1.5                                                 |             |
| `surface=dirt`                                                                                                                                                               |                   |                        | 1.5                                                 |             |
| `surface=earth`                                                                                                                                                              |                   |                        | 1.5                                                 |             |
| `surface=grass`                                                                                                                                                              |                   |                        | 1.5                                                 |             |
| `surface=mud`                                                                                                                                                                |                   |                        | 1.5                                                 |             |
| `surface=woodchip`                                                                                                                                                           |                   |                        | 1.5                                                 |             |
| `surface=gravel`                                                                                                                                                             |                   |                        | 1.5                                                 |             |
| `surface=artifical_turf`                                                                                                                                                     |                   |                        | 1.5                                                 |             |
| `surface=sand`                                                                                                                                                               |                   |                        | 100.0                                               |             |
| `foot=discouraged`                                                                                                                                                           |                   |                        |                                                     | 3.0         |
| `bicycle=discouraged`                                                                                                                                                        |                   |                        | 3.0                                                 |             |
| `foot=use_sidepath`                                                                                                                                                          |                   |                        |                                                     | 5.0         |
| `bicycle=use_sidepath`                                                                                                                                                       |                   |                        | 5.0                                                 |             |

<!-- mixins END -->
