### Overview

This page describes how to prepare a DEM GeoTIFF for use as elevation input to OTP. For the
`build-config.json` options that control elevation handling, see the
[Elevation data](BuildConfiguration.md#elevation-data) section of the build configuration reference.

During the graph build, OTP samples the DEM along every street edge using bilinear interpolation, at
intervals controlled by `distanceBetweenElevationSamples` (default 10 m). The file format choices
below — resolution, projection, tile layout, NoData declaration, and compression — affect both the
duration of this step and the correctness of the resulting profiles.

### Resolution

DEM horizontal resolution determines how much terrain detail OTP can see.

- **50 m** (e.g. SRTM) smooths out short hills, underestimates climbs on undulating
  terrain, and produces visibly piecewise-linear elevation profiles. Acceptable for small or flat areas
  but a poor fit for bicycle/walk routing in hilly regions.
- **10 m** (e.g. national lidar-derived products) produces noticeably more realistic
  profiles for bicycling and walking, captures short climbs, and matches the default
  `distanceBetweenElevationSamples` of 10 m.

### Projection

OTP queries each elevation as a WGS84 lon/lat point (`ElevationModule#WGS84_XY` in
`application/src/main/java/org/opentripplanner/graph_builder/module/ned/ElevationModule.java`).
GeoTools then transforms that single query point into the coverage's native CRS at sample time, on the
fly. A coordinate-by-coordinate transform is essentially free compared to the bilinear interpolation
itself.

**Therefore: do not reproject the DEM raster to WGS84.** Keep the DEM in its native projected CRS
(typically the same UTM zone used by the national survey that produced it) and tag the file with its
true EPSG code. Reprojecting the pixels would resample every cell, blur the data, and pay a one-time
cost for no runtime benefit.

If you are using a DEM in unprojected geographic coordinates (EPSG:4326), make sure the axis order is
(longitude, latitude). There is no reliable standard for WGS84 axis order; OTP follows the convention
used by SRTM and Proj4.

### Tile layout

DEM GeoTIFFs can be organised as **strips** (each block is one row of the full raster width) or as
**tiles** (each block is a small square, typically 256×256 or 512×512 pixels). For DEMs consumed by OTP,
**use tiled layout**.

OTP samples DEMs through GeoTools, which uses JAI's tile cache. Every bilinear sample reads a 2×2 pixel
footprint. With strip layout (`RowsPerStrip = 1` is common for DEMs produced by mosaicing tools that do
not set tiling explicitly), each footprint spans two strips, doubling the number of cache lookups and
thrashing the cache as the build walks across the country. With tiled 512×512 layout, the vast majority
of 2×2 footprints fall inside a single tile, the working set stays compact, and the elevation pass runs
substantially faster — observed at multiple times faster for country-scale builds.

**Recommended:** `TILED=YES`, `BLOCKXSIZE=512`, `BLOCKYSIZE=512`.

You can check the tile layout of an existing file with `gdalinfo`:

```
gdalinfo dem.tif | grep -i 'block\|tile'
```

A stripped file will report `Block=<width>x1`. A tiled file will report e.g. `Block=512x512`.

### NoData flag

DEMs typically use a sentinel pixel value (commonly `-32767` for int16 rasters) to mark "no data" —
ocean, areas outside the survey, missing tiles. **The output GeoTIFF must declare this sentinel in its
NoData metadata.**

OTP's `ElevationModule` reads the declared NoData value through `CoverageUtilities.getNoDataProperty`
and rejects matching samples in `NoDataGridCoverage`. If the file does not declare the sentinel:

- OTP treats the sentinel value as a literal elevation (e.g. `-32767 m`).
- Bilinear interpolation across the coastline or any outside-coverage boundary mixes real elevations
  with the sentinel and silently produces wildly wrong profiles for coastal and island streets.

This bug is invisible in build logs — the build completes successfully but the resulting profiles are
corrupt. Always confirm the NoData flag is set, and that it matches the actual sentinel used in the
pixel data.

```
gdalinfo dem.tif | grep -i nodata
```

If the file does not declare NoData but you know the sentinel value, you can set it in place:

```
gdal_edit.py -a_nodata -32767 dem.tif
```

### Compression

Compression (typically LZW with `predictor=2` for integer DEMs) trades runtime cost for disk size:

- **Without compression**, every tile read is a direct memory-mapped slice of the file — fastest at
  sample time.
- **With LZW**, every tile read includes a decode step. For a single-threaded build the per-sample cost
  is small, but it does add up.

Use uncompressed output for moderate DEMs (a 50 m DEM of a country is typically 1–2 GB uncompressed and
fits comfortably on disk and in the page cache). Use LZW for large DEMs where uncompressed size becomes
a problem — a 10 m DEM of a country can be several tens of GB uncompressed and 5–10 GB with LZW.

### Example: convert a stripped, untagged DEM to OTP-ready form

If you have a DEM that is stripped, missing NoData, or both, you can fix it in one pass with GDAL:

```
gdal_translate \
  -co TILED=YES -co BLOCKXSIZE=512 -co BLOCKYSIZE=512 \
  -a_nodata -32767 \
  input.tif output.tif
```

Add `-co COMPRESS=LZW -co PREDICTOR=2` if you need compression. Add `-co BIGTIFF=YES` if the output
will exceed 4 GB.

For mosaicing many source tiles into a single monolithic DEM (recommended — OTP handles multiple DEM
files but a single file simplifies deployment), `gdalbuildvrt` followed by `gdal_translate` works well,
or `rasterio.merge` in Python.

