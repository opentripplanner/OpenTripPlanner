import { TripPattern } from '../../gql/graphql.ts';
import { Layer, Source } from 'react-map-gl';
import { decode } from '@googlemaps/polyline-codec';
import { getColorForMode } from '../../util/getColorForMode.ts';

export function LegLines({ tripPattern }: { tripPattern?: TripPattern }) {
  return (
    <>
      {tripPattern?.legs.map(
        (leg, i) =>
          leg.pointsOnLink && (
            <Source
              key={leg.id || `footleg_${i}`}
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
                type="line"
                layout={{
                  'line-join': 'round',
                  'line-cap': 'round',
                }}
                paint={{
                  'line-color': getColorForMode(leg.mode),
                  'line-width': 5,
                }}
              />
            </Source>
          ),
      )}
    </>
  );
}
