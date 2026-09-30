import { ChangeDetectionStrategy, Component, ElementRef, OnDestroy, computed, inject, resource, signal, viewChild } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Dialog } from '@angular/cdk/dialog';
import { Api } from '../../api/api';
import { checkIn, listCheckIns } from '../../api/functions';
import { CheckInResponse } from '../../api/models';
import { apiErrorMessage } from '../../core/http/api-error';
import { BadgeComponent } from '../../shared/ui/badge/badge.component';
import { ButtonComponent } from '../../shared/ui/button/button.component';
import { IconComponent } from '../../shared/ui/icon/icon.component';
import { CameraScannerDialog } from './camera-scanner.dialog';
import { CheckInOutcome, REASON_LABEL, minutesBetween, outcomeOf } from './check-in-code';

const RESULT_VISIBLE_MS = 6000;

const PANEL: Record<CheckInOutcome, string> = {
  allowed: 'bg-emerald-600 text-white',
  duplicate: 'bg-amber-400 text-amber-950',
  denied: 'bg-red-600 text-white',
};

@Component({
  selector: 'gf-checkin-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, RouterLink, BadgeComponent, ButtonComponent, IconComponent],
  host: { '(document:click)': 'refocus()' },
  template: `
    <div class="mx-auto max-w-3xl">
      <h2 class="text-2xl font-semibold tracking-tight text-neutral-900">Check-in</h2>
      <p class="mt-1 text-sm text-neutral-600">Escanea el QR del socio o escribe su DNI y presiona Enter.</p>

      <form class="mt-6 flex gap-2" (submit)="$event.preventDefault(); submit()">
        <input #codeInput type="text" class="gf-input !py-4 text-center text-xl tracking-wider" autocomplete="off"
               placeholder="QR o DNI" aria-label="QR o DNI" [value]="code()" (input)="code.set($any($event.target).value)"
               [disabled]="busy()" autofocus />
        <button gfButton type="button" variant="secondary" (click)="openCamera(); $event.stopPropagation()" aria-label="Usar cámara">
          <gf-icon name="qr" /> <span class="hidden sm:inline">Cámara</span>
        </button>
      </form>

      <div class="mt-4 min-h-40" aria-live="assertive">
        @if (result(); as r) {
          <div class="rounded-2xl p-6 text-center shadow-sm" [class]="panelClass()">
            <p class="text-3xl font-bold">
              @switch (outcome()) {
                @case ('allowed') { ✓ Puede pasar }
                @case ('duplicate') { Ya registrado }
                @default { ✕ No puede pasar }
              }
            </p>
            @if (r.member; as m) {
              <p class="mt-2 text-xl font-semibold">{{ m.fullName }}</p>
            }
            <p class="mt-1 text-base opacity-90">
              @switch (outcome()) {
                @case ('duplicate') { Entró hace {{ minutesAgo() }} min ({{ r.checkedAt | date: 'HH:mm' }}) }
                @default { {{ r.message }} }
              }
            </p>
            @if (r.membership; as ms) {
              <p class="mt-2 text-sm opacity-90">
                {{ ms.planName }} · vence el {{ ms.endDate | date: 'dd/MM/yyyy' }}
                @if (ms.status === 'ACTIVE') {
                  · {{ ms.daysLeft }} {{ ms.daysLeft === 1 ? 'día' : 'días' }}
                }
              </p>
            }
            @if (r.member && outcome() === 'denied') {
              <a [routerLink]="['/socios', r.member.id]" class="mt-3 inline-block text-sm font-semibold underline">Ver ficha / renovar</a>
            }
          </div>
        } @else if (error()) {
          <p class="rounded-xl bg-red-50 p-4 text-center text-sm text-red-700">{{ error() }}</p>
        }
      </div>

      <section class="mt-6 overflow-hidden rounded-xl bg-white ring-1 ring-neutral-200" aria-labelledby="today-title">
        <h3 id="today-title" class="px-5 pt-5 text-sm font-semibold uppercase tracking-wide text-neutral-500">
          Hoy · {{ allowedToday() }} {{ allowedToday() === 1 ? 'entrada' : 'entradas' }}
        </h3>
        <ul class="mt-3 divide-y divide-neutral-100">
          @for (e of entries.value()?.items; track e.id) {
            <li class="flex items-center gap-4 px-5 py-2.5 text-sm">
              <span class="w-12 tabular-nums text-neutral-500">{{ e.checkedAt | date: 'HH:mm' }}</span>
              @if (e.memberId) {
                <a [routerLink]="['/socios', e.memberId]" class="flex-1 truncate font-medium text-neutral-900 hover:underline">{{ e.memberName }}</a>
              } @else {
                <span class="flex-1 italic text-neutral-400">Código desconocido</span>
              }
              <span class="text-xs text-neutral-400">{{ e.method }}</span>
              @if (e.result === 'ALLOWED') {
                <gf-badge tone="green">Entró</gf-badge>
              } @else {
                <gf-badge tone="red">{{ reasonLabel[e.reason!] }}</gf-badge>
              }
            </li>
          } @empty {
            <li class="px-5 pb-5 text-sm text-neutral-500">Aún no hay entradas hoy.</li>
          }
        </ul>
      </section>
    </div>
  `,
})
export class CheckinPage implements OnDestroy {
  private readonly api = inject(Api);
  private readonly dialog = inject(Dialog);
  private readonly input = viewChild.required<ElementRef<HTMLInputElement>>('codeInput');

  protected readonly code = signal('');
  protected readonly busy = signal(false);
  protected readonly result = signal<CheckInResponse | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly entries = resource({ loader: () => this.api.invoke(listCheckIns, { size: 100 }) });
  protected readonly reasonLabel = REASON_LABEL;

  protected readonly outcome = computed(() => {
    const r = this.result();
    return r ? outcomeOf(r) : null;
  });
  protected readonly panelClass = computed(() => PANEL[this.outcome() ?? 'denied']);
  protected readonly minutesAgo = computed(() => {
    const r = this.result();
    return r ? minutesBetween(r.checkedAt) : 0;
  });
  protected readonly allowedToday = computed(
    () => this.entries.value()?.items.filter((e) => e.result === 'ALLOWED').length ?? 0,
  );

  private clearTimer: ReturnType<typeof setTimeout> | null = null;
  private cameraOpen = false;

  async submit(raw: string = this.code()): Promise<void> {
    const code = raw.trim();
    if (!code || this.busy()) return;
    this.busy.set(true);
    this.error.set(null);
    try {
      const r = await this.api.invoke(checkIn, { body: { code } });
      this.show(r);
      beep(outcomeOf(r));
      this.entries.reload();
    } catch (err) {
      this.result.set(null);
      this.error.set(apiErrorMessage(err));
      beep('denied');
    } finally {
      this.code.set('');
      this.busy.set(false);
      this.refocus();
    }
  }

  protected openCamera(): void {
    this.cameraOpen = true;
    this.dialog
      .open<string>(CameraScannerDialog, { ariaLabel: 'Escanear QR con la cámara' })
      .closed.subscribe((text) => {
        this.cameraOpen = false;
        if (text) void this.submit(text);
        else this.refocus();
      });
  }

  /** El campo siempre debe tener el foco: el lector USB "escribe" donde esté el cursor. */
  protected refocus(): void {
    if (this.cameraOpen) return;
    const selection = window.getSelection?.()?.toString();
    if (selection) return; // no robar el foco si el usuario está seleccionando texto
    setTimeout(() => this.input().nativeElement.focus(), 0);
  }

  private show(r: CheckInResponse): void {
    this.result.set(r);
    if (this.clearTimer) clearTimeout(this.clearTimer);
    this.clearTimer = setTimeout(() => this.result.set(null), RESULT_VISIBLE_MS);
  }

  ngOnDestroy(): void {
    if (this.clearTimer) clearTimeout(this.clearTimer);
  }
}

let audio: AudioContext | null = null;

/** Tono corto: agudo si pasa, medio si ya estaba, grave si no puede pasar. */
function beep(outcome: CheckInOutcome): void {
  try {
    audio ??= new AudioContext();
    const osc = audio.createOscillator();
    const gain = audio.createGain();
    osc.frequency.value = outcome === 'allowed' ? 880 : outcome === 'duplicate' ? 600 : 220;
    gain.gain.value = 0.15;
    osc.connect(gain).connect(audio.destination);
    osc.start();
    osc.stop(audio.currentTime + (outcome === 'denied' ? 0.35 : 0.12));
  } catch {
    // sin audio (política del navegador): no pasa nada
  }
}
