import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { CashSessionDetailResponse, PaymentResponse } from '../../api/models';
import { BadgeComponent } from '../../shared/ui/badge/badge.component';
import { DIFFERENCE_CLASS, DIFFERENCE_TEXT, METHOD_LABEL, differenceKind } from './payment-method';

/** Resumen de una caja (tiles + pagos). Lo usan la caja actual y el detalle del historial. */
@Component({
  selector: 'gf-cash-session-view',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CurrencyPipe, DatePipe, RouterLink, BadgeComponent],
  template: `
    @let s = detail().session;
    @if (s.totals && s.expectedCash !== null && s.expectedCash !== undefined) {
      <div class="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-6">
        <div class="col-span-2 rounded-xl bg-neutral-900 p-4 text-white sm:col-span-1 lg:col-span-2">
          <p class="text-xs text-neutral-400">{{ s.status === 'OPEN' ? 'Efectivo esperado en caja' : 'Efectivo esperado' }}</p>
          <p class="mt-1 text-2xl font-semibold tracking-tight">{{ s.expectedCash | currency }}</p>
          <p class="mt-1 text-xs text-neutral-400">Inicial {{ s.openingAmount | currency }} + efectivo {{ s.totals.cash | currency }}</p>
        </div>
        @for (tile of tiles(); track tile.label) {
          <div class="rounded-xl bg-white p-4 ring-1 ring-neutral-200">
            <p class="text-xs text-neutral-500">{{ tile.label }}</p>
            <p class="mt-1 text-lg font-semibold text-neutral-900">{{ tile.value | currency }}</p>
          </div>
        }
      </div>
    } @else {
      <!-- arqueo a ciegas (recepción): el backend no envía esperado ni totales -->
      <div class="rounded-xl bg-white p-4 text-sm text-neutral-600 ring-1 ring-neutral-200">
        Monto inicial: <strong class="text-neutral-900">{{ s.openingAmount | currency }}</strong>.
        Al cerrar, cuenta el efectivo del cajón; el dueño o administrador verá si cuadra.
      </div>
    }

    @if (s.status === 'CLOSED' && s.difference !== null && s.difference !== undefined) {
      <div class="mt-3 rounded-xl bg-white p-4 text-sm ring-1 ring-neutral-200">
        Cerrada {{ s.closedAt | date: 'dd/MM/yyyy HH:mm' }} por {{ s.closedByName }} ·
        contado {{ s.countedCash | currency }} ·
        <strong [class]="diffClass()">{{ diffText() }} {{ (s.difference < 0 ? -s.difference : s.difference) | currency }}</strong>
        @if (s.notes) {
          <p class="mt-1 text-neutral-500">“{{ s.notes }}”</p>
        }
      </div>
    }

    <section class="mt-6 overflow-hidden rounded-xl bg-white ring-1 ring-neutral-200" aria-labelledby="payments-title">
      <h3 id="payments-title" class="px-5 pt-5 text-sm font-semibold uppercase tracking-wide text-neutral-500">
        Pagos ({{ validCount() }})
        @if (s.totals) {
          · total {{ s.totals.total | currency }}
        }
      </h3>
      <ul class="mt-3 divide-y divide-neutral-100">
        @for (p of detail().payments; track p.id) {
          <li class="flex flex-col gap-1 px-5 py-3 sm:flex-row sm:items-center sm:gap-4" [class.opacity-50]="p.voided">
            <span class="w-12 text-sm tabular-nums text-neutral-500">{{ p.paidAt | date: 'HH:mm' }}</span>
            <div class="min-w-0 flex-1">
              <a [routerLink]="['/socios', p.memberId]" class="font-medium text-neutral-900 hover:underline" [class.line-through]="p.voided">{{ p.memberName }}</a>
              <p class="text-xs text-neutral-500">
                {{ p.planName }} · cobró {{ p.receivedByName }}{{ p.reference ? ' · op. ' + p.reference : '' }}
                @if (p.voided) {
                  · <span class="text-red-700">anulado: {{ p.voidReason }}</span>
                }
              </p>
            </div>
            <gf-badge [tone]="p.method === 'CASH' ? 'green' : 'blue'">{{ methodLabel[p.method] }}</gf-badge>
            <span class="w-24 text-right font-semibold tabular-nums text-neutral-900" [class.line-through]="p.voided">{{ p.amount | currency }}</span>
            @if (canVoid() && !p.voided) {
              <button type="button" class="rounded-md px-2 py-1 text-xs font-medium text-red-700 hover:bg-red-50" (click)="voidPayment.emit(p)">Anular</button>
            }
          </li>
        } @empty {
          <li class="px-5 pb-5 text-sm text-neutral-500">Aún no hay pagos en esta caja.</li>
        }
      </ul>
    </section>
  `,
})
export class CashSessionViewComponent {
  readonly detail = input.required<CashSessionDetailResponse>();
  /** Muestra "Anular" (solo caja abierta y OWNER/ADMIN). */
  readonly canVoid = input(false);
  readonly voidPayment = output<PaymentResponse>();

  protected readonly methodLabel = METHOD_LABEL;
  protected readonly validCount = computed(() => this.detail().payments.filter((p) => !p.voided).length);
  protected readonly tiles = computed(() => {
    const t = this.detail().session.totals;
    if (!t) return [];
    return [
      { label: 'Efectivo', value: t.cash },
      { label: 'Yape', value: t.yape },
      { label: 'Plin', value: t.plin },
      { label: 'Tarjeta', value: t.card },
    ];
  });
  private readonly kind = computed(() => differenceKind(this.detail().session.difference ?? 0));
  protected readonly diffText = computed(() => DIFFERENCE_TEXT[this.kind()]);
  protected readonly diffClass = computed(() => DIFFERENCE_CLASS[this.kind()]);
}
