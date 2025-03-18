import { MapInstance } from 'react-map-gl/maplibre';

/**
 * Find currently selected (= visible) debug layers.
 */
export const findSelectedDebugLayers = (mapInstance: MapInstance): string[] => {
  const style = mapInstance.getStyle();
  if (!style || !style.layers) {
    return [];
  }

  return style.layers
    .filter((l) => l.type !== 'raster' && !l.id.startsWith('jsx'))
    .filter((l) => mapInstance.getLayoutProperty(l.id, 'visibility') !== 'none')
    .map((l) => l.id);
};
