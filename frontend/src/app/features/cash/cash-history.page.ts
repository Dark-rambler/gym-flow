import { ChangeDetectionStrategy, Component, computed, inject, resource, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Api } from '../../api/api';
import { listCashSessions } from '../../api/functions';
import { apiErrorMessage } from '../../core/http/api-error';
import { BadgeComponent } from '../../shared/ui/badge/badge.component';
import { PaginationComponent } from '../../shared/ui/pagination/pagination.component';
import { DIFFERENCE_CLASS, DIFFERENCE_TEXT, differenceKind } from './payment-method';

@Component({
  selector: 'gf-cash-history-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CurrencyPipe, DatePipe, RouterLink, BadgeComponent, PaginationComponent],
  template: `
    <div class="mx-auto max-w-5xl">
      <a routerLink="/caja" class="text-sm text-neutral-500 hover:text-neutral-900">← Caja</a>
      <h2 class="mt-3 text-2xl font-semibold tracking-tight text-neutral-900">Historial de cajas</h2>

      <div class="mt-6 overflow-hidden rounded-xl bg-white ring-1 ring-neutral-200">
        @if (sessions.isLoading() && !sessions.hasValue()) {
          <p class="p-6 text-sm text-neutral-500">Cargando…</p>
        } @else if (sessions.error()) {
          <p class="p-6 text-sm text-red-700">{{ loadError() }}</p>
        } @else {
          <ul class="divide-y divide-neutral-100">
            @for (s of sessions.value()?.items; track s.id) {
              <li>
                <a [routerLink]="['/caja/sesiones', s.id]" class="flex flex-col gap-1 px-5 py-3.5 hover:bg-neutral-50 sm:flex-row sm:items-center sm:gap-4">
                  <div class="flex-1">
                    <p class="font-medium text-neutral-900">{{ s.openedAt | date: "EEEE dd/MM 'desde' HH:mm" }}</p>
                    <p class="text-xs text-neutral-500">
                      Abrió {{ s.openedByName }}
                      @if (s.closedByName) {
                        · cerró {{ s.closedByName }} a las {{ s.closedAt | date: 'HH:mm' }}
                      }
                      · {{ s.totals?.count ?? 0 }} pagos
                    </p>
                  </div>
                  <span class="font-semibold tabular-nums text-neutral-900">{{ s.totals?.total ?? 0 | currency }}</span>
                  @if (s.status === 'OPEN') {
                    <gf-badge tone="green">Abierta</gf-badge>
                  } @else {
                    <span class="w-28 text-right text-sm font-medium" [class]="diffClass(s.difference ?? 0)">
                      {{ diffText(s.difference ?? 0) }}{{ (s.difference ?? 0) !== 0 ? ' ' + (abs(s.difference ?? 0) | currency) : '' }}
                    </span>
                  }
                </a>
              </li>
            } @empty {
              <li class="p-6 text-sm text-neutral-500">Aún no hay cajas.</li>
            }
          </ul>
        }
      </div>
      @if (sessions.value(); as r) {
        <gf-pagination class="mt-4 block" [page]="r.page" [totalPages]="r.totalPages" [totalItems]="r.totalItems" (pageChange)="page.set($event)" />
      }
    </div>
  `,
})
export class CashHistoryPage {
  private readonly api = inject(Api);
  protected readonly page = signal(0);
  protected readonly sessions = resource({
    params: () => ({ page: this.page(), size: 20 }),
    loader: ({ params }) => this.api.invoke(listCashSessions, params),
  });
  protected readonly loadError = computed(() => apiErrorMessage(this.sessions.error(), 'No se pudo cargar el historial'));
  protected readonly abs = Math.abs;
  protected diffText(d: number): string {
    return DIFFERENCE_TEXT[differenceKind(d)];
  }
  protected diffClass(d: number): string {
    return DIFFERENCE_CLASS[differenceKind(d)];
  }
}
