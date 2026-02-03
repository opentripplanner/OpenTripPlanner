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

<!-- INSERT: props -->

### Safety mixins

Mixins are selectors that have an effect on the bicycle and walk safety factors. 
Their safety values are multiplied with the base values from the selected way properties.

Mixins can also add or remove permissions on an OSM way, which will be further overridden with
explicitly set permission tags. If two mixins add and remove the same permission on the same way,
the behavior is unspecified which usually indicates a tagging error on the way.

Multiple mixins can apply to the same way and their effects compound.

<!-- INSERT: mixins -->
