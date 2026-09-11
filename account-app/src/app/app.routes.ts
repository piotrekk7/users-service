import { Routes } from '@angular/router';
import { DashboardComponent } from './dashboard/dashboard';
import { BalanceComponent } from './balance/balance.component';
import { MapComponent } from './map/map.component';

export const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  { path: 'dashboard', component: DashboardComponent },
  { path: 'balance', component: BalanceComponent },
  { path: 'map', component: MapComponent },
];
