## OSM tag mapping

### Way properties

Way properties set a way's permission and optionally influences its walk and bicycle safety factors.
These factors determine how desirable an OSM way is when routing for cyclists and pedestrians.

<!-- props BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

| specifier                                               | permission             | safety |
|---------------------------------------------------------|------------------------|--------|
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

<!-- prop-details BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

<h3 id="0">Rule #0</h3>

**Specifier:** `mtb:scale=3`   
**Permission:** NONE   
**Bike safety factor:** forward: 1.0, back: 1.0   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="1">Rule #1</h3>

**Specifier:** `mtb:scale=4`   
**Permission:** NONE   
**Bike safety factor:** forward: 1.0, back: 1.0   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="2">Rule #2</h3>

**Specifier:** `mtb:scale=5`   
**Permission:** NONE   
**Bike safety factor:** forward: 1.0, back: 1.0   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="3">Rule #3</h3>

**Specifier:** `mtb:scale=6`   
**Permission:** NONE   
**Bike safety factor:** forward: 1.0, back: 1.0   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="4">Rule #4</h3>

**Specifier:** `highway=corridor`   
**Permission:** PEDESTRIAN   
**Bike safety factor:** forward: 1.0, back: 1.0   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="5">Rule #5</h3>

**Specifier:** `highway=steps`   
**Permission:** PEDESTRIAN   
**Bike safety factor:** forward: 1.0, back: 1.0   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="6">Rule #6</h3>

**Specifier:** `highway=crossing`   
**Permission:** PEDESTRIAN   
**Bike safety factor:** forward: 1.0, back: 1.0   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="7">Rule #7</h3>

**Specifier:** `highway=platform`   
**Permission:** PEDESTRIAN   
**Bike safety factor:** forward: 1.0, back: 1.0   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="8">Rule #8</h3>

**Specifier:** `public_transport=platform`   
**Permission:** PEDESTRIAN   
**Bike safety factor:** forward: 1.0, back: 1.0   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="9">Rule #9</h3>

**Specifier:** `railway=platform`   
**Permission:** PEDESTRIAN   
**Bike safety factor:** forward: 1.0, back: 1.0   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="10">Rule #10</h3>

**Specifier:** `footway=sidewalk; highway=footway`   
**Permission:** PEDESTRIAN   
**Bike safety factor:** forward: 1.0, back: 1.0   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="11">Rule #11</h3>

**Specifier:** `mtb:scale=1`   
**Permission:** PEDESTRIAN   
**Bike safety factor:** forward: 1.0, back: 1.0   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="12">Rule #12</h3>

**Specifier:** `mtb:scale=2`   
**Permission:** PEDESTRIAN   
**Bike safety factor:** forward: 1.0, back: 1.0   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="13">Rule #13</h3>

**Specifier:** `mtb:scale=0`   
**Permission:** PEDESTRIAN_AND_BICYCLE   
**Bike safety factor:** forward: 1.0, back: 1.0   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="14">Rule #14</h3>

**Specifier:** `highway=cycleway`   
**Permission:** PEDESTRIAN_AND_BICYCLE   
**Bike safety factor:** forward: 0.6, back: 0.6   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="15">Rule #15</h3>

**Specifier:** `highway=path`   
**Permission:** PEDESTRIAN_AND_BICYCLE   
**Bike safety factor:** forward: 0.75, back: 0.75   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="16">Rule #16</h3>

**Specifier:** `highway=pedestrian`   
**Permission:** PEDESTRIAN_AND_BICYCLE   
**Bike safety factor:** forward: 0.9, back: 0.9   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="17">Rule #17</h3>

**Specifier:** `highway=footway`   
**Permission:** PEDESTRIAN_AND_BICYCLE   
**Bike safety factor:** forward: 1.1, back: 1.1   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="18">Rule #18</h3>

**Specifier:** `highway=bridleway`   
**Permission:** PEDESTRIAN_AND_BICYCLE   
**Bike safety factor:** forward: 1.3, back: 1.3   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="19">Rule #19</h3>

**Specifier:** `highway=living_street`   
**Permission:** ALL   
**Bike safety factor:** forward: 0.9, back: 0.9   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="20">Rule #20</h3>

**Specifier:** `highway=unclassified`   
**Permission:** ALL   
**Bike safety factor:** forward: 1.0, back: 1.0   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="21">Rule #21</h3>

**Specifier:** `highway=road`   
**Permission:** ALL   
**Bike safety factor:** forward: 1.0, back: 1.0   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="22">Rule #22</h3>

**Specifier:** `highway=byway`   
**Permission:** ALL   
**Bike safety factor:** forward: 1.3, back: 1.3   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="23">Rule #23</h3>

**Specifier:** `highway=track`   
**Permission:** ALL   
**Bike safety factor:** forward: 1.3, back: 1.3   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="24">Rule #24</h3>

**Specifier:** `highway=service`   
**Permission:** ALL   
**Bike safety factor:** forward: 1.1, back: 1.1   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="25">Rule #25</h3>

**Specifier:** `highway=residential`   
**Permission:** ALL   
**Bike safety factor:** forward: 0.98, back: 0.98   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="26">Rule #26</h3>

**Specifier:** `highway=residential_link`   
**Permission:** ALL   
**Bike safety factor:** forward: 0.98, back: 0.98   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="27">Rule #27</h3>

**Specifier:** `highway=tertiary`   
**Permission:** ALL   
**Bike safety factor:** forward: 1.0, back: 1.0   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="28">Rule #28</h3>

**Specifier:** `highway=tertiary_link`   
**Permission:** ALL   
**Bike safety factor:** forward: 1.0, back: 1.0   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="29">Rule #29</h3>

**Specifier:** `highway=secondary`   
**Permission:** ALL   
**Bike safety factor:** forward: 1.5, back: 1.5   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="30">Rule #30</h3>

**Specifier:** `highway=secondary_link`   
**Permission:** ALL   
**Bike safety factor:** forward: 1.5, back: 1.5   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="31">Rule #31</h3>

**Specifier:** `highway=primary`   
**Permission:** ALL   
**Bike safety factor:** forward: 2.06, back: 2.06   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="32">Rule #32</h3>

**Specifier:** `highway=primary_link`   
**Permission:** ALL   
**Bike safety factor:** forward: 2.06, back: 2.06   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="33">Rule #33</h3>

**Specifier:** `highway=trunk_link`   
**Permission:** CAR   
**Bike safety factor:** forward: 2.06, back: 2.06   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="34">Rule #34</h3>

**Specifier:** `highway=motorway_link`   
**Permission:** CAR   
**Bike safety factor:** forward: 2.06, back: 2.06   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="35">Rule #35</h3>

**Specifier:** `highway=trunk`   
**Permission:** CAR   
**Bike safety factor:** forward: 7.47, back: 7.47   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="36">Rule #36</h3>

**Specifier:** `highway=motorway`   
**Permission:** CAR   
**Bike safety factor:** forward: 8.0, back: 8.0   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="37">Rule #37</h3>

**Specifier:** `present(highway); cycleway=lane`   
**Permission:** PEDESTRIAN_AND_BICYCLE   
**Bike safety factor:** forward: 0.87, back: 0.87   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="38">Rule #38</h3>

**Specifier:** `highway=service; cycleway=lane`   
**Permission:** ALL   
**Bike safety factor:** forward: 0.77, back: 0.77   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="39">Rule #39</h3>

**Specifier:** `highway=residential; cycleway=lane`   
**Permission:** ALL   
**Bike safety factor:** forward: 0.77, back: 0.77   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="40">Rule #40</h3>

**Specifier:** `highway=residential_link; cycleway=lane`   
**Permission:** ALL   
**Bike safety factor:** forward: 0.77, back: 0.77   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="41">Rule #41</h3>

**Specifier:** `highway=tertiary; cycleway=lane`   
**Permission:** ALL   
**Bike safety factor:** forward: 0.87, back: 0.87   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="42">Rule #42</h3>

**Specifier:** `highway=tertiary_link; cycleway=lane`   
**Permission:** ALL   
**Bike safety factor:** forward: 0.87, back: 0.87   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="43">Rule #43</h3>

**Specifier:** `highway=secondary; cycleway=lane`   
**Permission:** ALL   
**Bike safety factor:** forward: 0.96, back: 0.96   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="44">Rule #44</h3>

**Specifier:** `highway=secondary_link; cycleway=lane`   
**Permission:** ALL   
**Bike safety factor:** forward: 0.96, back: 0.96   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="45">Rule #45</h3>

**Specifier:** `highway=primary; cycleway=lane`   
**Permission:** ALL   
**Bike safety factor:** forward: 1.15, back: 1.15   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="46">Rule #46</h3>

**Specifier:** `highway=primary_link; cycleway=lane`   
**Permission:** ALL   
**Bike safety factor:** forward: 1.15, back: 1.15   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="47">Rule #47</h3>

**Specifier:** `highway=trunk; cycleway=lane`   
**Permission:** BICYCLE_AND_CAR   
**Bike safety factor:** forward: 1.5, back: 1.5   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="48">Rule #48</h3>

**Specifier:** `highway=trunk_link; cycleway=lane`   
**Permission:** BICYCLE_AND_CAR   
**Bike safety factor:** forward: 1.15, back: 1.15   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="49">Rule #49</h3>

**Specifier:** `highway=motorway; cycleway=lane`   
**Permission:** BICYCLE_AND_CAR   
**Bike safety factor:** forward: 2.0, back: 2.0   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="50">Rule #50</h3>

**Specifier:** `highway=motorway_link; cycleway=lane`   
**Permission:** BICYCLE_AND_CAR   
**Bike safety factor:** forward: 1.15, back: 1.15   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="51">Rule #51</h3>

**Specifier:** `present(highway); cycleway=share_busway`   
**Permission:** PEDESTRIAN_AND_BICYCLE   
**Bike safety factor:** forward: 0.92, back: 0.92   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="52">Rule #52</h3>

**Specifier:** `highway=service; cycleway=share_busway`   
**Permission:** ALL   
**Bike safety factor:** forward: 0.85, back: 0.85   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="53">Rule #53</h3>

**Specifier:** `highway=residential; cycleway=share_busway`   
**Permission:** ALL   
**Bike safety factor:** forward: 0.85, back: 0.85   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="54">Rule #54</h3>

**Specifier:** `highway=residential_link; cycleway=share_busway`   
**Permission:** ALL   
**Bike safety factor:** forward: 0.85, back: 0.85   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="55">Rule #55</h3>

**Specifier:** `highway=tertiary; cycleway=share_busway`   
**Permission:** ALL   
**Bike safety factor:** forward: 0.92, back: 0.92   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="56">Rule #56</h3>

**Specifier:** `highway=tertiary_link; cycleway=share_busway`   
**Permission:** ALL   
**Bike safety factor:** forward: 0.92, back: 0.92   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="57">Rule #57</h3>

**Specifier:** `highway=secondary; cycleway=share_busway`   
**Permission:** ALL   
**Bike safety factor:** forward: 0.99, back: 0.99   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="58">Rule #58</h3>

**Specifier:** `highway=secondary_link; cycleway=share_busway`   
**Permission:** ALL   
**Bike safety factor:** forward: 0.99, back: 0.99   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="59">Rule #59</h3>

**Specifier:** `highway=primary; cycleway=share_busway`   
**Permission:** ALL   
**Bike safety factor:** forward: 1.25, back: 1.25   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="60">Rule #60</h3>

**Specifier:** `highway=primary_link; cycleway=share_busway`   
**Permission:** ALL   
**Bike safety factor:** forward: 1.25, back: 1.25   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="61">Rule #61</h3>

**Specifier:** `highway=trunk; cycleway=share_busway`   
**Permission:** BICYCLE_AND_CAR   
**Bike safety factor:** forward: 1.75, back: 1.75   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="62">Rule #62</h3>

**Specifier:** `highway=trunk_link; cycleway=share_busway`   
**Permission:** BICYCLE_AND_CAR   
**Bike safety factor:** forward: 1.25, back: 1.25   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="63">Rule #63</h3>

**Specifier:** `highway=motorway; cycleway=share_busway`   
**Permission:** BICYCLE_AND_CAR   
**Bike safety factor:** forward: 2.5, back: 2.5   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="64">Rule #64</h3>

**Specifier:** `highway=motorway_link; cycleway=share_busway`   
**Permission:** BICYCLE_AND_CAR   
**Bike safety factor:** forward: 1.25, back: 1.25   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="65">Rule #65</h3>

**Specifier:** `present(highway); cycleway=opposite_lane`   
**Permission:** PEDESTRIAN_AND_BICYCLE   
**Bike safety factor:** forward: 1.0, back: 0.87   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="66">Rule #66</h3>

**Specifier:** `highway=service; cycleway=opposite_lane`   
**Permission:** ALL   
**Bike safety factor:** forward: 1.1, back: 0.77   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="67">Rule #67</h3>

**Specifier:** `highway=residential; cycleway=opposite_lane`   
**Permission:** ALL   
**Bike safety factor:** forward: 0.98, back: 0.77   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="68">Rule #68</h3>

**Specifier:** `highway=residential_link; cycleway=opposite_lane`   
**Permission:** ALL   
**Bike safety factor:** forward: 0.98, back: 0.77   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="69">Rule #69</h3>

**Specifier:** `highway=tertiary; cycleway=opposite_lane`   
**Permission:** ALL   
**Bike safety factor:** forward: 1.0, back: 0.87   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="70">Rule #70</h3>

**Specifier:** `highway=tertiary_link; cycleway=opposite_lane`   
**Permission:** ALL   
**Bike safety factor:** forward: 1.0, back: 0.87   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="71">Rule #71</h3>

**Specifier:** `highway=secondary; cycleway=opposite_lane`   
**Permission:** ALL   
**Bike safety factor:** forward: 1.5, back: 0.96   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="72">Rule #72</h3>

**Specifier:** `highway=secondary_link; cycleway=opposite_lane`   
**Permission:** ALL   
**Bike safety factor:** forward: 1.5, back: 0.96   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="73">Rule #73</h3>

**Specifier:** `highway=primary; cycleway=opposite_lane`   
**Permission:** ALL   
**Bike safety factor:** forward: 2.06, back: 1.15   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="74">Rule #74</h3>

**Specifier:** `highway=primary_link; cycleway=opposite_lane`   
**Permission:** ALL   
**Bike safety factor:** forward: 2.06, back: 1.15   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="75">Rule #75</h3>

**Specifier:** `highway=trunk; cycleway=opposite_lane`   
**Permission:** BICYCLE_AND_CAR   
**Bike safety factor:** forward: 7.47, back: 1.5   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="76">Rule #76</h3>

**Specifier:** `highway=trunk_link; cycleway=opposite_lane`   
**Permission:** BICYCLE_AND_CAR   
**Bike safety factor:** forward: 2.06, back: 1.15   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="77">Rule #77</h3>

**Specifier:** `present(highway); cycleway=track`   
**Permission:** PEDESTRIAN_AND_BICYCLE   
**Bike safety factor:** forward: 0.75, back: 0.75   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="78">Rule #78</h3>

**Specifier:** `highway=service; cycleway=track`   
**Permission:** ALL   
**Bike safety factor:** forward: 0.65, back: 0.65   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="79">Rule #79</h3>

**Specifier:** `highway=residential; cycleway=track`   
**Permission:** ALL   
**Bike safety factor:** forward: 0.65, back: 0.65   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="80">Rule #80</h3>

**Specifier:** `highway=residential_link; cycleway=track`   
**Permission:** ALL   
**Bike safety factor:** forward: 0.65, back: 0.65   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="81">Rule #81</h3>

**Specifier:** `highway=tertiary; cycleway=track`   
**Permission:** ALL   
**Bike safety factor:** forward: 0.75, back: 0.75   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="82">Rule #82</h3>

**Specifier:** `highway=tertiary_link; cycleway=track`   
**Permission:** ALL   
**Bike safety factor:** forward: 0.75, back: 0.75   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="83">Rule #83</h3>

**Specifier:** `highway=secondary; cycleway=track`   
**Permission:** ALL   
**Bike safety factor:** forward: 0.8, back: 0.8   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="84">Rule #84</h3>

**Specifier:** `highway=secondary_link; cycleway=track`   
**Permission:** ALL   
**Bike safety factor:** forward: 0.8, back: 0.8   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="85">Rule #85</h3>

**Specifier:** `highway=primary; cycleway=track`   
**Permission:** ALL   
**Bike safety factor:** forward: 0.85, back: 0.85   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="86">Rule #86</h3>

**Specifier:** `highway=primary_link; cycleway=track`   
**Permission:** ALL   
**Bike safety factor:** forward: 0.85, back: 0.85   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="87">Rule #87</h3>

**Specifier:** `highway=trunk; cycleway=track`   
**Permission:** BICYCLE_AND_CAR   
**Bike safety factor:** forward: 0.95, back: 0.95   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="88">Rule #88</h3>

**Specifier:** `highway=trunk_link; cycleway=track`   
**Permission:** BICYCLE_AND_CAR   
**Bike safety factor:** forward: 0.85, back: 0.85   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="89">Rule #89</h3>

**Specifier:** `present(highway); cycleway=opposite_track`   
**Permission:** PEDESTRIAN_AND_BICYCLE   
**Bike safety factor:** forward: 1.0, back: 0.75   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="90">Rule #90</h3>

**Specifier:** `highway=service; cycleway=opposite_track`   
**Permission:** ALL   
**Bike safety factor:** forward: 1.1, back: 0.65   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="91">Rule #91</h3>

**Specifier:** `highway=residential; cycleway=opposite_track`   
**Permission:** ALL   
**Bike safety factor:** forward: 0.98, back: 0.65   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="92">Rule #92</h3>

**Specifier:** `highway=residential_link; cycleway=opposite_track`   
**Permission:** ALL   
**Bike safety factor:** forward: 0.98, back: 0.65   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="93">Rule #93</h3>

**Specifier:** `highway=tertiary; cycleway=opposite_track`   
**Permission:** ALL   
**Bike safety factor:** forward: 1.0, back: 0.75   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="94">Rule #94</h3>

**Specifier:** `highway=tertiary_link; cycleway=opposite_track`   
**Permission:** ALL   
**Bike safety factor:** forward: 1.0, back: 0.75   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="95">Rule #95</h3>

**Specifier:** `highway=secondary; cycleway=opposite_track`   
**Permission:** ALL   
**Bike safety factor:** forward: 1.5, back: 0.8   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="96">Rule #96</h3>

**Specifier:** `highway=secondary_link; cycleway=opposite_track`   
**Permission:** ALL   
**Bike safety factor:** forward: 1.5, back: 0.8   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="97">Rule #97</h3>

**Specifier:** `highway=primary; cycleway=opposite_track`   
**Permission:** ALL   
**Bike safety factor:** forward: 2.06, back: 0.85   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="98">Rule #98</h3>

**Specifier:** `highway=primary_link; cycleway=opposite_track`   
**Permission:** ALL   
**Bike safety factor:** forward: 2.06, back: 0.85   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="99">Rule #99</h3>

**Specifier:** `highway=trunk; cycleway=opposite_track`   
**Permission:** BICYCLE_AND_CAR   
**Bike safety factor:** forward: 7.47, back: 0.95   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="100">Rule #100</h3>

**Specifier:** `highway=trunk_link; cycleway=opposite_track`   
**Permission:** BICYCLE_AND_CAR   
**Bike safety factor:** forward: 2.06, back: 0.85   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="101">Rule #101</h3>

**Specifier:** `present(highway); cycleway=shared_lane`   
**Permission:** PEDESTRIAN_AND_BICYCLE   
**Bike safety factor:** forward: 0.77, back: 0.77   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="102">Rule #102</h3>

**Specifier:** `highway=service; cycleway=shared_lane`   
**Permission:** ALL   
**Bike safety factor:** forward: 0.73, back: 0.73   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="103">Rule #103</h3>

**Specifier:** `highway=residential; cycleway=shared_lane`   
**Permission:** ALL   
**Bike safety factor:** forward: 0.77, back: 0.77   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="104">Rule #104</h3>

**Specifier:** `highway=residential_link; cycleway=shared_lane`   
**Permission:** ALL   
**Bike safety factor:** forward: 0.77, back: 0.77   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="105">Rule #105</h3>

**Specifier:** `highway=tertiary; cycleway=shared_lane`   
**Permission:** ALL   
**Bike safety factor:** forward: 0.83, back: 0.83   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="106">Rule #106</h3>

**Specifier:** `highway=tertiary_link; cycleway=shared_lane`   
**Permission:** ALL   
**Bike safety factor:** forward: 0.83, back: 0.83   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="107">Rule #107</h3>

**Specifier:** `highway=secondary; cycleway=shared_lane`   
**Permission:** ALL   
**Bike safety factor:** forward: 1.25, back: 1.25   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="108">Rule #108</h3>

**Specifier:** `highway=secondary_link; cycleway=shared_lane`   
**Permission:** ALL   
**Bike safety factor:** forward: 1.25, back: 1.25   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="109">Rule #109</h3>

**Specifier:** `highway=primary; cycleway=shared_lane`   
**Permission:** ALL   
**Bike safety factor:** forward: 1.75, back: 1.75   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="110">Rule #110</h3>

**Specifier:** `highway=primary_link; cycleway=shared_lane`   
**Permission:** ALL   
**Bike safety factor:** forward: 1.75, back: 1.75   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="111">Rule #111</h3>

**Specifier:** `present(highway); cycleway=opposite`   
**Permission:** PEDESTRIAN_AND_BICYCLE   
**Bike safety factor:** forward: 1.0, back: 1.4   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="112">Rule #112</h3>

**Specifier:** `highway=service; cycleway=opposite`   
**Permission:** ALL   
**Bike safety factor:** forward: 1.1, back: 1.1   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="113">Rule #113</h3>

**Specifier:** `highway=residential; cycleway=opposite`   
**Permission:** ALL   
**Bike safety factor:** forward: 0.98, back: 0.98   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="114">Rule #114</h3>

**Specifier:** `highway=residential_link; cycleway=opposite`   
**Permission:** ALL   
**Bike safety factor:** forward: 0.98, back: 0.98   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="115">Rule #115</h3>

**Specifier:** `highway=tertiary; cycleway=opposite`   
**Permission:** ALL   
**Bike safety factor:** forward: 1.0, back: 1.0   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="116">Rule #116</h3>

**Specifier:** `highway=tertiary_link; cycleway=opposite`   
**Permission:** ALL   
**Bike safety factor:** forward: 1.0, back: 1.0   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="117">Rule #117</h3>

**Specifier:** `highway=secondary; cycleway=opposite`   
**Permission:** ALL   
**Bike safety factor:** forward: 1.5, back: 1.71   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="118">Rule #118</h3>

**Specifier:** `highway=secondary_link; cycleway=opposite`   
**Permission:** ALL   
**Bike safety factor:** forward: 1.5, back: 1.71   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="119">Rule #119</h3>

**Specifier:** `highway=primary; cycleway=opposite`   
**Permission:** ALL   
**Bike safety factor:** forward: 2.06, back: 2.99   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="120">Rule #120</h3>

**Specifier:** `highway=primary_link; cycleway=opposite`   
**Permission:** ALL   
**Bike safety factor:** forward: 2.06, back: 2.99   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="121">Rule #121</h3>

**Specifier:** `highway=path; bicycle=designated`   
**Permission:** PEDESTRIAN_AND_BICYCLE   
**Bike safety factor:** forward: 0.6, back: 0.6   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="122">Rule #122</h3>

**Specifier:** `highway=footway; bicycle=designated`   
**Permission:** PEDESTRIAN_AND_BICYCLE   
**Bike safety factor:** forward: 0.75, back: 0.75   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="123">Rule #123</h3>

**Specifier:** `highway=footway; bicycle=yes; area=yes`   
**Permission:** PEDESTRIAN_AND_BICYCLE   
**Bike safety factor:** forward: 0.9, back: 0.9   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="124">Rule #124</h3>

**Specifier:** `highway=pedestrian; bicycle=designated`   
**Permission:** PEDESTRIAN_AND_BICYCLE   
**Bike safety factor:** forward: 0.75, back: 0.75   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="125">Rule #125</h3>

**Specifier:** `footway=sidewalk; highway=footway; bicycle=yes`   
**Permission:** PEDESTRIAN_AND_BICYCLE   
**Bike safety factor:** forward: 2.5, back: 2.5   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="126">Rule #126</h3>

**Specifier:** `footway=sidewalk; highway=footway; bicycle=designated`   
**Permission:** PEDESTRIAN_AND_BICYCLE   
**Bike safety factor:** forward: 1.1, back: 1.1   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="127">Rule #127</h3>

**Specifier:** `highway=footway; footway=crossing`   
**Permission:** PEDESTRIAN_AND_BICYCLE   
**Bike safety factor:** forward: 2.5, back: 2.5   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="128">Rule #128</h3>

**Specifier:** `highway=footway; footway=crossing; bicycle=designated`   
**Permission:** PEDESTRIAN_AND_BICYCLE   
**Bike safety factor:** forward: 1.1, back: 1.1   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="129">Rule #129</h3>

**Specifier:** `highway=track; bicycle=yes`   
**Permission:** PEDESTRIAN_AND_BICYCLE   
**Bike safety factor:** forward: 1.18, back: 1.18   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="130">Rule #130</h3>

**Specifier:** `highway=track; bicycle=designated`   
**Permission:** PEDESTRIAN_AND_BICYCLE   
**Bike safety factor:** forward: 0.99, back: 0.99   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="131">Rule #131</h3>

**Specifier:** `highway=track; bicycle=yes; present(surface)`   
**Permission:** PEDESTRIAN_AND_BICYCLE   
**Bike safety factor:** forward: 1.18, back: 1.18   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="132">Rule #132</h3>

**Specifier:** `highway=track; bicycle=designated; present(surface)`   
**Permission:** PEDESTRIAN_AND_BICYCLE   
**Bike safety factor:** forward: 0.99, back: 0.99   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="133">Rule #133</h3>

**Specifier:** `highway=track; present(surface)`   
**Permission:** PEDESTRIAN_AND_BICYCLE   
**Bike safety factor:** forward: 1.3, back: 1.3   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="134">Rule #134</h3>

**Specifier:** `present(highway); bicycle=designated`   
**Permission:** ALL   
**Bike safety factor:** forward: 0.97, back: 0.97   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="135">Rule #135</h3>

**Specifier:** `highway=service; bicycle=designated`   
**Permission:** ALL   
**Bike safety factor:** forward: 0.84, back: 0.84   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="136">Rule #136</h3>

**Specifier:** `highway=residential; bicycle=designated`   
**Permission:** ALL   
**Bike safety factor:** forward: 0.95, back: 0.95   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="137">Rule #137</h3>

**Specifier:** `highway=unclassified; bicycle=designated`   
**Permission:** ALL   
**Bike safety factor:** forward: 0.95, back: 0.95   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="138">Rule #138</h3>

**Specifier:** `highway=residential_link; bicycle=designated`   
**Permission:** ALL   
**Bike safety factor:** forward: 0.95, back: 0.95   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="139">Rule #139</h3>

**Specifier:** `highway=tertiary; bicycle=designated`   
**Permission:** ALL   
**Bike safety factor:** forward: 0.97, back: 0.97   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="140">Rule #140</h3>

**Specifier:** `highway=tertiary_link; bicycle=designated`   
**Permission:** ALL   
**Bike safety factor:** forward: 0.97, back: 0.97   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="141">Rule #141</h3>

**Specifier:** `highway=secondary; bicycle=designated`   
**Permission:** ALL   
**Bike safety factor:** forward: 1.46, back: 1.46   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="142">Rule #142</h3>

**Specifier:** `highway=secondary_link; bicycle=designated`   
**Permission:** ALL   
**Bike safety factor:** forward: 1.46, back: 1.46   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="143">Rule #143</h3>

**Specifier:** `highway=primary; bicycle=designated`   
**Permission:** ALL   
**Bike safety factor:** forward: 2.0, back: 2.0   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="144">Rule #144</h3>

**Specifier:** `highway=primary_link; bicycle=designated`   
**Permission:** ALL   
**Bike safety factor:** forward: 2.0, back: 2.0   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="145">Rule #145</h3>

**Specifier:** `highway=trunk; bicycle=designated`   
**Permission:** BICYCLE_AND_CAR   
**Bike safety factor:** forward: 7.25, back: 7.25   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="146">Rule #146</h3>

**Specifier:** `highway=trunk_link; bicycle=designated`   
**Permission:** BICYCLE_AND_CAR   
**Bike safety factor:** forward: 2.0, back: 2.0   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="147">Rule #147</h3>

**Specifier:** `highway=motorway; bicycle=designated`   
**Permission:** BICYCLE_AND_CAR   
**Bike safety factor:** forward: 7.76, back: 7.76   
**Walk safety factor:** forward: 1.0, back: 1.0 

<h3 id="148">Rule #148</h3>

**Specifier:** `highway=motorway_link; bicycle=designated`   
**Permission:** BICYCLE_AND_CAR   
**Bike safety factor:** forward: 2.0, back: 2.0   
**Walk safety factor:** forward: 1.0, back: 1.0 


<!-- prop-details END -->

### Bicycle and walking safety mixins

Mixins are selectors that have only an effect on the bicycle and walk safety factors but not on the
permission of an OSM way. Multiple mixins can apply to the same way and their effects compound.

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
