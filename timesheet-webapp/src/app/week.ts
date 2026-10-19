import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { Api } from './api';
import { addDays, formatDay, formatRange, fromIso, hoursText, mondayOf, toIso, todayIso } from './dates';
import { Entry, Task, Week as WeekData } from './models';

@Component({
  selector: 'app-week',
  imports: [FormsModule],
  template: `
    <main class="page">
      <header class="head">
        <div class="nav">
          <button class="btn btn-quiet" type="button" (click)="shift(-1)" aria-label="Previous week">‹</button>
          <h1>{{ title() }}</h1>
          <button class="btn btn-quiet" type="button" (click)="shift(1)" aria-label="Next week">›</button>
          <button class="btn btn-link" type="button" (click)="goToday()">This week</button>
        </div>
        <div class="summary">
          <span class="total">{{ total() }}<small>h</small></span>
          <span class="status" [class.status-submitted]="submitted()" [class.status-open]="!submitted()">
            {{ submitted() ? 'Submitted' : 'Open' }}
          </span>
          @if (!submitted()) {
            <button class="btn" type="button" (click)="submitWeek()">Submit week</button>
          }
        </div>
      </header>

      @if (submitted()) {
        <p class="notice">This week is submitted. Ask an admin to reopen it if something needs to change.</p>
      } @else {
        <form class="panel logger" (ngSubmit)="save()">
          <div class="field">
            <label for="date">Day</label>
            <input id="date" name="date" type="date" required [min]="weekStart()" [max]="weekEnd()" [(ngModel)]="date" />
          </div>
          <div class="field grow">
            <label for="task">Project and task</label>
            <select id="task" name="task" required [(ngModel)]="taskId">
              @for (group of groups(); track group.label) {
                <optgroup [label]="group.label">
                  @for (task of group.tasks; track task.id) {
                    <option [ngValue]="task.id">{{ task.name }}</option>
                  }
                </optgroup>
              }
            </select>
          </div>
          <div class="field hours">
            <label for="hours">Hours</label>
            <input id="hours" name="hours" type="number" step="0.25" min="0.25" max="24" required [(ngModel)]="hours" />
          </div>
          <div class="field grow">
            <label for="note">Note</label>
            <input id="note" name="note" type="text" maxlength="500" [(ngModel)]="note" />
          </div>
          <div class="buttons">
            <button class="btn" type="submit">{{ editingId() === null ? 'Add entry' : 'Save changes' }}</button>
            @if (editingId() !== null) {
              <button class="btn btn-link" type="button" (click)="clearForm()">Cancel</button>
            }
          </div>
        </form>
      }

      @if (error()) {
        <p class="error" role="alert">{{ error() }}</p>
      }

      @for (day of days(); track day.iso) {
        <section class="day" [class.today]="day.iso === today">
          <div class="day-head">
            <h2>{{ day.weekday }} {{ day.day }}</h2>
            <div class="ruler" [style.--hours]="day.ruler"></div>
            <span class="day-hours">{{ day.hours }} h</span>
          </div>
          @for (entry of day.entries; track entry.id) {
            <div class="entry">
              <span><strong>{{ entry.projectName }}</strong> {{ entry.taskName }}</span>
              <span class="muted">{{ entry.note }}</span>
              <span class="hrs">{{ hoursText(entry.minutes) }} h</span>
              <span class="actions">
                @if (!submitted()) {
                  <button class="btn btn-link" type="button" (click)="edit(entry)">Edit</button>
                  <button class="btn btn-link" type="button" (click)="remove(entry)">Delete</button>
                }
              </span>
            </div>
          }
        </section>
      }
    </main>
  `,
  styles: [`
    .head { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 16px; }
    .nav { display: flex; align-items: center; gap: 10px; }
    .summary { display: flex; align-items: center; gap: 16px; }
    .total { font: 700 56px/1 var(--font-display); letter-spacing: -0.02em; }
    .total small { margin-left: 4px; font-size: 22px; color: var(--muted); }

    .notice { margin: 24px 0; padding: 12px 16px; border-left: 4px solid var(--go); background: var(--surface); }
    .logger { display: flex; flex-wrap: wrap; align-items: end; gap: 12px; margin: 24px 0 8px; padding: 16px; }
    .logger .grow { flex: 1 1 180px; }
    .logger .hours { width: 90px; }
    .buttons { display: flex; align-items: center; gap: 8px; }
    .error { margin: 8px 0; }

    .day { margin-top: 8px; padding: 14px 0 6px 12px; border-top: 1px solid var(--rule); border-left: 4px solid transparent; }
    .day.today { border-left-color: var(--highlight); }
    .day-head { display: grid; grid-template-columns: 88px 1fr 72px; align-items: center; gap: 16px; }
    .day-head h2 { font-size: 20px; }
    .day-hours { text-align: right; font-weight: 600; }
    .entry { display: grid; grid-template-columns: 1.2fr 1.4fr 64px auto; align-items: center; gap: 12px; padding: 8px 0; }
    .hrs { text-align: right; }
    .actions { min-width: 96px; text-align: right; }

    @media (max-width: 640px) {
      .entry { grid-template-columns: 1fr auto; }
      .entry .muted { grid-column: 1 / -1; order: 3; }
      .total { font-size: 44px; }
    }
  `],
})
export class Week implements OnInit {
  private readonly api = inject(Api);

  protected readonly today = todayIso();
  protected readonly hoursText = hoursText;

  protected readonly weekStart = signal(toIso(mondayOf(new Date())));
  protected readonly data = signal<WeekData | null>(null);
  protected readonly tasks = signal<Task[]>([]);
  protected readonly error = signal('');

  // The single form, used for adding and editing.
  protected readonly editingId = signal<number | null>(null);
  protected readonly date = signal(todayIso());
  protected readonly taskId = signal<number | null>(null);
  protected readonly hours = signal<number | null>(null);
  protected readonly note = signal('');

  protected readonly submitted = computed(() => this.data()?.status === 'SUBMITTED');
  protected readonly weekEnd = computed(() => toIso(addDays(fromIso(this.weekStart()), 6)));
  protected readonly title = computed(() => formatRange(this.weekStart()));
  protected readonly total = computed(() => hoursText(this.data()?.totalMinutes ?? 0));

  protected readonly groups = computed(() => {
    const byProject = new Map<string, { label: string; tasks: Task[] }>();
    for (const task of this.tasks()) {
      const group = byProject.get(task.projectCode)
        ?? { label: `${task.projectName} (${task.clientName})`, tasks: [] };
      group.tasks.push(task);
      byProject.set(task.projectCode, group);
    }
    return [...byProject.values()];
  });

  protected readonly days = computed(() => {
    const entries = this.data()?.entries ?? [];
    return Array.from({ length: 7 }, (_, i) => {
      const iso = toIso(addDays(fromIso(this.weekStart()), i));
      const list = entries.filter((e) => e.workDate === iso);
      const minutes = list.reduce((sum, e) => sum + e.minutes, 0);
      return { iso, ...formatDay(iso), entries: list, hours: hoursText(minutes), ruler: minutes / 60 };
    });
  });

  async ngOnInit() {
    try {
      this.tasks.set(await firstValueFrom(this.api.tasks()));
    } catch (e) {
      this.fail(e);
    }
    await this.load();
  }

  protected async shift(weeks: number) {
    this.weekStart.set(toIso(addDays(fromIso(this.weekStart()), weeks * 7)));
    await this.load();
  }

  protected async goToday() {
    this.weekStart.set(toIso(mondayOf(new Date())));
    await this.load();
  }

  protected async save() {
    const taskId = this.taskId();
    const hours = this.hours();
    if (taskId === null || hours === null) {
      this.error.set('Choose a task and enter the hours.');
      return;
    }
    const input = {
      taskId,
      workDate: this.date(),
      minutes: Math.round(hours * 60),
      note: this.note().trim() || null,
    };
    try {
      const id = this.editingId();
      if (id === null) {
        await firstValueFrom(this.api.createEntry(input));
      } else {
        await firstValueFrom(this.api.updateEntry(id, input));
      }
      await this.refresh();
      // Keep the day and task so several entries can be typed in a row.
      this.editingId.set(null);
      this.hours.set(null);
      this.note.set('');
    } catch (e) {
      this.fail(e);
    }
  }

  protected edit(entry: Entry) {
    this.editingId.set(entry.id);
    this.date.set(entry.workDate);
    this.taskId.set(entry.taskId);
    this.hours.set(entry.minutes / 60);
    this.note.set(entry.note ?? '');
    window.scrollTo({ top: 0 });
  }

  protected async remove(entry: Entry) {
    if (!confirm(`Delete ${hoursText(entry.minutes)} h on ${entry.projectName}?`)) {
      return;
    }
    try {
      await firstValueFrom(this.api.deleteEntry(entry.id));
      await this.refresh();
    } catch (e) {
      this.fail(e);
    }
  }

  protected async submitWeek() {
    if (!confirm('Submit this week? You will not be able to edit it afterwards.')) {
      return;
    }
    try {
      this.data.set(await firstValueFrom(this.api.submitWeek(this.weekStart())));
      this.error.set('');
    } catch (e) {
      this.fail(e);
    }
  }

  protected clearForm() {
    this.editingId.set(null);
    this.date.set(this.defaultDate());
    this.hours.set(null);
    this.note.set('');
    if (this.taskId() === null) {
      this.taskId.set(this.tasks()[0]?.id ?? null);
    }
  }

  private async load() {
    await this.refresh();
    this.clearForm();
  }

  private async refresh() {
    try {
      this.data.set(await firstValueFrom(this.api.week(this.weekStart())));
      this.error.set('');
    } catch (e) {
      this.fail(e);
    }
  }

  /** Today if it falls in the shown week, otherwise the week's Monday. */
  private defaultDate(): string {
    return this.today >= this.weekStart() && this.today <= this.weekEnd() ? this.today : this.weekStart();
  }

  private fail(e: unknown) {
    this.error.set(e instanceof HttpErrorResponse && e.error?.message ? e.error.message : 'Something went wrong. Try again.');
  }
}