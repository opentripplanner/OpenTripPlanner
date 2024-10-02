import type { ControlPosition } from 'react-map-gl';
import { useControl } from 'react-map-gl';
import { IControl, Map as WebMap } from 'maplibre-gl';

type LayerControlProps = {
  position: ControlPosition;
};

/**
 * A maplibre control that allows you to switch vector tile layers on and off.
 *
 * It appears that you cannot use React elements but have to drop down to raw DOM. Please correct
 * me if I'm wrong.
 */
class LayerControl implements IControl {
  private readonly container: HTMLDivElement = document.createElement('div');

  onAdd(map: WebMap) {
    this.container.className = 'maplibregl-ctrl maplibregl-ctrl-group layer-select';

    map.on('load', () => {
      // clean on
      while (this.container.firstChild) {
        this.container.removeChild(this.container.firstChild);
      }

      const title = document.createElement('h4');
      title.textContent = 'Debug layers';
      this.container.appendChild(title);

      const groups: Map<string, HTMLDivElement> = new Map<string, HTMLDivElement>();
      map
        .getLayersOrder()
        .map((l) => map.getLayer(l))
        .filter((s) => s?.type !== 'raster')
        // the polylines of the routing result are put in map layers called jsx-1, jsx-2...
        // we don't want them to show up in the debug layer selector
        .filter((s) => !s?.id.startsWith('jsx'))
        .reverse()
        .forEach((layer) => {
          if (layer) {
            const meta: { group: string } = layer.metadata as { group: string };

            let groupName: string = 'Misc';
            if (meta.group) {
              groupName = meta.group;
            }
            console.log(groupName);

            const layerDiv = document.createElement('div');
            layerDiv.className = 'layer';
            const input = document.createElement('input');
            input.type = 'checkbox';
            input.value = layer.id;
            input.id = layer.id;
            input.onchange = (e) => {
              e.preventDefault();
              e.stopPropagation();

              if (this.layerVisible(map, layer)) {
                map.setLayoutProperty(layer.id, 'visibility', 'none');
              } else {
                map.setLayoutProperty(layer.id, 'visibility', 'visible');
              }
            };
            input.checked = this.layerVisible(map, layer);
            const label = document.createElement('label');
            label.textContent = layer.id;
            label.htmlFor = layer.id;
            layerDiv.appendChild(input);
            layerDiv.appendChild(label);

            if (groups.has(groupName)) {
              const g = groups.get(groupName);
              g?.appendChild(layerDiv);
            } else {
              const input = document.createElement('input');
              input.type = 'checkbox';
              input.id = groupName;

              const label = document.createElement('label');
              label.textContent = groupName;
              label.htmlFor = groupName;

              const groupDiv = document.createElement('div');
              groupDiv.className = 'group';
              groupDiv.appendChild(input);
              groupDiv.appendChild(label);
              groupDiv.appendChild(layerDiv);
              groups.set(groupName, groupDiv);
              this.container.appendChild(groupDiv);
            }
          }
        });
    });

    return this.container;
  }

  private layerVisible(map: WebMap, layer: { id: string }) {
    return map.getLayoutProperty(layer.id, 'visibility') !== 'none';
  }

  onRemove() {
    this.container.parentNode?.removeChild(this.container);
  }
}

export default function DebugLayerControl(props: LayerControlProps) {
  useControl(() => new LayerControl(), {
    position: props.position,
  });

  return null;
}
