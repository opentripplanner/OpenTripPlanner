# Spatial Analysis Concepts and Usage

## PointSets

PointSets are named collections of geographic places that serve as sets of origins or destinations in one-to-many
or many-to-many requests. OTP can load PointSets from GeoJSON or CSV files.

The following is an example PointSet (two blocks from the 2010 US census) in the GeoJSON format:
```JSON
{
  "type": "FeatureCollection",
  "properties": {
    "id": "uniqueID",
    "label": "A short label",
    "description": "A longer, detailed description of the PointSet as a whole.",
    "schema": {
      "pop10": {
        "style": {},
        "label": "population"
      },
      "housing10": {
        "style": {},
        "label": "housing units"
      }
    }
  },
  "features": [
    {
      "properties": {
        "structured": {
          "pop10": 166,
          "housing10": 64
        }
      },
      "geometry": {
        "coordinates": [
          -71.13364978767969,
          42.345030084059864
        ],
        "type": "Point"
      },
      "type": "Feature",
      "id": "tiny_blocks.1"
    },
    {
      "properties": {
        "structured": {
          ".pop10": 76,
          ".housing10": 34
        }
      },
      "geometry": {
        "coordinates": [
          -71.12924764173955,
          42.34592621836178
        ],
        "type": "Point"
      },
      "type": "Feature",
      "id": "tiny_blocks.2"
    }
  ],
}
```

A GeoJSON PointSet is a valid GeoJSON feature collection containing point or polygon features.
Each feature has a special property called "structured" that contains origin or destination opportunity counts
for that location. The features in a PointSet may have other properties of any kind you like (which OTP will ignore),
but the structured properties must have integer numeric values. Each feature in this particular PointSet
contains a count of people (pop10) and a count of housing units (housing10). Conceptually every feature
in the collection has the same set of structured properties, but if one is omitted then its value is assumed to be zero.
For example, if one feature has a structured property field called "population" with a value of 10, another feature with
no such field will be assumed to have a population of zero.

The feature collection as a whole can (optionally) have id, label, description, and schema properties.The schema provides
additional information about how to display the structured properties that appear in the features, including labels
color, and other styling information.

This is the same example PointSet in the CSV format:

```CSV
lat,lon,pop10,housing10
42.345030084059864,-71.13364978767969,166,64
42.34592621836178,-71.12924764173955,76,34
```

Note that while this format is much more compact, it contains no metadata and styling information for the
structured properties or the data set as a whole.