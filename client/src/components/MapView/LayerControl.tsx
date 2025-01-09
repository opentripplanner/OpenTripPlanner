import React, { useEffect, useState, useCallback } from 'react';
import type { ControlPosition } from 'react-map-gl';
import type { MapRef } from 'react-map-gl/maplibre';

interface Layer {
    id: string;
    name: string;
}

interface LayerControlProps {
    mapRef: MapRef | null;
    position: ControlPosition;  // not used in inline styling, but you might use it if you want
    setInteractiveLayerIds: (interactiveLayerIds: string[]) => void;
}

/**
 * A React component to control:
 *   1. Background (raster) layers (select exactly one to show).
 *   2. Debug layers (vector-like layers) with groupings, toggle on/off individually.
 */
const LayerControl: React.FC<LayerControlProps> = ({
                                                       mapRef,
                                                       setInteractiveLayerIds,
                                                   }) => {
    const [rasterLayers, setRasterLayers] = useState<Layer[]>([]);
    const [layerGroups, setLayerGroups] =
        useState<Record<string, Layer[]>>({});

    /**
     * Load background + debug layers from the style once the map is ready.
     */
    useEffect(() => {
        if (!mapRef) return;
        const mapInstance = mapRef.getMap();

        const loadLayers = () => {
            const style = mapInstance.getStyle();
            if (!style || !style.layers) return;

            // 1. Gather all raster layers (for the background selector).
            const rasters = style.layers
                .filter((layer) => layer.type === 'raster')
                .map((layer) => {
                    // Try to pick up a pretty name from metadata if available.
                    let name = layer.id;
                    if ((layer as any).metadata?.name) {
                        name = (layer as any).metadata.name;
                    }
                    return { id: layer.id, name };
                });
            setRasterLayers(rasters);

            // 2. Gather all "debug" layers (i.e. not raster, not "jsx").
            //    Group them by metadata.group (falling back to "Misc").
            const groups: Record<string, Layer[]> = {};
            style.layers
                .filter((layer) => layer.type !== 'raster' && !layer.id.startsWith('jsx'))
                .reverse() // so that the topmost layers appear first
                .forEach((layer) => {
                    const groupName = (layer as any).metadata?.group || 'Misc';
                    if (!groups[groupName]) {
                        groups[groupName] = [];
                    }
                    groups[groupName].push({ id: layer.id, name: layer.id });
                });

            setLayerGroups(groups);
        };

        if (mapInstance.isStyleLoaded()) {
            loadLayers();
        } else {
            mapInstance.on('styledata', loadLayers);
        }

        return () => {
            mapInstance.off('styledata', loadLayers);
        };
    }, [mapRef]);

    /**
     * Toggle the visibility of an individual debug layer.
     */
    const toggleLayerVisibility = useCallback(
        (layerId: string, isVisible: boolean) => {
            if (!mapRef) return;
            const mapInstance = mapRef.getMap();
            mapInstance.setLayoutProperty(layerId, 'visibility', isVisible ? 'visible' : 'none');

            // After toggling, recalculate which interactive layers are visible.
            const style = mapInstance.getStyle();
            if (!style || !style.layers) return;

            const visibleInteractive = style.layers
                .filter((l) => l.type !== 'raster' && !l.id.startsWith('jsx'))
                .filter((l) => mapInstance.getLayoutProperty(l.id, 'visibility') !== 'none')
                .map((l) => l.id);

            setInteractiveLayerIds(visibleInteractive);
        },
        [mapRef, setInteractiveLayerIds]
    );

    /**
     * Show exactly one background (raster) layer at a time.
     */
    const setBackgroundLayer = useCallback(
        (layerId: string) => {
            if (!mapRef) return;
            const mapInstance = mapRef.getMap();
            rasterLayers.forEach((r) => {
                mapInstance.setLayoutProperty(
                    r.id,
                    'visibility',
                    r.id === layerId ? 'visible' : 'none'
                );
            });
        },
        [mapRef, rasterLayers]
    );

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
            {/* BACKGROUND (RASTER) LAYERS */}
            <h4 style={{ marginTop: 0 }}>Background</h4>
            <select onChange={(e) => setBackgroundLayer(e.target.value)}>
                {rasterLayers.map((layer) => (
                    <option key={layer.id} value={layer.id}>
                        {layer.name}
                    </option>
                ))}
            </select>

            {/* DEBUG (VECTOR) LAYERS */}
            <h4 style={{ marginTop: '1rem' }}>Debug Layers</h4>
            {Object.entries(layerGroups).map(([groupName, layers]) => (
                <div key={groupName} style={{ marginBottom: '10px' }}>
                    <h6 style={{ margin: '0 0 5px' }}>{groupName}</h6>
                    {layers.map((layer) => {
                        // Grab the map property for whether it’s visible or not:
                        const isVisible =
                            mapRef?.getMap().getLayoutProperty(layer.id, 'visibility') !== 'none';

                        return (
                            <label
                                key={layer.id}
                                style={{
                                    display: 'block',
                                    cursor: 'pointer',
                                    marginBottom: '5px',
                                }}
                            >
                                <input
                                    type="checkbox"
                                    checked={isVisible}
                                    onChange={(e) =>
                                        toggleLayerVisibility(layer.id, e.target.checked)
                                    }
                                    style={{ marginRight: '5px' }}
                                />
                                {layer.name}
                            </label>
                        );
                    })}
                </div>
            ))}
        </div>
    );
};

export default LayerControl;
