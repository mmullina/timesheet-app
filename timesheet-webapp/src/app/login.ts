import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Auth } from './auth';

@Component({
  selector: 'app-login',
  imports: [FormsModule],
  template: `
    <main class="login">
      <div class="ruler" style="--hours: 8"></div>
      <h1>Sign in</h1>
      <form class="panel" (ngSubmit)="submit()">
        <div class="field">
          <label for="email">Email</label>
          <input id="email" name="email" type="email" autocomplete="username" required [(ngModel)]="email" />
        </div>
        <div class="field">
          <label for="password">Password</label>
          <input id="password" name="password" type="password" autocomplete="current-password" required [(ngModel)]="password" />
        </div>
        @if (failed()) {
          <p class="error" role="alert">That email and password don't match.</p>
        }
        <button class="btn" type="submit" [disabled]="busy()">Sign in</button>
      </form>
      <p class="muted hint">Demo: mitch&#64;example.com / user123, admin&#64;example.com / admin123</p>
    </main>
  `,
  styles: [`
    .login { display: grid; gap: 16px; max-width: 360px; margin: 12vh auto 0; padding: 0 16px; }
    .login .ruler { width: 120px; }
    form { display: grid; gap: 16px; padding: 20px; }
    .error { margin: 0; }
    .hint { margin: 0; font-size: 13px; }
  `],
})
export class Login {
  private readonly auth = inject(Auth);
  private readonly router = inject(Router);

  protected readonly email = signal('');
  protected readonly password = signal('');
  protected readonly failed = signal(false);
  protected readonly busy = signal(false);

  protected async submit() {
    this.busy.set(true);
    this.failed.set(false);
    const ok = await this.auth.login(this.email(), this.password());
    this.busy.set(false);
    if (ok) {
      await this.router.navigateByUrl('/week');
    } else {
      this.failed.set(true);
    }
  }
}