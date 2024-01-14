import type { ControlPosition } from 'react-map-gl';
import { useControl } from 'react-map-gl';
import { Map } from 'maplibre-gl';

type LayerControlProps = {
  position: ControlPosition;
};

class LayerList {
  private map: Map | null = null;
  private readonly container: HTMLDivElement = document.createElement('div');

  onAdd(map: Map) {
    this.map = map;
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
            input.value = layer?.id;
            input.onchange = (e) => {
              e.preventDefault();
              e.stopPropagation();

              if (this.layerVisible(layer)) {
                map.setLayoutProperty(layer.id, 'visibility', 'none');
              } else {
                map.setLayoutProperty(layer.id, 'visibility', 'visible');
              }
            };
            input.checked = this.layerVisible(layer);
            const label = document.createElement('label');
            label.textContent = layer.id;
            div.appendChild(input);
            div.appendChild(label);
            this.container.appendChild(div);
          }
        });
    });

    return this.container;
  }

  private layerVisible(layer: { id: string }) {
    return this.map?.getLayoutProperty(layer.id, 'visibility') !== 'none';
  }

  onCreate() {}

  onRemove() {
    this.container.parentNode?.removeChild(this.container);
    this.map = null;
  }
}

export default function LayerListControl(props: LayerControlProps) {
  useControl(() => new LayerList(), {
    position: props.position,
  });

  return null;
}
