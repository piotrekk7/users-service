import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  OnDestroy,
  ViewChild,
  effect,
  inject,
  signal,
} from '@angular/core';
import Map from 'ol/Map';
import View from 'ol/View';
import TileLayer from 'ol/layer/Tile';
import ImageLayer from 'ol/layer/Image';
import VectorLayer from 'ol/layer/Vector';
import OSM from 'ol/source/OSM';
import ImageWMS from 'ol/source/ImageWMS';
import VectorSource from 'ol/source/Vector';
import GeoJSON from 'ol/format/GeoJSON';
import Style from 'ol/style/Style';
import CircleStyle from 'ol/style/Circle';
import Fill from 'ol/style/Fill';
import Stroke from 'ol/style/Stroke';
import Overlay from 'ol/Overlay';
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
  @ViewChild('popup') private popupRef!: ElementRef<HTMLDivElement>;

  private readonly geoLayerService = inject(GeoLayerService);
  private olMap?: Map;
  private countriesLayer?: ImageLayer<ImageWMS>;
  private riversLayer?: ImageLayer<ImageWMS>;
  private citiesLayer?: VectorLayer<VectorSource>;
  private popupOverlay?: Overlay;

  protected readonly popupCity = signal('');
  protected readonly popupCountry = signal('');

  constructor() {
    effect(() => {
      const visible = this.geoLayerService.showCountries();
      this.countriesLayer?.setVisible(visible);
    });
    effect(() => {
      const visible = this.geoLayerService.showRivers();
      this.riversLayer?.setVisible(visible);
    });
    effect(() => {
      const visible = this.geoLayerService.showCities();
      this.citiesLayer?.setVisible(visible);
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

    this.citiesLayer = new VectorLayer({
      source: new VectorSource({
        url: this.geoLayerService.citiesWfsUrl(),
        format: new GeoJSON(),
      }),
      style: new Style({
        image: new CircleStyle({
          radius: 5,
          fill: new Fill({ color: '#e07b5a' }),
          stroke: new Stroke({ color: '#b5432a', width: 1 }),
        }),
      }),
    });

    this.popupOverlay = new Overlay({
      element: this.popupRef.nativeElement,
      positioning: 'bottom-center',
      stopEvent: true,
      offset: [0, -10],
    });

    this.olMap = new Map({
      target: this.mapContainer.nativeElement,
      layers: [
        new TileLayer({ source: new OSM() }),
        this.countriesLayer,
        this.riversLayer,
        this.citiesLayer,
      ],
      overlays: [this.popupOverlay],
      view: new View({
        center: fromLonLat([15, 50]),
        zoom: 4,
      }),
      controls: [new ScaleLine()],
    });

    this.olMap.on('click', (event) => {
      const feature = this.olMap!.forEachFeatureAtPixel(
        event.pixel,
        (f) => f,
        { hitTolerance: 5 },
      );

      if (feature) {
        this.popupCity.set((feature.get('name') as string) ?? '');
        this.popupCountry.set((feature.get('adm0name') as string) ?? '');
        this.popupOverlay!.setPosition(event.coordinate);
      } else {
        this.closePopup();
      }
    });
  }

  closePopup(): void {
    this.popupOverlay?.setPosition(undefined);
  }

  ngOnDestroy(): void {
    this.olMap?.setTarget(undefined);
    this.olMap = undefined;
  }
}
