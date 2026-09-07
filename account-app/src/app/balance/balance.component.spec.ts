import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BalanceComponent } from './balance.component';
import { BalanceService, BalanceEvent } from './balance.service';
import { Subject } from 'rxjs';

describe('BalanceComponent', () => {
  let fixture: ComponentFixture<BalanceComponent>;
  let component: BalanceComponent;
  let subject$: Subject<BalanceEvent>;

  beforeEach(async () => {
    subject$ = new Subject<BalanceEvent>();

    await TestBed.configureTestingModule({
      imports: [BalanceComponent],
      providers: [{ provide: BalanceService, useValue: { events: () => subject$.asObservable() } }],
    }).compileComponents();

    fixture = TestBed.createComponent(BalanceComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should render no rows initially', () => {
    const rows = fixture.nativeElement.querySelectorAll('tbody tr');
    expect(rows.length).toBe(0);
  });

  it('should render a row when an event is emitted', () => {
    subject$.next({ accountId: 'account1', balance: 1234.56 });
    fixture.detectChanges();

    const rows = fixture.nativeElement.querySelectorAll('tbody tr');
    expect(rows.length).toBe(1);
    expect(rows[0].cells[0].textContent).toBe('account1');
    expect(rows[0].cells[1].textContent).toContain('1,234.56');
  });

  it('should update balance for existing account on subsequent event', () => {
    subject$.next({ accountId: 'account1', balance: 100 });
    fixture.detectChanges();
    subject$.next({ accountId: 'account1', balance: 200 });
    fixture.detectChanges();

    const rows = fixture.nativeElement.querySelectorAll('tbody tr');
    expect(rows.length).toBe(1);
    expect(rows[0].cells[1].textContent).toContain('200');
  });

  it('should render a separate row for each account', () => {
    subject$.next({ accountId: 'account1', balance: 100 });
    subject$.next({ accountId: 'account2', balance: 200 });
    subject$.next({ accountId: 'account3', balance: 300 });
    fixture.detectChanges();

    const rows = fixture.nativeElement.querySelectorAll('tbody tr');
    expect(rows.length).toBe(3);
  });
});
