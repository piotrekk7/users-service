import { computed, Inject, Injectable, InjectionToken, Optional, signal } from '@angular/core';

export const GEOSERVER_URL = new InjectionToken<string>('geoserver.url');

export interface WmsSourceConfig {
  url: string;
  params: { LAYERS: string; TILED: boolean };
}

@Injectable({ providedIn: 'root' })
export class GeoLayerService {
  private readonly geoserverUrl: string;

  constructor(@Optional() @Inject(GEOSERVER_URL) geoserverUrl: string | null = null) {
    this.geoserverUrl = geoserverUrl ?? '/geoserver';
  }

  readonly showCountries = signal(true);
  readonly showRivers = signal(true);
  readonly showCities = signal(true);

  toggleCountries(): void {
    this.showCountries.update((v) => !v);
  }

  toggleRivers(): void {
    this.showRivers.update((v) => !v);
  }

  toggleCities(): void {
    this.showCities.update((v) => !v);
  }

  readonly countriesWmsConfig = computed<WmsSourceConfig>(() => ({
    url: `${this.geoserverUrl}/wms`,
    params: { LAYERS: 'geodata:countries', TILED: true },
  }));

  readonly riversWmsConfig = computed<WmsSourceConfig>(() => ({
    url: `${this.geoserverUrl}/wms`,
    params: { LAYERS: 'geodata:rivers', TILED: true },
  }));

  readonly citiesWfsUrl = computed<string>(
    () =>
      `${this.geoserverUrl}/ows?service=WFS&version=1.0.0&request=GetFeature&typeName=geodata:cities&outputFormat=application/json`,
  );
}
