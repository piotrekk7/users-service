import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { MapLayerPanelComponent } from './map-layer-panel.component';
import { GeoLayerService } from './geo-layer.service';

describe('MapLayerPanelComponent', () => {
  let fixture: ComponentFixture<MapLayerPanelComponent>;
  let toggleCountries: ReturnType<typeof vi.fn>;
  let toggleRivers: ReturnType<typeof vi.fn>;
  let toggleCities: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    toggleCountries = vi.fn();
    toggleRivers = vi.fn();
    toggleCities = vi.fn();

    const mockService = {
      showCountries: signal(true),
      showRivers: signal(true),
      showCities: signal(true),
      toggleCountries,
      toggleRivers,
      toggleCities,
    };

    await TestBed.configureTestingModule({
      imports: [MapLayerPanelComponent],
      providers: [{ provide: GeoLayerService, useValue: mockService }],
    }).compileComponents();

    fixture = TestBed.createComponent(MapLayerPanelComponent);
    fixture.detectChanges();
  });

  describe('checkboxes', () => {
    it('renders three checkboxes', () => {
      const checkboxes = fixture.nativeElement.querySelectorAll('input[type="checkbox"]');
      expect(checkboxes.length).toBe(3);
    });

    it('clicking Countries checkbox calls toggleCountries()', () => {
      const checkboxes = fixture.nativeElement.querySelectorAll('input[type="checkbox"]');
      checkboxes[0].dispatchEvent(new Event('change'));
      expect(toggleCountries).toHaveBeenCalledOnce();
    });

    it('clicking Rivers checkbox calls toggleRivers()', () => {
      const checkboxes = fixture.nativeElement.querySelectorAll('input[type="checkbox"]');
      checkboxes[1].dispatchEvent(new Event('change'));
      expect(toggleRivers).toHaveBeenCalledOnce();
    });
  });

  describe('legend', () => {
    it('renders three legend entries', () => {
      const entries = fixture.nativeElement.querySelectorAll('.legend-entry');
      expect(entries.length).toBe(3);
    });

    it('legend contains Countries label', () => {
      const text = fixture.nativeElement.textContent as string;
      expect(text).toContain('Countries');
    });

    it('legend contains Rivers label', () => {
      const text = fixture.nativeElement.textContent as string;
      expect(text).toContain('Rivers');
    });

    it('legend contains Cities label', () => {
      const text = fixture.nativeElement.textContent as string;
      expect(text).toContain('Cities');
    });
  });
});
