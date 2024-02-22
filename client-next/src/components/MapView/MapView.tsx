import {
  LngLat,
  Map,
  MapEvent,
  MapGeoJSONFeature,
  MapMouseEvent,
  NavigationControl,
  VectorTileSource,
} from 'react-map-gl/maplibre';
import 'maplibre-gl/dist/maplibre-gl.css';
import { TripPattern, TripQuery, TripQueryVariables } from '../../gql/graphql.ts';
import { NavigationMarkers } from './NavigationMarkers.tsx';
import { LegLines } from './LegLines.tsx';
import { useMapDoubleClick } from './useMapDoubleClick.ts';
import { useState } from 'react';
import { ContextMenuPopup } from './ContextMenuPopup.tsx';
import { GeometryPropertyPopup } from './GeometryPropertyPopup.tsx';
import DebugLayerControl from './LayerControl.tsx';

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

  return (
    <div className="map-container below-content">
      <Map
        // @ts-ignore
        mapLib={import('maplibre-gl')}
        // @ts-ignore
        mapStyle={styleUrl}
        onDblClick={onMapDoubleClick}
        onContextMenu={(e) => {
          setShowContextPopup(e.lngLat);
        }}
        // it's unfortunate that you have to list these layers here.
        // maybe there is a way around it: https://github.com/visgl/react-map-gl/discussions/2343
        interactiveLayerIds={['regular-stop', 'area-stop', 'group-stop', 'vertex', 'edge', 'link']}
        onClick={showFeaturePropPopup}
        // put lat/long in URL and pan to it on page reload
        hash={true}
        // disable pitching and rotating the map
        touchPitch={false}
        dragRotate={false}
        onLoad={panToWorldEnvelopeIfRequired}
      >
        <NavigationControl position="top-left" />
        <NavigationMarkers
          tripQueryVariables={tripQueryVariables}
          setTripQueryVariables={setTripQueryVariables}
          loading={loading}
        />
        <DebugLayerControl position="top-right" />
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
