import {Injectable, signal, computed, inject, WritableSignal, Signal} from '@angular/core';
import { Router } from '@angular/router';
import {UserDto} from '../api/models/user-dto';
import {Api} from '../api/api';
import {getMe} from '../api/functions';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private api = inject(Api);
  private router = inject(Router);

  currentUser: WritableSignal<UserDto|null> = signal<UserDto | null>(null);

  userInitials: Signal<string> = computed(() => {
    const user = this.currentUser();
    if (!user || !user.username) return '';

    const names = user.username.split(' ');
    if (names.length >= 2) {
      return (names[0][0] + names[1][0]).toUpperCase();
    }
    return names[0].slice(0, 2).toUpperCase();
  });

  constructor() {
    this.initializeUser();
  }

  private async initializeUser() {
    const token = localStorage.getItem('token');

    if (token) {
      try {
        const user = await this.api.invoke(getMe);
        this.currentUser.set(user);
      } catch (error) {
        console.warn("Couldn't confirm session, logging out", error);
        this.logout();
      }
    }
  }

  login(user: UserDto, token: string) {
    localStorage.setItem('token', token);
    this.currentUser.set(user);
    this.router.navigate(['/home']);
  }

  logout() {
    localStorage.removeItem('token');
    this.currentUser.set(null);
    this.router.navigate(['/login']);
  }
}
