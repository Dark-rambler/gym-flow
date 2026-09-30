import { ChangeDetectionStrategy, Component, computed, inject, input, resource } from '@angular/core';
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Api } from '../../api/api';
import { getPublicMemberCard } from '../../api/functions';
import { BadgeComponent } from '../../shared/ui/badge/badge.component';
import { QrCodeComponent } from '../../shared/ui/qr-code/qr-code.component';
import { STATUS_LABEL, STATUS_TONE } from './membership-status';

/** Carnet del socio para su celular. Pública (sin login): el token del enlace es la credencial. */
@Component({
  selector: 'gf-public-card-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, BadgeComponent, QrCodeComponent],
  host: { class: 'flex min-h-screen flex-col items-center justify-center bg-neutral-900 px-6 py-10 text-center' },
  template: `
    @if (card.value(); as c) {
      <p class="text-sm font-semibold uppercase tracking-widest text-brand-500">{{ c.gymName }}</p>
      <h1 class="mt-2 text-2xl font-bold text-white">{{ c.memberName }}</h1>
      <div class="mt-6 rounded-3xl bg-white p-5">
        <gf-qr-code [value]="c.payload" [size]="260" alt="Tu QR de acceso" />
      </div>
      @if (c.status) {
        <div class="mt-6 space-y-1 text-neutral-300">
          <gf-badge [tone]="statusTone[c.status]">{{ c.planName }} · {{ statusLabel[c.status] }}</gf-badge>
          <p class="text-sm">Vence el {{ c.endDate | date: 'dd/MM/yyyy' }}</p>
        </div>
      } @else {
        <p class="mt-6 text-sm text-neutral-400">Sin membresía activa</p>
      }
      <p class="mt-8 max-w-xs text-xs text-neutral-500">Muestra este código en recepción. Sube el brillo de la pantalla si no lo leen.</p>
    } @else if (card.error()) {
      <p class="text-lg font-semibold text-white">{{ notFound() ? 'Este enlace ya no es válido' : 'No se pudo cargar tu carnet' }}</p>
      <p class="mt-2 text-sm text-neutral-400">{{ notFound() ? 'Pide un enlace nuevo en recepción.' : 'Revisa tu conexión e inténtalo de nuevo.' }}</p>
    } @else {
      <p class="text-sm text-neutral-400">Cargando…</p>
    }
  `,
})
export class PublicCardPage {
  private readonly api = inject(Api);
  /** :token de la ruta */
  readonly token = input.required<string>();

  protected readonly card = resource({
    params: () => ({ token: this.token() }),
    loader: ({ params }) => this.api.invoke(getPublicMemberCard, params),
  });
  protected readonly notFound = computed(() => {
    const err = this.card.error();
    return err instanceof HttpErrorResponse && (err.status === 404 || err.status === 400);
  });
  protected readonly statusLabel = STATUS_LABEL;
  protected readonly statusTone = STATUS_TONE;
}
