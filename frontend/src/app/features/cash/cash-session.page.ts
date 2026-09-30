import { ChangeDetectionStrategy, Component, computed, inject, input, resource } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Api } from '../../api/api';
import { getCashSession } from '../../api/functions';
import { apiErrorMessage } from '../../core/http/api-error';
import { CashSessionViewComponent } from './cash-session-view.component';

/** Detalle de una caja del historial (solo lectura). */
@Component({
  selector: 'gf-cash-session-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, RouterLink, CashSessionViewComponent],
  template: `
    <div class="mx-auto max-w-5xl">
      <a routerLink="/caja/historial" class="text-sm text-neutral-500 hover:text-neutral-900">← Historial</a>
      @if (detail.value(); as d) {
        <h2 class="mt-3 text-2xl font-semibold tracking-tight text-neutral-900">Caja del {{ d.session.openedAt | date: 'dd/MM/yyyy' }}</h2>
        <p class="mt-1 text-sm text-neutral-600">Abierta por {{ d.session.openedByName }} a las {{ d.session.openedAt | date: 'HH:mm' }}</p>
        <gf-cash-session-view class="mt-6 block" [detail]="d" />
      } @else if (detail.error()) {
        <p class="mt-6 text-sm text-red-700">{{ loadError() }}</p>
      } @else {
        <p class="mt-6 text-sm text-neutral-500">Cargando…</p>
      }
    </div>
  `,
})
export class CashSessionPage {
  private readonly api = inject(Api);
  readonly id = input.required<string>();
  protected readonly detail = resource({
    params: () => ({ id: Number(this.id()) }),
    loader: ({ params }) => this.api.invoke(getCashSession, params),
  });
  protected readonly loadError = computed(() => apiErrorMessage(this.detail.error(), 'No se pudo cargar la caja'));
}
