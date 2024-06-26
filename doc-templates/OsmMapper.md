# OSM tag mapping

### Way properties

Way properties set a way's permission and optionally influences its walk and bicycle safety factors.

These factors determine how desirable an OSM way is when routing for cyclists and pedestrians.
Lower safety values make an OSM way more desirable and higher values less desirable.

<!-- INSERT: props -->

### Safety mixins

Mixins are selectors that have only an effect on the bicycle and walk safety factors but not on the
permission of an OSM way. Their safety values are multiplied with the base values from the selected
way properties. Multiple mixins can apply to the same way and their effects compound.

<!-- INSERT: mixins -->
