import { Component, ChangeDetectionStrategy, inject, computed } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { scan } from 'rxjs';
import { DecimalPipe } from '@angular/common';
import { BalanceService, BalanceEvent } from './balance.service';

@Component({
  selector: 'app-balance',
  standalone: true,
  imports: [DecimalPipe],
  templateUrl: './balance.component.html',
  styleUrl: './balance.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BalanceComponent {
  private balanceService = inject(BalanceService);

  private balances = toSignal(
    this.balanceService
      .events()
      .pipe(
        scan(
          (acc: Record<string, BalanceEvent>, event) => ({ ...acc, [event.accountId]: event }),
          {},
        ),
      ),
    { initialValue: {} },
  );

  accounts = computed(() => Object.values(this.balances()));
}
