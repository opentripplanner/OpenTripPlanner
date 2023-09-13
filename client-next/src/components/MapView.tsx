import { Layer, Map, Marker, NavigationControl, Source } from 'react-map-gl';
import 'maplibre-gl/dist/maplibre-gl.css';
import { TripQuery, TripQueryVariables } from '../gql/graphql.ts';
import { decode } from '@googlemaps/polyline-codec';

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
export function MapView({
  tripQueryVariables,
  setTripQueryVariables,
  tripQueryResult,
}: {
  tripQueryVariables?: TripQueryVariables;
  setTripQueryVariables: (variables: TripQueryVariables) => void;
  tripQueryResult: TripQuery | null;
}) {
  return (
    <Map
      // @ts-ignore
      mapLib={import('maplibre-gl')}
      // @ts-ignore
      mapStyle={mapStyle}
      initialViewState={initialViewState}
      style={{ width: '100%', height: 'calc(100vh - 184px)' }}
      onDblClick={(e) => {
        e.preventDefault();
        if (!tripQueryVariables?.from.coordinates) {
          setTripQueryVariables({
            ...tripQueryVariables,
            from: {
              coordinates: {
                latitude: e.lngLat.lat,
                longitude: e.lngLat.lng,
              },
            },
            to: {
              coordinates: {
                latitude: 0.0,
                longitude: 0.0,
              },
            },
          });
        } else {
          setTripQueryVariables({
            ...tripQueryVariables,
            to: {
              coordinates: {
                latitude: e.lngLat.lat,
                longitude: e.lngLat.lng,
              },
            },
          });
        }
      }}
    >
      <NavigationControl position="top-left" />
      {tripQueryVariables?.from.coordinates && (
        <Marker
          draggable
          latitude={tripQueryVariables.from.coordinates?.latitude}
          longitude={tripQueryVariables.from.coordinates?.longitude}
          onDragEnd={(e) => {
            setTripQueryVariables({
              ...tripQueryVariables,
              from: { coordinates: { latitude: e.lngLat.lat, longitude: e.lngLat.lng } },
            });
          }}
        />
      )}
      {tripQueryVariables?.to.coordinates && (
        <Marker
          draggable
          latitude={tripQueryVariables.to.coordinates?.latitude}
          longitude={tripQueryVariables.to.coordinates?.longitude}
          onDragEnd={(e) => {
            setTripQueryVariables({
              ...tripQueryVariables,
              to: { coordinates: { latitude: e.lngLat.lat, longitude: e.lngLat.lng } },
            });
          }}
        />
      )}
      {tripQueryResult &&
        tripQueryResult.trip.tripPatterns[0].legs.map(
          (leg) =>
            leg.pointsOnLink && (
              <Source
                key={leg.id}
                //id="polylineLayer"
                type="geojson"
                data={{
                  type: 'Feature',
                  properties: {},
                  geometry: {
                    type: 'LineString',
                    coordinates: decode(leg.pointsOnLink.points as string, 5).map((value) => value.reverse()),
                  },
                }}
              >
                <Layer
                  //id="lineLayer"
                  type="line"
                  //source="my-data"
                  layout={{
                    'line-join': 'round',
                    'line-cap': 'round',
                  }}
                  paint={{
                    'line-color': 'rgba(3, 170, 238, 1)',
                    'line-width': 5,
                  }}
                />
              </Source>
            ),
        )}
    </Map>
  );
}
