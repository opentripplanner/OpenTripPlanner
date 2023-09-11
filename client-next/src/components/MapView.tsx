import { Map, NavigationControl } from 'react-map-gl';
import 'maplibre-gl/dist/maplibre-gl.css';

const mapStyle = {
  version: 8,
  sources: {
    osm: {
      type: 'raster',
      tiles: ['https://a.tile.openstreetmap.org/{z}/{x}/{y}.png'],
      tileSize: 256,
      attribution: '&copy; OpenStreetMap Contributors',
      maxzoom: 19,
    },
  },
  layers: [
    {
      id: 'osm',
      type: 'raster',
      source: 'osm', // This must match the source key above
    },
  ],
};

// TODO: this should be configurable
const initialViewState = {
  latitude: 60.7554885,
  longitude: 10.2332855,
  zoom: 4,
};
export function MapView() {
  return (
    <Map
      mapLib={import('maplibre-gl')}
      mapStyle={mapStyle}
      initialViewState={initialViewState}
      style={{ width: '100%', height: 'calc(100vh - 184px)' }}
    >
      <NavigationControl position="top-left" />
    </Map>
  );
}
