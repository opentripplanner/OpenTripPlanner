package org.opentripplanner.apis.vectortiles.model;

/**
 * A vector source layer for use in a Maplibre style spec. It contains both the name of the layer
 * inside the tile and a reference to the source (which in turn has the URL where to fetch the
 * data).
 */
public record VectorSourceLayer(TileSource.VectorSource vectorSource, String vectorLayer) {}
