import type { ControlPosition } from 'react-map-gl';
import { useControl } from 'react-map-gl';
import { IControl, Map, TypedStyleLayer } from 'maplibre-gl';

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

  onAdd(map: Map) {
    this.container.className = 'maplibregl-ctrl maplibregl-ctrl-group layer-select';

    map.on('load', () => {
      // clean on
      while (this.container.firstChild) {
        this.container.removeChild(this.container.firstChild);
      }

      const title = document.createElement('h4');
      title.textContent = 'Debug layers';
      this.container.appendChild(title);

      const groups: Record<string, HTMLDivElement> = {};
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

            const layerDiv = this.buildLayerDiv(layer as TypedStyleLayer, map);

            if (groups[groupName]) {
              groups[groupName]?.appendChild(layerDiv);
            } else {
              const groupDiv = this.buildGroupDiv(groupName, layerDiv);
              groups[groupName] = groupDiv;
              this.container.appendChild(groupDiv);
            }
          }
        });
    });

    return this.container;
  }

  private buildLayerDiv(layer: TypedStyleLayer, map: Map) {
    const layerDiv = document.createElement('div');
    layerDiv.className = 'layer';
    const input = document.createElement('input');
    input.type = 'checkbox';
    input.value = layer.id;
    input.id = layer.id;
    input.onchange = (e) => {
      e.preventDefault();
      e.stopPropagation();
      if (input.checked) {
        map.setLayoutProperty(layer.id, 'visibility', 'visible');
      } else {
        map.setLayoutProperty(layer.id, 'visibility', 'none');
      }
    };
    input.checked = this.layerVisible(map, layer);
    input.className = 'layer';
    const label = document.createElement('label');
    label.textContent = layer.id;
    label.htmlFor = layer.id;
    layerDiv.appendChild(input);
    layerDiv.appendChild(label);
    return layerDiv;
  }

  private buildGroupDiv(groupName: string, layerDiv: HTMLDivElement) {
    const groupDiv = document.createElement('div');
    groupDiv.className = 'group';

    const groupInput = document.createElement('input');
    groupInput.onchange = () => {
      groupDiv.querySelectorAll<HTMLInputElement>('input.layer').forEach((input) => {
        input.checked = groupInput.checked;
        const event = new Event('change');
        input.dispatchEvent(event);
      });
    };
    groupInput.type = 'checkbox';
    groupInput.id = groupName;

    const groupLabel = document.createElement('label');
    groupLabel.textContent = groupName;
    groupLabel.htmlFor = groupName;
    groupLabel.className = 'group-label';

    groupDiv.appendChild(groupInput);
    groupDiv.appendChild(groupLabel);
    groupDiv.appendChild(layerDiv);
    return groupDiv;
  }

  private layerVisible(map: Map, layer: { id: string }) {
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
