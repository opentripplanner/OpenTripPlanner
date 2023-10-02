import { Map, NavigationControl } from 'react-map-gl';
import 'maplibre-gl/dist/maplibre-gl.css';
import { TripPattern, TripQuery, TripQueryVariables } from '../../gql/graphql.ts';
import { NavigationMarkers } from './NavigationMarkers.tsx';
import { LegLines } from './LegLines.tsx';
import { useMapDoubleClick } from './useMapDoubleClick.ts';
import { mapStyle } from './mapStyle.ts';

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
  selectedTripPatternIndex,
}: {
  tripQueryVariables: TripQueryVariables;
  setTripQueryVariables: (variables: TripQueryVariables) => void;
  tripQueryResult: TripQuery | null;
  selectedTripPatternIndex: number;
}) {
  const onMapDoubleClick = useMapDoubleClick({ tripQueryVariables, setTripQueryVariables });

  return (
    <Map
      // @ts-ignore
      mapLib={import('maplibre-gl')}
      // @ts-ignore
      mapStyle={mapStyle}
      initialViewState={initialViewState}
      style={{ width: '100%', height: 'calc(100vh - 200px)' }}
      onDblClick={onMapDoubleClick}
    >
      <NavigationControl position="top-left" />
      <NavigationMarkers tripQueryVariables={tripQueryVariables} setTripQueryVariables={setTripQueryVariables} />
      {tripQueryResult?.trip.tripPatterns.length && (
        <LegLines tripPattern={tripQueryResult.trip.tripPatterns[selectedTripPatternIndex] as TripPattern} />
      )}
    </Map>
  );
}
