import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { Api } from './api';
import { addDays, formatRange, hoursText, mondayOf, toIso } from './dates';
import { WeekTotal } from './models';

@Component({
  selector: 'app-admin',
  imports: [FormsModule],
  template: `
    <main class="page">
      <h1>All timesheets</h1>

      <form class="panel filters" (ngSubmit)="load()">
        <div class="field">
          <label for="from">From</label>
          <input id="from" name="from" type="date" required [(ngModel)]="from" />
        </div>
        <div class="field">
          <label for="to">To</label>
          <input id="to" name="to" type="date" required [(ngModel)]="to" />
        </div>
        <button class="btn" type="submit">Show weeks</button>
        <a class="btn btn-quiet export" [href]="csvHref()" download>Export CSV</a>
      </form>
      <p class="muted note">The export includes submitted weeks only. Periods are widened to whole weeks.</p>

      @if (error()) {
        <p class="error" role="alert">{{ error() }}</p>
      }

      <p class="summary">{{ submittedCount() }} of {{ rows().length }} weeks submitted</p>

      <table>
        <thead>
          <tr>
            <th>Name</th>
            <th>Week</th>
            <th>Status</th>
            <th class="num">Hours</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          @for (row of rows(); track row.userId + row.weekStart) {
            <tr>
              <td>{{ row.userName }}</td>
              <td>{{ formatRange(row.weekStart) }}</td>
              <td>
                <span class="status" [class.status-submitted]="row.status === 'SUBMITTED'" [class.status-open]="row.status === 'OPEN'">
                  {{ row.status === 'SUBMITTED' ? 'Submitted' : 'Open' }}
                </span>
              </td>
              <td class="num">{{ hoursText(row.totalMinutes) }}</td>
              <td class="act">
                @if (row.status === 'SUBMITTED') {
                  <button class="btn btn-link" type="button" (click)="reopen(row)">Reopen</button>
                }
              </td>
            </tr>
          } @empty {
            <tr><td colspan="5" class="muted">No weeks in this period.</td></tr>
          }
        </tbody>
      </table>
    </main>
  `,
  styles: [`
    .filters { display: flex; flex-wrap: wrap; align-items: end; gap: 12px; margin: 24px 0 8px; padding: 16px; }
    .export { margin-left: auto; }
    .note { margin: 0 0 16px; font-size: 13px; }
    .summary { margin: 16px 0 8px; font-weight: 600; }
    table { width: 100%; border-collapse: collapse; }
    th { padding: 8px; text-align: left; font-size: 13px; font-weight: 500; color: var(--muted); }
    td { padding: 10px 8px; border-top: 1px solid var(--rule); }
    .num { text-align: right; }
    .act { width: 90px; text-align: right; }
  `],
})
export class Admin implements OnInit {
  private readonly api = inject(Api);

  protected readonly hoursText = hoursText;
  protected readonly formatRange = formatRange;

  protected readonly from = signal(toIso(mondayOf(new Date())));
  protected readonly to = signal(toIso(addDays(mondayOf(new Date()), 6)));
  protected readonly rows = signal<WeekTotal[]>([]);
  protected readonly error = signal('');

  protected readonly submittedCount = computed(() => this.rows().filter((r) => r.status === 'SUBMITTED').length);
  protected readonly csvHref = computed(() => this.api.csvUrl(this.from(), this.to()));

  async ngOnInit() {
    await this.load();
  }

  protected async load() {
    try {
      this.rows.set(await firstValueFrom(this.api.adminWeeks(this.from(), this.to())));
      this.error.set('');
    } catch (e) {
      this.fail(e);
    }
  }

  protected async reopen(row: WeekTotal) {
    if (!confirm(`Reopen ${row.userName}'s week of ${formatRange(row.weekStart)}?`)) {
      return;
    }
    try {
      await firstValueFrom(this.api.reopenWeek(row.userId, row.weekStart));
      await this.load();
    } catch (e) {
      this.fail(e);
    }
  }

  private fail(e: unknown) {
    this.error.set(e instanceof HttpErrorResponse && e.error?.message ? e.error.message : 'Something went wrong. Try again.');
  }
}