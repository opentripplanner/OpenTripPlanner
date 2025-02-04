<!--
  NOTE! Part of this document is generated. Make sure you edit the template, not the generated doc.

   - Template directory is:  /doc/templates
   - Generated directory is: /doc/user 
-->

# Debug UI configuration

The Debug UI is the standard interface that is bundled with OTP and available by visiting 
[`http://localhost:8080`](http://localhost:8080). This page lists the configuration options available
by placing a file `debug-ui-config.json` into OTP's working directory.

<!-- PARAMETERS-TABLE BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

| Config Parameter                                          |    Type    | Summary                                                                                                                                                                                      |  Req./Opt. | Default Value         | Since |
|-----------------------------------------------------------|:----------:|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:----------:|-----------------------|:-----:|
| [additionalBackgroundLayers](#additionalBackgroundLayers) | `object[]` | Additional background raster map layers.                                                                                                                                                     | *Optional* |                       |  2.7  |
|       attribution                                         |  `string`  | Attribution for the map data.                                                                                                                                                                | *Optional* | `"© OpenTripPlanner"` |  2.7  |
|       name                                                |  `string`  | Name to appear in the layer selector.                                                                                                                                                        | *Required* |                       |  2.7  |
|       templateUrl                                         |  `string`  | The [Maplibre-compatible template URL](https://maplibre.org/maplibre-native/ios/api/tile-url-templates.html) for the raster layer, for example `https://examples.com/tiles/{z}/{x}/{y}.png`. | *Required* |                       |  2.7  |
|       tileSize                                            |  `integer` | Size of the tile in pixels.                                                                                                                                                                  | *Optional* | `256`                 |  2.7  |

<!-- PARAMETERS-TABLE END -->


## Parameter Details

<!-- PARAMETERS-DETAILS BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

<h3 id="additionalBackgroundLayers">additionalBackgroundLayers</h3>

**Since version:** `2.7` ∙ **Type:** `object[]` ∙ **Cardinality:** `Optional`   
**Path:** / 

Additional background raster map layers.

Add additional background layers that will appear in the Debug UI as one of the choices.

Currently only raster tile layers are supported.



<!-- PARAMETERS-DETAILS END -->

## Config Example

<!-- JSON-EXAMPLE BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

```JSON
// debug-ui-config.json
{
  "additionalBackgroundLayers" : [
    {
      "name" : "TriMet aerial photos",
      "templateUrl" : "https://maps.trimet.org/wms/reflect?bbox={bbox-epsg-3857}&format=image/png&service=WMS&version=1.1.0&request=GetMap&srs=EPSG:3857&width=256&height=256&layers=aerials",
      "attribution" : "© TriMet"
    }
  ]
}
```

<!-- JSON-EXAMPLE END -->
