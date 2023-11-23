import { LngLat, Map, NavigationControl, Popup } from 'react-map-gl';
import 'maplibre-gl/dist/maplibre-gl.css';
import { TripPattern, TripQuery, TripQueryVariables } from '../../gql/graphql.ts';
import { NavigationMarkers } from './NavigationMarkers.tsx';
import { LegLines } from './LegLines.tsx';
import { useMapDoubleClick } from './useMapDoubleClick.ts';
import { mapStyle } from './mapStyle.ts';
import { useState } from 'react';
import { Button, ButtonGroup } from 'react-bootstrap';

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
        mapStyle={mapStyle}
        initialViewState={initialViewState}
        onDblClick={onMapDoubleClick}
        onContextMenu={(e) => {
          setShowPopup(e.lngLat);
        }}
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
          <Popup longitude={showPopup.lng} latitude={showPopup.lat} anchor="bottom" onClose={() => setShowPopup(null)}>
            <ButtonGroup vertical>
              <Button
                onClick={() => {
                  if (showPopup) {
                    setTripQueryVariables({
                      ...tripQueryVariables,
                      from: {
                        coordinates: {
                          latitude: showPopup.lat,
                          longitude: showPopup.lng,
                        },
                      },
                    });
                    setShowPopup(null);
                  }
                }}
              >
                Start here
              </Button>
              <Button
                onClick={() => {
                  if (showPopup) {
                    setTripQueryVariables({
                      ...tripQueryVariables,
                      to: {
                        coordinates: {
                          latitude: showPopup.lat,
                          longitude: showPopup.lng,
                        },
                      },
                    });
                    setShowPopup(null);
                  }
                }}
              >
                End here
              </Button>
            </ButtonGroup>
          </Popup>
        )}
      </Map>
    </div>
  );
}
