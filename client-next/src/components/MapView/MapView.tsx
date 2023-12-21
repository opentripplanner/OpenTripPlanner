import { LngLat, Map, NavigationControl } from 'react-map-gl';
import 'maplibre-gl/dist/maplibre-gl.css';
import { TripPattern, TripQuery, TripQueryVariables } from '../../gql/graphql.ts';
import { NavigationMarkers } from './NavigationMarkers.tsx';
import { LegLines } from './LegLines.tsx';
import { useMapDoubleClick } from './useMapDoubleClick.ts';
import { useState } from 'react';
import { ContextMenuPopup } from './ContextMenuPopup.tsx';

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
  loading,
}: {
  tripQueryVariables: TripQueryVariables;
  setTripQueryVariables: (variables: TripQueryVariables) => void;
  tripQueryResult: TripQuery | null;
  selectedTripPatternIndex: number;
  loading: boolean;
}) {
  const onMapDoubleClick = useMapDoubleClick({ tripQueryVariables, setTripQueryVariables });
  const [showPopup, setShowPopup] = useState<LngLat | null>(null);

  return (
    <div className="map-container below-content">
      <Map
        // @ts-ignore
        mapLib={import('maplibre-gl')}
        // @ts-ignore
        mapStyle="http://localhost:8080/otp/routers/default/inspector/vectortile/style.json"
        initialViewState={initialViewState}
        onDblClick={onMapDoubleClick}
        onContextMenu={(e) => {
          setShowPopup(e.lngLat);
        }}
        interactiveLayerIds={["regular-stop"]}
        onClick={e => {
          console.log(e.features);
        }}
        // put lat/long in URL and pan to it on page reload
        hash={true}
        // disable pitching and rotating the map
        touchPitch={false}
        dragRotate={false}
      >
        <NavigationControl position="top-left" />
        <NavigationMarkers
          tripQueryVariables={tripQueryVariables}
          setTripQueryVariables={setTripQueryVariables}
          loading={loading}
        />
        {tripQueryResult?.trip.tripPatterns.length && (
          <LegLines tripPattern={tripQueryResult.trip.tripPatterns[selectedTripPatternIndex] as TripPattern} />
        )}
        {showPopup && (
          <ContextMenuPopup
            tripQueryVariables={tripQueryVariables}
            setTripQueryVariables={setTripQueryVariables}
            coordinates={showPopup}
            onClose={() => setShowPopup(null)}
          />
        )}
      </Map>
    </div>
  );
}
