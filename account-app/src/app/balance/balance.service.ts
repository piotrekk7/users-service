import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface BalanceEvent {
  accountId: string;
  balance: number;
}

@Injectable({ providedIn: 'root' })
export class BalanceService {
  private readonly SSE_URL = '/balance/stream';

  events(): Observable<BalanceEvent> {
    return new Observable<BalanceEvent>((observer) => {
      const source = new EventSource(this.SSE_URL);

      source.onmessage = (event: MessageEvent) => {
        try {
          observer.next(JSON.parse(event.data) as BalanceEvent);
        } catch {
          observer.error(new Error(`Failed to parse SSE data: ${event.data}`));
        }
      };

      source.onerror = () => {
        observer.error(new Error('SSE connection error'));
        source.close();
      };

      return () => source.close();
    });
  }
}
