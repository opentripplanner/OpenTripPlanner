import { TripQuery } from '../../gql/graphql.ts';
import { Layer, Source } from 'react-map-gl';
import { decode } from '@googlemaps/polyline-codec';
import { getColorForMode } from '../../util/getColorForMode.ts';

export function LegLines({ tripQueryResult }: { tripQueryResult: TripQuery | null }) {
  return (
    <>
      {tripQueryResult?.trip.tripPatterns.length &&
        tripQueryResult.trip.tripPatterns[0].legs.map(
          (leg) =>
            leg.pointsOnLink && (
              <Source
                key={leg.id}
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
