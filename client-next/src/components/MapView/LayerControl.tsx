import type { ControlPosition } from 'react-map-gl';
import { useControl } from 'react-map-gl';
import { IControl, Map } from 'maplibre-gl';

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

      const title = document.createElement('h6');
      title.textContent = 'Debug layers';
      this.container.appendChild(title);

      map
        .getLayersOrder()
        .map((l) => map.getLayer(l))
        .filter((s) => s?.type !== 'raster')
        .reverse()
        .forEach((layer) => {
          if (layer) {
            const div = document.createElement('div');
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
            div.appendChild(input);
            div.appendChild(label);
            this.container.appendChild(div);
          }
        });
    });

    return this.container;
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
