import { LngLat, Map, MapboxGeoJSONFeature, NavigationControl } from 'react-map-gl';
import 'maplibre-gl/dist/maplibre-gl.css';
import { TripPattern, TripQuery, TripQueryVariables } from '../../gql/graphql.ts';
import { NavigationMarkers } from './NavigationMarkers.tsx';
import { LegLines } from './LegLines.tsx';
import { useMapDoubleClick } from './useMapDoubleClick.ts';
import { useState } from 'react';
import { ContextMenuPopup } from './ContextMenuPopup.tsx';
import { GeometryPropertyPopup } from './GeometryPropertyPopup.tsx';

// TODO: this should be configurable
const initialViewState = {
  latitude: 60.7554885,
  longitude: 10.2332855,
  zoom: 4,
};

const styleUrl = import.meta.env.VITE_DEBUG_STYLE_URL;

type PopupData = { coordinates: LngLat; feature: MapboxGeoJSONFeature };

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
  const [showContextPopup, setShowContextPopup] = useState<LngLat | null>(null);
  const [showPropsPopup, setShowPropsPopup] = useState<PopupData | null>(null);
  const showFeaturePropPopup = (
    e: mapboxgl.MapMouseEvent & {
      features?: mapboxgl.MapboxGeoJSONFeature[] | undefined;
    },
  ) => {
    if (e.features) {
      // if you click on a cluster of map features it's possible that there are multiple
      // to select from. we are using the first one instead of presenting a selection UI.
      // you can always zoom in closer if you want to make a more specific click.
      const feature = e.features[0];
      setShowPropsPopup({ coordinates: e.lngLat, feature: feature });
    }
  };

  return (
    <div className="map-container below-content">
      <Map
        // @ts-ignore
        mapLib={import('maplibre-gl')}
        // @ts-ignore
        mapStyle={styleUrl}
        initialViewState={initialViewState}
        onDblClick={onMapDoubleClick}
        onContextMenu={(e) => {
          setShowContextPopup(e.lngLat);
        }}
        interactiveLayerIds={['regular-stop']}
        onClick={showFeaturePropPopup}
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
        {showContextPopup && (
          <ContextMenuPopup
            tripQueryVariables={tripQueryVariables}
            setTripQueryVariables={setTripQueryVariables}
            coordinates={showContextPopup}
            onClose={() => setShowContextPopup(null)}
          />
        )}
        {showPropsPopup?.feature?.properties && (
          <GeometryPropertyPopup
            coordinates={showPropsPopup?.coordinates}
            properties={showPropsPopup?.feature?.properties}
            onClose={() => setShowPropsPopup(null)}
          />
        )}
      </Map>
    </div>
  );
}
