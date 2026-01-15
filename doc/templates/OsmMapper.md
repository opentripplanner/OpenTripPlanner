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

<!-- INSERT: props -->

### Safety mixins

Mixins are selectors that have an effect on the bicycle and walk safety factors. 
Their safety values are multiplied with the base values from the selected way properties.

Mixins can also add or remove permissions on an OSM way, which will be further overridden with
explicitly set permission tags. If two mixins add and remove the same permission on the same way,
the behavior is unspecified which usually indicates a tagging error on the way.

Multiple mixins can apply to the same way and their effects compound.

<!-- INSERT: mixins -->
