import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Auth } from './auth';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    @if (auth.user(); as user) {
      <header class="bar">
        <a class="brand" routerLink="/week">Timesheet</a>
        <nav>
          <a routerLink="/week" routerLinkActive="active">My week</a>
          @if (auth.isAdmin()) {
            <a routerLink="/admin" routerLinkActive="active">Admin</a>
          }
        </nav>
        <span class="who">{{ user.name }}</span>
        <button class="btn btn-link" type="button" (click)="signOut()">Sign out</button>
      </header>
    }
    <router-outlet />
  `,
  styles: [`
    .bar {
      display: flex;
      align-items: center;
      gap: 24px;
      padding: 0 24px;
      border-bottom: 1px solid var(--rule);
      background: var(--surface);
    }
    .brand { font: 700 20px var(--font-display); text-decoration: none; }
    nav { display: flex; gap: 20px; margin-right: auto; }
    nav a { padding: 16px 0 13px; border-bottom: 3px solid transparent; color: var(--muted); text-decoration: none; font-weight: 500; }
    nav a.active { color: var(--ink); border-bottom-color: var(--highlight); }
    .who { color: var(--muted); }
  `],
})
export class App {
  protected readonly auth = inject(Auth);
  private readonly router = inject(Router);

  protected async signOut() {
    await this.auth.logout();
    await this.router.navigateByUrl('/login');
  }
}