import React, { useCallback, useEffect, useRef, useState } from 'react';
import type { ControlPosition, MapRef } from 'react-map-gl/maplibre';
import { findSelectedDebugLayers } from '../../util/map.ts';

interface Layer {
  id: string;
  name: string;
}

/** The group whose layers can be narrowed to a set of vehicle rental networks. */
const RENTAL_GROUP = 'Rental';

/**
 * MapLibre's legacy filter syntax names the property directly, as in ["in", "class", "StreetEdge"],
 * where the expression syntax wraps it: ["in", ["get", "class"], ["literal", [...]]]. The debug
 * style uses both - the rental vehicle and station layers are class-filtered in the legacy form,
 * the geofencing zone layers use expressions - and the two cannot be mixed inside one filter.
 */
function isLegacyFilter(filter: unknown): boolean {
  return Array.isArray(filter) && filter[0] === 'in' && typeof filter[1] === 'string';
}

function networkFilter(serverFilter: unknown, networks: Set<string>): unknown[] {
  return isLegacyFilter(serverFilter)
    ? ['in', 'network', ...networks]
    : ['in', ['get', 'network'], ['literal', [...networks]]];
}

interface LayerControlProps {
  mapRef: MapRef | null;
  position: ControlPosition; // not used in inline styling, but you might use it if you want
  setInteractiveLayerIds: (interactiveLayerIds: string[]) => void;
}

/**
 * A React component to control:
 *   1. Background (raster) layers (select exactly one to show).
 *   2. Debug layers (vector-like layers) with groupings, toggle on/off individually.
 */
const LayerControl: React.FC<LayerControlProps> = ({ mapRef, setInteractiveLayerIds }) => {
  const [rasterLayers, setRasterLayers] = useState<Layer[]>([]);
  const [layerGroups, setLayerGroups] = useState<Record<string, Layer[]>>({});
  const [rentalNetworks, setRentalNetworks] = useState<string[]>([]);
  const [selectedNetworks, setSelectedNetworks] = useState<Set<string>>(new Set());

  /**
   * The filter each rental layer was given by the server, captured before we narrow it. Restoring
   * it is what makes "all networks selected" identical to an untouched style rather than merely
   * equivalent to it.
   */
  const serverFilters = useRef<Map<string, unknown>>(new Map());

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
          const layerName = (layer.metadata as Record<string, string>)?.name;
          if (layerName) {
            name = layerName;
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
          const groupName = (layer.metadata as Record<string, string>)?.group || 'Misc';
          if (!groups[groupName]) {
            groups[groupName] = [];
          }
          groups[groupName].push({ id: layer.id, name: layer.id });
        });

      setLayerGroups(groups);

      // The networks cannot be derived from the tiles: a tile only reveals the networks present in
      // the area currently loaded, so the list would change as you pan and be empty when zoomed out.
      const networks = (style.metadata as Record<string, string[]> | undefined)?.rentalNetworks ?? [];
      setRentalNetworks(networks);
      setSelectedNetworks((previous) => (previous.size === 0 ? new Set(networks) : previous));
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
      const selected = findSelectedDebugLayers(mapInstance);
      setInteractiveLayerIds(selected);
    },
    [mapRef, setInteractiveLayerIds],
  );

  /**
   * Narrow the rental layers to the given networks, leaving every other layer untouched.
   */
  const applyNetworkFilter = useCallback(
    (networks: Set<string>) => {
      if (!mapRef) return;
      const mapInstance = mapRef.getMap();

      (layerGroups[RENTAL_GROUP] ?? []).forEach((layer) => {
        if (!serverFilters.current.has(layer.id)) {
          serverFilters.current.set(layer.id, mapInstance.getFilter(layer.id));
        }
        const serverFilter = serverFilters.current.get(layer.id);

        // setFilter replaces rather than merges, so the server's own filter - which selects the
        // vehicle, station or zone type the layer draws - has to be reapplied alongside ours, in
        // the same dialect: an expression cannot be combined with a legacy filter.
        const filter =
          networks.size === rentalNetworks.length
            ? serverFilter
            : ['all', serverFilter, networkFilter(serverFilter, networks)];

        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        mapInstance.setFilter(layer.id, filter as any);
      });
    },
    [mapRef, layerGroups, rentalNetworks],
  );

  const toggleNetwork = useCallback(
    (network: string, isSelected: boolean) => {
      const next = new Set(selectedNetworks);
      if (isSelected) {
        next.add(network);
      } else {
        next.delete(network);
      }
      setSelectedNetworks(next);
      applyNetworkFilter(next);
    },
    [selectedNetworks, applyNetworkFilter],
  );

  const setAllNetworks = useCallback(
    (isSelected: boolean) => {
      const next = isSelected ? new Set(rentalNetworks) : new Set<string>();
      setSelectedNetworks(next);
      applyNetworkFilter(next);
    },
    [rentalNetworks, applyNetworkFilter],
  );

  /**
   * Show exactly one background (raster) layer at a time.
   */
  const setBackgroundLayer = useCallback(
    (layerId: string) => {
      if (!mapRef) return;
      const mapInstance = mapRef.getMap();
      rasterLayers.forEach((r) => {
        mapInstance.setLayoutProperty(r.id, 'visibility', r.id === layerId ? 'visible' : 'none');
      });
    },
    [mapRef, rasterLayers],
  );

  return (
    <div className="layer-control">
      {/* BACKGROUND (RASTER) LAYERS */}
      <h4>Background</h4>
      <select onChange={(e) => setBackgroundLayer(e.target.value)}>
        {rasterLayers.map((layer) => (
          <option key={layer.id} value={layer.id}>
            {layer.name}
          </option>
        ))}
      </select>

      {/* DEBUG (VECTOR) LAYERS */}
      <h4>Debug Layers</h4>
      {Object.entries(layerGroups).map(([groupName, layers]) => {
        // Determine if *all* layers in this group are currently visible.
        const allVisible = layers.every(
          (layer) => mapRef?.getMap().getLayoutProperty(layer.id, 'visibility') !== 'none',
        );

        // Networks filter the rental layers, so the group needs a second, labelled list.
        const showNetworks = groupName === RENTAL_GROUP && rentalNetworks.length > 0;

        // Define a helper to toggle all layers in the group at once.
        const toggleGroupVisibility = (checked: boolean) => {
          layers.forEach((layer) => {
            toggleLayerVisibility(layer.id, checked);
          });
        };

        return (
          <div key={groupName} className="layer-group">
            <h6>
              <label className="group-label">
                <input type="checkbox" checked={allVisible} onChange={(e) => toggleGroupVisibility(e.target.checked)} />
                {groupName}
              </label>
            </h6>

            {showNetworks && <div className="sub-heading">Layers</div>}

            {layers.map((layer) => {
              // Figure out if the layer is visible or not:
              const isVisible = mapRef?.getMap().getLayoutProperty(layer.id, 'visibility') !== 'none';

              return (
                <label key={layer.id} className="toggle">
                  <input
                    type="checkbox"
                    checked={isVisible}
                    onChange={(e) => toggleLayerVisibility(layer.id, e.target.checked)}
                  />
                  {layer.name}
                </label>
              );
            })}

            {showNetworks && (
              <div className="networks">
                <div className="sub-heading">
                  <span>Networks</span>
                  <button type="button" className="link-button" onClick={() => setAllNetworks(true)}>
                    all
                  </button>
                  <button type="button" className="link-button" onClick={() => setAllNetworks(false)}>
                    none
                  </button>
                </div>
                <div className="network-list">
                  {rentalNetworks.map((network) => (
                    <label key={network}>
                      <input
                        type="checkbox"
                        checked={selectedNetworks.has(network)}
                        onChange={(e) => toggleNetwork(network, e.target.checked)}
                      />
                      {network}
                    </label>
                  ))}
                </div>
              </div>
            )}
          </div>
        );
      })}
    </div>
  );
};

export default LayerControl;
