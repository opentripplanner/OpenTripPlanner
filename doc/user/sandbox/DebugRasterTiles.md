# Debug raster tiles 

<b>These are only meant to be used when using vector tiles is not an option (mainly JOSM).</b>

The raster tiles are available with the following path structure:
`/otp/debug/rastertiles/{layer}/{z}/{x}/{y}.png`

The available layers are:

- bike-safety
- walk-safety
- thru-traffic
- traversal
- wheelchair
- elevation

## Contact Info

- Digitransit, Finland

## Changelog

- 2025-02-13: Moved raster tiles from core code into sandbox.
- 2025-05-16: Changed API path slightly.


### Configuration

This is turned _off_ by default. To turn it on enable the `DebugRasterTiles` feature.

```json
// otp-config.json
{
  "otpFeatures": {
    "DebugRasterTiles": true
  }
}
```
