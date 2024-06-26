## OSM tag mapping

### Way properties

Way properties set a way's permission and optionally influences its walk and bicycle safety factors.
These factors determine how desirable an OSM way is when routing for cyclists and pedestrians.

<!-- props BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

| specifier                                                                                                                                                  | permission             | safety |
|------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------|--------|
| `highway one of [motorway, motorway_link]`                                                                                                                 | CAR                    |        |
| `highway one of [trunk, trunk_link, primary, primary_link]; motorroad=yes`                                                                                 | CAR                    |        |
| `highway one of [trunk, trunk_link, primary, primary_link, secondary, secondary_link, tertiary, tertiary_link, unclassified, residential]`                 | ALL                    |        |
| `cycleway=track; highway one of [trunk, trunk_link, primary, primary_link, secondary, secondary_link, tertiary, tertiary_link, unclassified, residential]` | ALL                    |        |
| `cycleway=lane; highway one of [trunk, trunk_link, primary, primary_link, secondary, secondary_link, tertiary, tertiary_link]`                             | ALL                    | 🚴     |
| `cycleway=lane; maxspeed < 50; highway one of [trunk, trunk_link, primary, primary_link, secondary, secondary_link, tertiary, tertiary_link]`              | ALL                    | 🚴     |
| `cycleway=lane; highway one of [unclassified, residential]`                                                                                                | ALL                    | 🚴     |
| `highway=service`                                                                                                                                          | ALL                    |        |
| `highway=service; service=parking_aisle`                                                                                                                   | ALL                    | 🚴     |
| `highway=service; service=drive-through`                                                                                                                   | ALL                    | 🚴     |
| `highway=living_street`                                                                                                                                    | ALL                    | 🚴     |
| `highway=pedestrian`                                                                                                                                       | PEDESTRIAN_AND_BICYCLE | 🚴     |
| `highway=busway`                                                                                                                                           | PEDESTRIAN_AND_BICYCLE | 🚴 🚶  |
| `highway=service; bus one of [yes, designated]`                                                                                                            | PEDESTRIAN_AND_BICYCLE | 🚴 🚶  |
| `highway=footway`                                                                                                                                          | PEDESTRIAN_AND_BICYCLE | 🚴     |
| `highway=cycleway`                                                                                                                                         | PEDESTRIAN_AND_BICYCLE | 🚴 🚶  |
| `highway=cycleway; lanes > 1`                                                                                                                              | PEDESTRIAN_AND_BICYCLE | 🚶     |
| `highway=cycleway; oneway=yes`                                                                                                                             | PEDESTRIAN_AND_BICYCLE | 🚶     |
| `highway=cycleway; sidewalk one of [yes, left, right, both]`                                                                                               | PEDESTRIAN_AND_BICYCLE | 🚴     |
| `highway=cycleway; lanes > 1; sidewalk one of [yes, left, right, both]`                                                                                    | PEDESTRIAN_AND_BICYCLE |        |
| `highway=cycleway; oneway=yes; sidewalk one of [yes, left, right, both]`                                                                                   | PEDESTRIAN_AND_BICYCLE |        |
| `highway=cycleway; foot=designated; segregated=no`                                                                                                         | PEDESTRIAN_AND_BICYCLE | 🚴 🚶  |
| `highway=path; foot=designated; bicycle=designated; segregated=no`                                                                                         | PEDESTRIAN_AND_BICYCLE | 🚴 🚶  |
| `highway=cycleway; foot=designated; segregated=yes`                                                                                                        | PEDESTRIAN_AND_BICYCLE | 🚴     |
| `highway=path; foot=designated; bicycle=designated; segregated=yes`                                                                                        | PEDESTRIAN_AND_BICYCLE | 🚴     |
| `highway=cycleway; foot=designated; segregated=yes; lanes > 1`                                                                                             | PEDESTRIAN_AND_BICYCLE |        |
| `highway=cycleway; foot=designated; present(segregated); motor_vehicle=destination`                                                                        | PEDESTRIAN_AND_BICYCLE | 🚴     |
| `highway=path; foot=designated; bicycle=designated; present(segregated); motor_vehicle=destination`                                                        | PEDESTRIAN_AND_BICYCLE | 🚴     |
| `highway=footway; footway=sidewalk`                                                                                                                        | PEDESTRIAN_AND_BICYCLE | 🚴 🚶  |
| `highway=footway; footway=crossing`                                                                                                                        | PEDESTRIAN_AND_BICYCLE | 🚴 🚶  |
| `highway=cycleway; cycleway=crossing`                                                                                                                      | PEDESTRIAN_AND_BICYCLE | 🚴 🚶  |
| `highway=track`                                                                                                                                            | PEDESTRIAN_AND_BICYCLE |        |
| `highway=bridleway`                                                                                                                                        | PEDESTRIAN_AND_BICYCLE |        |
| `highway=path`                                                                                                                                             | PEDESTRIAN_AND_BICYCLE |        |
| `highway=steps`                                                                                                                                            | PEDESTRIAN             |        |
| `highway=corridor`                                                                                                                                         | PEDESTRIAN             |        |
| `highway=footway; indoor=yes`                                                                                                                              | PEDESTRIAN             |        |
| `highway=platform`                                                                                                                                         | PEDESTRIAN             |        |
| `public_transport=platform`                                                                                                                                | PEDESTRIAN             |        |
| `trail_visibility one of [bad, low, poor, horrible, no]; highway=path`                                                                                     | NONE                   |        |
| `sac_scale one of [demanding_mountain_hiking, alpine_hiking, demanding_alpine_hiking, difficult_alpine_hiking]; highway one of [path, steps]`              | NONE                   |        |
| `smoothness one of [horrible, very_horrible]; highway one of [path, bridleway, track]`                                                                     | PEDESTRIAN             | 🚶     |
| `smoothness=impassable; highway one of [path, bridleway, track]`                                                                                           | NONE                   |        |
| `1 > mtb:scale < 2; highway one of [path, bridleway, track]`                                                                                               | PEDESTRIAN             | 🚶     |
| `mtb:scale > 2; highway one of [path, bridleway, track]`                                                                                                   | NONE                   |        |

<!-- props END -->

<!-- prop-details BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

<h3 id="0">Rule #0</h3>

**Specifier:** `highway one of [motorway, motorway_link]`   
**Permission:** CAR   
**Bike safety factor:** forward: 1.0, back: 1.0   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="1">Rule #1</h3>

**Specifier:** `highway one of [trunk, trunk_link, primary, primary_link]; motorroad=yes`   
**Permission:** CAR   
**Bike safety factor:** forward: 1.0, back: 1.0   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="2">Rule #2</h3>

**Specifier:** `highway one of [trunk, trunk_link, primary, primary_link, secondary, secondary_link, tertiary, tertiary_link, unclassified, residential]`   
**Permission:** ALL   
**Bike safety factor:** forward: 1.0, back: 1.0   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="3">Rule #3</h3>

**Specifier:** `cycleway=track; highway one of [trunk, trunk_link, primary, primary_link, secondary, secondary_link, tertiary, tertiary_link, unclassified, residential]`   
**Permission:** ALL   
**Bike safety factor:** forward: 1.0, back: 1.0   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="4">Rule #4</h3>

**Specifier:** `cycleway=lane; highway one of [trunk, trunk_link, primary, primary_link, secondary, secondary_link, tertiary, tertiary_link]`   
**Permission:** ALL   
**Bike safety factor:** forward: 1.27, back: 1.27   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="5">Rule #5</h3>

**Specifier:** `cycleway=lane; maxspeed < 50; highway one of [trunk, trunk_link, primary, primary_link, secondary, secondary_link, tertiary, tertiary_link]`   
**Permission:** ALL   
**Bike safety factor:** forward: 1.1, back: 1.1   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="6">Rule #6</h3>

**Specifier:** `cycleway=lane; highway one of [unclassified, residential]`   
**Permission:** ALL   
**Bike safety factor:** forward: 1.1, back: 1.1   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="7">Rule #7</h3>

**Specifier:** `highway=service`   
**Permission:** ALL   
**Bike safety factor:** forward: 1.0, back: 1.0   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="8">Rule #8</h3>

**Specifier:** `highway=service; service=parking_aisle`   
**Permission:** ALL   
**Bike safety factor:** forward: 2.5, back: 2.5   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="9">Rule #9</h3>

**Specifier:** `highway=service; service=drive-through`   
**Permission:** ALL   
**Bike safety factor:** forward: 2.5, back: 2.5   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="10">Rule #10</h3>

**Specifier:** `highway=living_street`   
**Permission:** ALL   
**Bike safety factor:** forward: 1.83, back: 1.83   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="11">Rule #11</h3>

**Specifier:** `highway=pedestrian`   
**Permission:** PEDESTRIAN_AND_BICYCLE   
**Bike safety factor:** forward: 1.2, back: 1.2   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="12">Rule #12</h3>

**Specifier:** `highway=busway`   
**Permission:** PEDESTRIAN_AND_BICYCLE   
**Bike safety factor:** forward: 2.37, back: 2.37   
**Walk safety factor:** forward: 1.9, back: 1.9 

<h3 id="13">Rule #13</h3>

**Specifier:** `highway=service; bus one of [yes, designated]`   
**Permission:** PEDESTRIAN_AND_BICYCLE   
**Bike safety factor:** forward: 2.37, back: 2.37   
**Walk safety factor:** forward: 1.9, back: 1.9 

<h3 id="14">Rule #14</h3>

**Specifier:** `highway=footway`   
**Permission:** PEDESTRIAN_AND_BICYCLE   
**Bike safety factor:** forward: 1.42, back: 1.42   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="15">Rule #15</h3>

**Specifier:** `highway=cycleway`   
**Permission:** PEDESTRIAN_AND_BICYCLE   
**Bike safety factor:** forward: 1.05, back: 1.05   
**Walk safety factor:** forward: 1.4, back: 1.4 

<h3 id="16">Rule #16</h3>

**Specifier:** `highway=cycleway; lanes > 1`   
**Permission:** PEDESTRIAN_AND_BICYCLE   
**Bike safety factor:** forward: 1.0, back: 1.0   
**Walk safety factor:** forward: 1.4, back: 1.4 

<h3 id="17">Rule #17</h3>

**Specifier:** `highway=cycleway; oneway=yes`   
**Permission:** PEDESTRIAN_AND_BICYCLE   
**Bike safety factor:** forward: 1.0, back: 1.0   
**Walk safety factor:** forward: 1.4, back: 1.4 

<h3 id="18">Rule #18</h3>

**Specifier:** `highway=cycleway; sidewalk one of [yes, left, right, both]`   
**Permission:** PEDESTRIAN_AND_BICYCLE   
**Bike safety factor:** forward: 1.05, back: 1.05   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="19">Rule #19</h3>

**Specifier:** `highway=cycleway; lanes > 1; sidewalk one of [yes, left, right, both]`   
**Permission:** PEDESTRIAN_AND_BICYCLE   
**Bike safety factor:** forward: 1.0, back: 1.0   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="20">Rule #20</h3>

**Specifier:** `highway=cycleway; oneway=yes; sidewalk one of [yes, left, right, both]`   
**Permission:** PEDESTRIAN_AND_BICYCLE   
**Bike safety factor:** forward: 1.0, back: 1.0   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="21">Rule #21</h3>

**Specifier:** `highway=cycleway; foot=designated; segregated=no`   
**Permission:** PEDESTRIAN_AND_BICYCLE   
**Bike safety factor:** forward: 1.05, back: 1.05   
**Walk safety factor:** forward: 1.15, back: 1.15 

<h3 id="22">Rule #22</h3>

**Specifier:** `highway=path; foot=designated; bicycle=designated; segregated=no`   
**Permission:** PEDESTRIAN_AND_BICYCLE   
**Bike safety factor:** forward: 1.05, back: 1.05   
**Walk safety factor:** forward: 1.15, back: 1.15 

<h3 id="23">Rule #23</h3>

**Specifier:** `highway=cycleway; foot=designated; segregated=yes`   
**Permission:** PEDESTRIAN_AND_BICYCLE   
**Bike safety factor:** forward: 1.05, back: 1.05   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="24">Rule #24</h3>

**Specifier:** `highway=path; foot=designated; bicycle=designated; segregated=yes`   
**Permission:** PEDESTRIAN_AND_BICYCLE   
**Bike safety factor:** forward: 1.05, back: 1.05   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="25">Rule #25</h3>

**Specifier:** `highway=cycleway; foot=designated; segregated=yes; lanes > 1`   
**Permission:** PEDESTRIAN_AND_BICYCLE   
**Bike safety factor:** forward: 1.0, back: 1.0   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="26">Rule #26</h3>

**Specifier:** `highway=cycleway; foot=designated; present(segregated); motor_vehicle=destination`   
**Permission:** PEDESTRIAN_AND_BICYCLE   
**Bike safety factor:** forward: 1.57, back: 1.57   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="27">Rule #27</h3>

**Specifier:** `highway=path; foot=designated; bicycle=designated; present(segregated); motor_vehicle=destination`   
**Permission:** PEDESTRIAN_AND_BICYCLE   
**Bike safety factor:** forward: 1.57, back: 1.57   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="28">Rule #28</h3>

**Specifier:** `highway=footway; footway=sidewalk`   
**Permission:** PEDESTRIAN_AND_BICYCLE   
**Bike safety factor:** forward: 1.93, back: 1.93   
**Walk safety factor:** forward: 1.1, back: 1.1 

<h3 id="29">Rule #29</h3>

**Specifier:** `highway=footway; footway=crossing`   
**Permission:** PEDESTRIAN_AND_BICYCLE   
**Bike safety factor:** forward: 2.33, back: 2.33   
**Walk safety factor:** forward: 1.35, back: 1.35 

<h3 id="30">Rule #30</h3>

**Specifier:** `highway=cycleway; cycleway=crossing`   
**Permission:** PEDESTRIAN_AND_BICYCLE   
**Bike safety factor:** forward: 2.33, back: 2.33   
**Walk safety factor:** forward: 1.35, back: 1.35 

<h3 id="31">Rule #31</h3>

**Specifier:** `highway=track`   
**Permission:** PEDESTRIAN_AND_BICYCLE   
**Bike safety factor:** forward: 1.0, back: 1.0   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="32">Rule #32</h3>

**Specifier:** `highway=bridleway`   
**Permission:** PEDESTRIAN_AND_BICYCLE   
**Bike safety factor:** forward: 1.0, back: 1.0   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="33">Rule #33</h3>

**Specifier:** `highway=path`   
**Permission:** PEDESTRIAN_AND_BICYCLE   
**Bike safety factor:** forward: 1.0, back: 1.0   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="34">Rule #34</h3>

**Specifier:** `highway=steps`   
**Permission:** PEDESTRIAN   
**Bike safety factor:** forward: 1.0, back: 1.0   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="35">Rule #35</h3>

**Specifier:** `highway=corridor`   
**Permission:** PEDESTRIAN   
**Bike safety factor:** forward: 1.0, back: 1.0   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="36">Rule #36</h3>

**Specifier:** `highway=footway; indoor=yes`   
**Permission:** PEDESTRIAN   
**Bike safety factor:** forward: 1.0, back: 1.0   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="37">Rule #37</h3>

**Specifier:** `highway=platform`   
**Permission:** PEDESTRIAN   
**Bike safety factor:** forward: 1.0, back: 1.0   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="38">Rule #38</h3>

**Specifier:** `public_transport=platform`   
**Permission:** PEDESTRIAN   
**Bike safety factor:** forward: 1.0, back: 1.0   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="39">Rule #39</h3>

**Specifier:** `trail_visibility one of [bad, low, poor, horrible, no]; highway=path`   
**Permission:** NONE   
**Bike safety factor:** forward: 1.0, back: 1.0   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="40">Rule #40</h3>

**Specifier:** `sac_scale one of [demanding_mountain_hiking, alpine_hiking, demanding_alpine_hiking, difficult_alpine_hiking]; highway one of [path, steps]`   
**Permission:** NONE   
**Bike safety factor:** forward: 1.0, back: 1.0   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="41">Rule #41</h3>

**Specifier:** `smoothness one of [horrible, very_horrible]; highway one of [path, bridleway, track]`   
**Permission:** PEDESTRIAN   
**Bike safety factor:** forward: 1.0, back: 1.0   
**Walk safety factor:** forward: 1.15, back: 1.15 

<h3 id="42">Rule #42</h3>

**Specifier:** `smoothness=impassable; highway one of [path, bridleway, track]`   
**Permission:** NONE   
**Bike safety factor:** forward: 1.0, back: 1.0   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="43">Rule #43</h3>

**Specifier:** `1 > mtb:scale < 2; highway one of [path, bridleway, track]`   
**Permission:** PEDESTRIAN   
**Bike safety factor:** forward: 1.0, back: 1.0   
**Walk safety factor:** forward: 1.15, back: 1.15 

<h3 id="44">Rule #44</h3>

**Specifier:** `mtb:scale > 2; highway one of [path, bridleway, track]`   
**Permission:** NONE   
**Bike safety factor:** forward: 1.0, back: 1.0   
**Walk safety factor:** forward: 1.0, back: 1.0 


<!-- prop-details END -->

### Bicycle and walking safety mixins

Mixins are selectors that have only an effect on the bicycle and walk safety factors but not on the
permission of an OSM way. Multiple mixins can apply to the same way and their effects compound.

<!-- mixins BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

| matcher                                                                                                                                                                                                                                                                                                                                                                     | bicycle safety               | walk safety |
|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------|-------------|
| `cycleway=shared_lane; highway one of [trunk, trunk_link, primary, primary_link, secondary, secondary_link, tertiary, tertiary_link, unclassified, residential]`                                                                                                                                                                                                            | 0.85                         |             |
| `lcn=yes¦rcn=yes¦ncn=yes`                                                                                                                                                                                                                                                                                                                                                   | 0.85                         |             |
| `oneway=yes; cycleway not one of [no, none] or absent; highway one of [trunk, trunk_link, primary, primary_link, secondary, secondary_link, tertiary, tertiary_link, unclassified, residential]`                                                                                                                                                                            | forward: 1.0 <br> back: 1.15 |             |
| `embedded_rails one of [tram, light_rail, disused]`                                                                                                                                                                                                                                                                                                                         | 1.2                          |             |
| `tunnel=yes; highway one of [trunk, trunk_link, primary, primary_link, secondary, secondary_link, tertiary, tertiary_link, unclassified]`                                                                                                                                                                                                                                   |                              | 2.0         |
| `bridge=yes; sidewalk not one of [no, separate] or absent; highway one of [trunk, trunk_link, primary, primary_link, secondary, secondary_link, tertiary, tertiary_link, unclassified]¦verge=no; sidewalk not one of [no, separate] or absent; highway one of [trunk, trunk_link, primary, primary_link, secondary, secondary_link, tertiary, tertiary_link, unclassified]` |                              | 2.0         |
| `junction=roundabout; sidewalk not one of [no, separate] or absent`                                                                                                                                                                                                                                                                                                         |                              | 2.0         |
| `surface=grass_paver`                                                                                                                                                                                                                                                                                                                                                       | 1.2                          |             |
| `surface=sett`                                                                                                                                                                                                                                                                                                                                                              | 1.2                          |             |
| `surface=cobblestone`                                                                                                                                                                                                                                                                                                                                                       | 1.2                          |             |
| `surface=unhewn_cobblestone`                                                                                                                                                                                                                                                                                                                                                | 3.0                          |             |
| `surface=metal_grid`                                                                                                                                                                                                                                                                                                                                                        | 1.2                          |             |
| `surface=metal`                                                                                                                                                                                                                                                                                                                                                             | 1.2                          |             |
| `smoothness=intermediate; surface one of [asfalt, concrete, paving_stones, paved, wood]`                                                                                                                                                                                                                                                                                    | 1.2                          |             |
| `smoothness=bad; surface one of [asfalt, concrete, paving_stones, paved, wood]`                                                                                                                                                                                                                                                                                             | 1.4                          | 1.6         |
| `surface=unpaved; !tracktype`                                                                                                                                                                                                                                                                                                                                               | 1.8                          | 1.6         |
| `surface=compacted`                                                                                                                                                                                                                                                                                                                                                         | 1.4                          | 1.4         |
| `surface=fine_gravel`                                                                                                                                                                                                                                                                                                                                                       | 1.8                          | 1.6         |
| `surface=pebblestone`                                                                                                                                                                                                                                                                                                                                                       | 1.8                          | 1.6         |
| `surface=gravel`                                                                                                                                                                                                                                                                                                                                                            | 1.8                          | 1.6         |
| `surface=woodchip`                                                                                                                                                                                                                                                                                                                                                          | 1.8                          | 1.6         |
| `surface=ground`                                                                                                                                                                                                                                                                                                                                                            | 2.3                          | 2.4         |
| `surface=dirt`                                                                                                                                                                                                                                                                                                                                                              | 2.3                          | 2.4         |
| `surface=earth`                                                                                                                                                                                                                                                                                                                                                             | 2.3                          | 2.4         |
| `surface=grass`                                                                                                                                                                                                                                                                                                                                                             | 2.3                          | 1.8         |
| `surface=mud`                                                                                                                                                                                                                                                                                                                                                               | 3.0                          | 3.0         |
| `surface=sand`                                                                                                                                                                                                                                                                                                                                                              | 3.0                          | 1.8         |
| `!tracktype; surface not one of [unpaved] or absent; highway one of [track, bridleway]`                                                                                                                                                                                                                                                                                     | 1.8                          | 1.6         |
| `tracktype=grade2; surface not one of [unpaved] or absent; highway one of [track, bridleway, service, unclassified]`                                                                                                                                                                                                                                                        | 1.4                          | 1.4         |
| `tracktype=grade3; surface not one of [unpaved] or absent; highway one of [track, bridleway, service, unclassified]`                                                                                                                                                                                                                                                        | 1.8                          | 1.6         |
| `tracktype=grade4; surface not one of [unpaved] or absent; highway one of [track, bridleway, service, unclassified]`                                                                                                                                                                                                                                                        | 2.3                          | 1.8         |
| `tracktype=grade5; surface not one of [unpaved] or absent; highway one of [track, bridleway, service, unclassified]`                                                                                                                                                                                                                                                        | 2.3                          | 2.4         |
| `surface not one of [no, none] or absent; highway=path`                                                                                                                                                                                                                                                                                                                     | 2.3                          | 2.4         |
| `sac_scale=mountain_hiking`                                                                                                                                                                                                                                                                                                                                                 |                              | 1.8         |
| `trail_visibility=intermediate`                                                                                                                                                                                                                                                                                                                                             |                              | 1.8         |

<!-- mixins END -->
