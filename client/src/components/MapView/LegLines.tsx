import { TripPattern } from '../../gql/graphql.ts';
import { Layer, Source } from 'react-map-gl/maplibre';
import { decode } from '@googlemaps/polyline-codec';
import { getColorForLeg } from '../../util/getColorForLeg.ts';

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
                  'line-color': '#000',
                  'line-width': 5,
                }}
              />
              <Layer
                type="line"
                layout={{
                  'line-join': 'round',
                  'line-cap': 'round',
                }}
                paint={{
                  'line-color': getColorForLeg(leg),
                  'line-width': 4,
                }}
              />
            </Source>
          ),
      )}
    </>
  );
}
