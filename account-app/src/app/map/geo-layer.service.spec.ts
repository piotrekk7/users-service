import { GeoLayerService } from './geo-layer.service';

describe('GeoLayerService', () => {
  let service: GeoLayerService;

  beforeEach(() => {
    service = new GeoLayerService();
  });

  describe('initial visibility', () => {
    it('showCountries is true', () => {
      expect(service.showCountries()).toBe(true);
    });

    it('showRivers is true', () => {
      expect(service.showRivers()).toBe(true);
    });

    it('showCities is true', () => {
      expect(service.showCities()).toBe(true);
    });
  });

  describe('toggleCountries', () => {
    it('flips showCountries to false', () => {
      service.toggleCountries();
      expect(service.showCountries()).toBe(false);
    });

    it('flips showCountries back to true', () => {
      service.toggleCountries();
      service.toggleCountries();
      expect(service.showCountries()).toBe(true);
    });
  });

  describe('toggleRivers', () => {
    it('flips showRivers to false', () => {
      service.toggleRivers();
      expect(service.showRivers()).toBe(false);
    });

    it('flips showRivers back to true', () => {
      service.toggleRivers();
      service.toggleRivers();
      expect(service.showRivers()).toBe(true);
    });
  });

  describe('toggleCities', () => {
    it('flips showCities to false', () => {
      service.toggleCities();
      expect(service.showCities()).toBe(false);
    });

    it('flips showCities back to true', () => {
      service.toggleCities();
      service.toggleCities();
      expect(service.showCities()).toBe(true);
    });
  });

  describe('countriesWmsConfig', () => {
    it('contains LAYERS=geodata:countries', () => {
      expect(service.countriesWmsConfig().params.LAYERS).toBe('geodata:countries');
    });

    it('url contains /geoserver', () => {
      expect(service.countriesWmsConfig().url).toContain('/geoserver');
    });
  });

  describe('riversWmsConfig', () => {
    it('contains LAYERS=geodata:rivers', () => {
      expect(service.riversWmsConfig().params.LAYERS).toBe('geodata:rivers');
    });

    it('url contains /geoserver', () => {
      expect(service.riversWmsConfig().url).toContain('/geoserver');
    });
  });

  describe('citiesWfsUrl', () => {
    it('contains typeName=geodata:cities', () => {
      expect(service.citiesWfsUrl()).toContain('typeName=geodata:cities');
    });

    it('contains outputFormat=application/json', () => {
      expect(service.citiesWfsUrl()).toContain('outputFormat=application/json');
    });
  });

  describe('custom geoserverUrl', () => {
    it('WMS config url uses custom base URL', () => {
      const customService = new GeoLayerService('http://custom-server:8085');
      expect(customService.countriesWmsConfig().url).toContain('http://custom-server:8085');
    });

    it('WFS URL uses custom base URL', () => {
      const customService = new GeoLayerService('http://custom-server:8085');
      expect(customService.citiesWfsUrl()).toContain('http://custom-server:8085');
    });
  });
});
