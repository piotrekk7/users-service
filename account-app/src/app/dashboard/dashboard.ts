import { Component, inject, OnInit } from '@angular/core';
import { AsyncPipe } from '@angular/common';
import { UserService, User } from '../user.service';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [AsyncPipe],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss'
})
export class DashboardComponent implements OnInit {
  private userService = inject(UserService);
  user$!: Observable<User>;

  ngOnInit(): void {
    this.user$ = this.userService.getUser();
  }
}
