import React, { useEffect, useState } from 'react';
import type { ControlPosition } from 'react-map-gl';
import type { MapRef } from 'react-map-gl/maplibre';

interface LayerControlProps {
  mapRef: MapRef | null;
  position: ControlPosition;
  setInteractiveLayerIds: (interactiveLayerIds: string[]) => void;
}

const LayerControl: React.FC<LayerControlProps> = ({ mapRef, setInteractiveLayerIds }) => {
  const [layerGroups, setLayerGroups] = useState<Record<string, { id: string; name: string }[]>>({});

  useEffect(() => {
    if (!mapRef) return;

    const mapInstance = mapRef.getMap();

    const loadLayers = () => {
      const groups: Record<string, { id: string; name: string }[]> = {};
      const allLayers = mapInstance.getStyle().layers || []; // Get all layers from the map style

      allLayers.reverse().forEach((layer) => {
        if (layer?.type !== 'raster' && !layer?.id.startsWith('jsx')) {
          const metadata = (layer as any)?.metadata || {};
          const groupName = metadata.group || 'Misc';

          if (!groups[groupName]) {
            groups[groupName] = [];
          }

          groups[groupName].push({ id: layer.id, name: layer.id });
        }
      });

      setLayerGroups(groups);
    };

    // Ensure layers are loaded
    if (mapInstance.isStyleLoaded()) {
      loadLayers();
    } else {
      mapInstance.on('styledata', loadLayers);
    }

    return () => {
      mapInstance.off('styledata', loadLayers);
    };
  }, [mapRef]);

  const toggleLayerVisibility = (layerId: string, isVisible: boolean) => {
    if (!mapRef) return;

    const mapInstance = mapRef.getMap();
    mapInstance.setLayoutProperty(layerId, 'visibility', isVisible ? 'visible' : 'none');

    const visibleLayers = Object.values(layerGroups)
      .flat()
      .filter((layer) => mapInstance.getLayoutProperty(layer.id, 'visibility') === 'visible')
      .map((layer) => layer.id);

    setInteractiveLayerIds(visibleLayers);
  };

  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        padding: '10px',
        width: '250px',
        borderRadius: '4px',
        overflowY: 'auto',
      }}
    >
      <h4 style={{ margin: '0 0 10px' }}>Debug Layers</h4>
      {Object.entries(layerGroups).map(([groupName, layers]) => (
        <div key={groupName} style={{ marginBottom: '10px' }}>
          <h6 style={{ margin: '0 0 5px' }}>{groupName}</h6>
          {layers.map((layer) => (
            <label key={layer.id} style={{ display: 'block', cursor: 'pointer', marginBottom: '5px' }}>
              <input
                type="checkbox"
                defaultChecked={mapRef?.getMap().getLayoutProperty(layer.id, 'visibility') !== 'none'}
                onChange={(e) => toggleLayerVisibility(layer.id, e.target.checked)}
                style={{ marginRight: '5px' }}
              />
              {layer.name}
            </label>
          ))}
        </div>
      ))}
    </div>
  );
};

export default LayerControl;
