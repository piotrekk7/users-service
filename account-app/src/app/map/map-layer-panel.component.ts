import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { GeoLayerService } from './geo-layer.service';

@Component({
  selector: 'app-map-layer-panel',
  standalone: true,
  templateUrl: './map-layer-panel.component.html',
  styleUrl: './map-layer-panel.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MapLayerPanelComponent {
  private readonly geoLayerService = inject(GeoLayerService);

  readonly showCountries = this.geoLayerService.showCountries;
  readonly showRivers = this.geoLayerService.showRivers;
  readonly showCities = this.geoLayerService.showCities;

  toggleCountries(): void {
    this.geoLayerService.toggleCountries();
  }

  toggleRivers(): void {
    this.geoLayerService.toggleRivers();
  }

  toggleCities(): void {
    this.geoLayerService.toggleCities();
  }
}
