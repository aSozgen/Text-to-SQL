import { Injectable, signal, computed, inject, WritableSignal, Signal } from '@angular/core';
import { Router } from '@angular/router';
import { UserDto } from '../api/models/user-dto';
import { AuthenticationResponse } from '../api/models/authentication-response';
import { Api } from '../api/api';
import { getMe, getGuestToken, refreshToken, logout } from '../api/functions';
import { firstValueFrom, lastValueFrom } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private api = inject(Api);
  private router = inject(Router);

  currentUser: WritableSignal<UserDto | null> = signal<UserDto | null>(null);

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
      await this.refreshUser();
    }
  }

  async refreshUser() {
    try {
      const user = await this.api.invoke(getMe);
      // Only set currentUser if it's a real user (not GUEST)
      if (user.role === 'USER' || user.role === 'ADMIN') {
        this.currentUser.set(user);
      } else {
        this.currentUser.set(null);
      }
    } catch (error) {
      this.clearSession();
    }
  }

  login(user: UserDto, response: AuthenticationResponse) {
    if (response.token) localStorage.setItem('token', response.token);
    if (response.refreshToken) localStorage.setItem('refreshToken', response.refreshToken);
    this.currentUser.set(user);
    this.router.navigate(['/home']);
  }

  async createGuestSession(): Promise<void> {
    // If we already have a token, we don't need a new guest session
    if (localStorage.getItem('token')) return;

    try {
      const response = await this.api.invoke(getGuestToken);
      if (response.token) localStorage.setItem('token', response.token);
      if (response.refreshToken) localStorage.setItem('refreshToken', response.refreshToken);
      // We explicitly don't set currentUser for guests
    } catch (e) {
      console.error('Failed to create guest session', e);
    }
  }

  async doRefreshToken(): Promise<string | null> {
    const rToken = localStorage.getItem('refreshToken');
    if (!rToken) return null;

    try {
      const response = await this.api.invoke(refreshToken, { body: { refreshToken: rToken } });
      if (response.token) localStorage.setItem('token', response.token);
      if (response.refreshToken) localStorage.setItem('refreshToken', response.refreshToken);
      return response.token || null;
    } catch (e) {
      console.error('Failed to refresh token', e);
      this.logout();
      return null;
    }
  }

  async logout() {
    const rToken = localStorage.getItem('refreshToken');
    if (rToken) {
      try {
        await this.api.invoke(logout, { body: { refreshToken: rToken } });
      } catch (e) {
        console.error('Logout API call failed', e);
      }
    }
    this.clearSession();
    this.router.navigate(['/login']);
  }

  private clearSession() {
    localStorage.removeItem('token');
    localStorage.removeItem('refreshToken');
    this.currentUser.set(null);
  }
}
