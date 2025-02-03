import {
  LngLat,
  Map,
  MapEvent,
  MapGeoJSONFeature,
  MapMouseEvent,
  NavigationControl,
  VectorTileSource,
  MapRef,
} from 'react-map-gl/maplibre';
import maplibregl from 'maplibre-gl';
import 'maplibre-gl/dist/maplibre-gl.css';
import { TripPattern, TripQuery, TripQueryVariables } from '../../gql/graphql.ts';
import { NavigationMarkers } from './NavigationMarkers.tsx';
import { LegLines } from './LegLines.tsx';
import { useMapDoubleClick } from './useMapDoubleClick.ts';
import { useState, useCallback, useRef } from 'react';
import { ContextMenuPopup } from './ContextMenuPopup.tsx';
import { GeometryPropertyPopup } from './GeometryPropertyPopup.tsx';
import RightMenu from './RightMenu.tsx';

const styleUrl = import.meta.env.VITE_DEBUG_STYLE_URL;

type PopupData = { coordinates: LngLat; feature: MapGeoJSONFeature };

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
  const [interactiveLayerIds, setInteractiveLayerIds] = useState<string[]>([]);
  const [cursor, setCursor] = useState<string>('auto');
  const onMouseEnter = useCallback(() => setCursor('pointer'), []);
  const onMouseLeave = useCallback(() => setCursor('auto'), []);
  const showFeaturePropPopup = (
    e: MapMouseEvent & {
      features?: MapGeoJSONFeature[] | undefined;
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
  const panToWorldEnvelopeIfRequired = (e: MapEvent) => {
    const map = e.target;
    // if we are really far zoomed out and show the entire world it means that we are not starting
    // in a location selected from the URL hash.
    // in such a case we pan to the area that is specified in the tile bounds, which is
    // provided by the WorldEnvelopeService
    if (map.getZoom() < 2) {
      const source = map.getSource('stops') as VectorTileSource;
      map.fitBounds(source.bounds, { maxDuration: 50, linear: true });
    }
  };

  const onLoad = (e: MapEvent) => {
    const map = e.target;
    map.addControl(new maplibregl.AttributionControl(), 'bottom-left');
  };

  function handleMapLoad(e: MapEvent) {
    // 1) Call your existing function
    panToWorldEnvelopeIfRequired(e);

    // 2) Add the native MapLibre attribution control
    onLoad(e);
  }

  const mapRef = useRef<MapRef>(null); // Create a ref for MapRef
  return (
    <div className="map-container below-content">
      <Map
        attributionControl={false}
        // @ts-ignore
        mapLib={import('maplibre-gl')}
        // @ts-ignore
        mapStyle={styleUrl}
        onDblClick={onMapDoubleClick}
        onContextMenu={(e) => {
          setShowContextPopup(e.lngLat);
        }}
        interactiveLayerIds={interactiveLayerIds}
        cursor={cursor}
        onMouseEnter={onMouseEnter}
        onMouseLeave={onMouseLeave}
        onClick={showFeaturePropPopup}
        // put lat/long in URL and pan to it on page reload
        hash={true}
        // disable pitching and rotating the map
        touchPitch={false}
        dragRotate={false}
        onLoad={handleMapLoad}
        ref={mapRef}
      >
        <NavigationControl position="top-left" />
        <NavigationMarkers
          setCursor={setCursor}
          tripQueryVariables={tripQueryVariables}
          setTripQueryVariables={setTripQueryVariables}
          loading={loading}
        />

        <RightMenu position="top-right" setInteractiveLayerIds={setInteractiveLayerIds} mapRef={mapRef?.current} />
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
