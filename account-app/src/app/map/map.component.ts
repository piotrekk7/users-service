import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  OnDestroy,
  ViewChild,
  effect,
  inject,
} from '@angular/core';
import Map from 'ol/Map';
import View from 'ol/View';
import TileLayer from 'ol/layer/Tile';
import ImageLayer from 'ol/layer/Image';
import OSM from 'ol/source/OSM';
import ImageWMS from 'ol/source/ImageWMS';
import ScaleLine from 'ol/control/ScaleLine';
import { fromLonLat } from 'ol/proj';
import { GeoLayerService } from './geo-layer.service';
import { MapLayerPanelComponent } from './map-layer-panel.component';

@Component({
  selector: 'app-map',
  standalone: true,
  imports: [MapLayerPanelComponent],
  templateUrl: './map.component.html',
  styleUrl: './map.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MapComponent implements AfterViewInit, OnDestroy {
  @ViewChild('mapContainer') private mapContainer!: ElementRef<HTMLDivElement>;

  private readonly geoLayerService = inject(GeoLayerService);
  private olMap?: Map;
  private countriesLayer?: ImageLayer<ImageWMS>;
  private riversLayer?: ImageLayer<ImageWMS>;

  constructor() {
    effect(() => {
      const visible = this.geoLayerService.showCountries();
      this.countriesLayer?.setVisible(visible);
    });
    effect(() => {
      const visible = this.geoLayerService.showRivers();
      this.riversLayer?.setVisible(visible);
    });
  }

  ngAfterViewInit(): void {
    const countriesConfig = this.geoLayerService.countriesWmsConfig();
    const riversConfig = this.geoLayerService.riversWmsConfig();

    this.countriesLayer = new ImageLayer({
      source: new ImageWMS({
        url: countriesConfig.url,
        params: countriesConfig.params,
        projection: 'EPSG:4326',
      }),
    });

    this.riversLayer = new ImageLayer({
      source: new ImageWMS({
        url: riversConfig.url,
        params: riversConfig.params,
        projection: 'EPSG:4326',
      }),
    });

    this.olMap = new Map({
      target: this.mapContainer.nativeElement,
      layers: [
        new TileLayer({ source: new OSM() }),
        this.countriesLayer,
        this.riversLayer,
      ],
      view: new View({
        center: fromLonLat([15, 50]),
        zoom: 4,
      }),
      controls: [new ScaleLine()],
    });
  }

  ngOnDestroy(): void {
    this.olMap?.setTarget(undefined);
    this.olMap = undefined;
  }
}
