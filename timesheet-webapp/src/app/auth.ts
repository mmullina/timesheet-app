import { Injectable, computed, inject, signal } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { Api } from './api';
import { User } from './models';

@Injectable({ providedIn: 'root' })
export class Auth {
  private readonly api = inject(Api);

  /** undefined until the server has been asked, null when signed out. */
  readonly user = signal<User | null | undefined>(undefined);
  readonly isAdmin = computed(() => this.user()?.role === 'ADMIN');

  async load(): Promise<void> {
    try {
      this.user.set(await firstValueFrom(this.api.me()));
    } catch {
      this.user.set(null);
    }
  }

  async login(email: string, password: string): Promise<boolean> {
    try {
      await firstValueFrom(this.api.login(email, password));
    } catch {
      return false;
    }
    await this.load();
    return this.user() !== null;
  }

  async logout(): Promise<void> {
    try {
      await firstValueFrom(this.api.logout());
    } finally {
      // Reloading also fetches a fresh CSRF cookie, which the next login needs.
      await this.load();
    }
  }
}

export const signedIn: CanActivateFn = () =>
  inject(Auth).user() ? true : inject(Router).createUrlTree(['/login']);

export const adminOnly: CanActivateFn = () =>
  inject(Auth).isAdmin() ? true : inject(Router).createUrlTree(['/week']);