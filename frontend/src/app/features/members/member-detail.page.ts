import { ChangeDetectionStrategy, Component, computed, inject, input, resource, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Dialog } from '@angular/cdk/dialog';
import { Api } from '../../api/api';
import { cancelMembership, freezeMembership, getMember, setMemberActive, unfreezeMembership } from '../../api/functions';
import { MemberDetailResponse, MembershipResponse } from '../../api/models';
import { AuthStore } from '../../core/auth/auth.store';
import { apiErrorMessage } from '../../core/http/api-error';
import { BadgeComponent } from '../../shared/ui/badge/badge.component';
import { ButtonComponent } from '../../shared/ui/button/button.component';
import { ConfirmService } from '../../shared/ui/confirm/confirm.service';
import { ToastService } from '../../shared/ui/toast/toast.service';
import { AssignMembershipData, AssignMembershipDialog } from './assign-membership.dialog';
import { MemberFormDialog } from './member-form.dialog';
import { STATUS_LABEL, STATUS_TONE, remainingLabel } from './membership-status';

type MembershipAction = 'freeze' | 'unfreeze' | 'cancel';

@Component({
  selector: 'gf-member-detail-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, CurrencyPipe, DatePipe, BadgeComponent, ButtonComponent],
  template: `
    <div class="mx-auto max-w-5xl">
      <a routerLink="/socios" class="text-sm text-neutral-500 hover:text-neutral-900">← Socios</a>

      @if (member.isLoading() && !member.hasValue()) {
        <p class="mt-6 text-sm text-neutral-500">Cargando…</p>
      } @else if (member.error()) {
        <p class="mt-6 text-sm text-red-700">{{ loadError() }}</p>
      } @else if (member.value(); as m) {
        <!-- cabecera -->
        <div class="mt-3 flex flex-wrap items-start justify-between gap-4">
          <div>
            <h2 class="text-2xl font-semibold tracking-tight text-neutral-900">
              {{ m.fullName }}
              @if (!m.active) {
                <gf-badge class="ml-2 align-middle" tone="neutral">Desactivado</gf-badge>
              }
            </h2>
            <p class="mt-1 text-sm text-neutral-600">DNI {{ m.dni }} · socio desde {{ m.createdAt | date: 'MMMM yyyy' }}</p>
          </div>
          <div class="flex gap-2">
            <button gfButton type="button" variant="secondary" (click)="edit(m)">Editar</button>
            @if (isManager()) {
              <button gfButton type="button" variant="ghost" [loading]="busy() === 'active'" (click)="toggleActive(m)">
                {{ m.active ? 'Desactivar' : 'Reactivar' }}
              </button>
            }
          </div>
        </div>

        <div class="mt-6 grid gap-6 lg:grid-cols-3">
          <!-- membresía actual -->
          <section class="rounded-xl bg-white p-5 ring-1 ring-neutral-200 lg:col-span-2" aria-labelledby="current-title">
            <div class="flex items-start justify-between gap-3">
              <h3 id="current-title" class="text-sm font-semibold uppercase tracking-wide text-neutral-500">Membresía actual</h3>
              @if (m.active) {
                <button gfButton type="button" [disabled]="hasFrozen(m)" (click)="assign(m)">
                  {{ m.currentMembership && m.currentMembership.status !== 'EXPIRED' ? 'Renovar' : 'Asignar membresía' }}
                </button>
              }
            </div>
            @if (m.currentMembership; as c) {
              <div class="mt-3">
                <p class="text-xl font-semibold text-neutral-900">{{ c.planName }}
                  <gf-badge class="ml-1 align-middle" [tone]="statusTone[c.status]">{{ statusLabel[c.status] }}</gf-badge></p>
                <p class="mt-1 text-sm text-neutral-600">
                  {{ c.startDate | date: 'dd/MM/yyyy' }} al {{ c.endDate | date: 'dd/MM/yyyy' }} · {{ remainingLabel(c) }}
                </p>
                @if (c.frozenDays > 0) {
                  <p class="mt-1 text-xs text-neutral-500">Incluye {{ c.frozenDays }} días de congelamiento.</p>
                }
                @if (hasFrozen(m)) {
                  <p class="mt-2 text-xs text-amber-700">Descongela la membresía para poder renovar.</p>
                }
              </div>
            } @else {
              <p class="mt-3 text-sm text-neutral-500">Este socio aún no tiene membresías.</p>
            }
          </section>

          <!-- datos -->
          <section class="rounded-xl bg-white p-5 ring-1 ring-neutral-200" aria-labelledby="data-title">
            <h3 id="data-title" class="text-sm font-semibold uppercase tracking-wide text-neutral-500">Datos</h3>
            <dl class="mt-3 space-y-2 text-sm">
              <div><dt class="text-neutral-500">Teléfono</dt><dd class="text-neutral-900">{{ m.phone || '—' }}</dd></div>
              <div><dt class="text-neutral-500">Email</dt><dd class="break-all text-neutral-900">{{ m.email || '—' }}</dd></div>
              <div><dt class="text-neutral-500">Nacimiento</dt><dd class="text-neutral-900">{{ m.birthDate ? (m.birthDate | date: 'dd/MM/yyyy') : '—' }}</dd></div>
              @if (m.notes) {
                <div><dt class="text-neutral-500">Notas</dt><dd class="whitespace-pre-line text-neutral-900">{{ m.notes }}</dd></div>
              }
            </dl>
          </section>
        </div>

        <!-- historial -->
        <section class="mt-6 overflow-hidden rounded-xl bg-white ring-1 ring-neutral-200" aria-labelledby="history-title">
          <h3 id="history-title" class="px-5 pt-5 text-sm font-semibold uppercase tracking-wide text-neutral-500">Historial</h3>
          <ul class="mt-3 divide-y divide-neutral-100">
            @for (ms of m.memberships; track ms.id) {
              <li class="flex flex-col gap-2 px-5 py-3 sm:flex-row sm:items-center">
                <div class="flex-1">
                  <p class="text-sm font-medium text-neutral-900">{{ ms.planName }} · {{ ms.price | currency }}</p>
                  <p class="text-xs text-neutral-500">{{ ms.startDate | date: 'dd/MM/yyyy' }} al {{ ms.endDate | date: 'dd/MM/yyyy' }}</p>
                </div>
                <gf-badge [tone]="statusTone[ms.status]">{{ statusLabel[ms.status] }}</gf-badge>
                @if (isManager()) {
                  <div class="flex gap-1">
                    @for (action of actionsFor(ms); track action) {
                      <button type="button" class="rounded-md px-2 py-1 text-xs font-medium text-neutral-600 hover:bg-neutral-100 hover:text-neutral-900 disabled:opacity-50"
                              [disabled]="busy() === ms.id" (click)="run(action, ms)">{{ actionLabel[action] }}</button>
                    }
                  </div>
                }
              </li>
            } @empty {
              <li class="px-5 pb-5 text-sm text-neutral-500">Sin movimientos.</li>
            }
          </ul>
        </section>
      }
    </div>
  `,
})
export class MemberDetailPage {
  private readonly api = inject(Api);
  private readonly store = inject(AuthStore);
  private readonly dialog = inject(Dialog);
  private readonly confirm = inject(ConfirmService);
  private readonly toast = inject(ToastService);

  /** :id de la ruta (withComponentInputBinding) */
  readonly id = input.required<string>();

  protected readonly member = resource({
    params: () => ({ id: Number(this.id()) }),
    loader: ({ params }) => this.api.invoke(getMember, params),
  });
  protected readonly isManager = computed(() => this.store.hasRole('OWNER', 'ADMIN'));
  protected readonly busy = signal<number | 'active' | null>(null);
  protected readonly loadError = computed(() => apiErrorMessage(this.member.error(), 'No se pudo cargar el socio'));

  protected readonly statusLabel = STATUS_LABEL;
  protected readonly statusTone = STATUS_TONE;
  protected readonly remainingLabel = remainingLabel;
  protected readonly actionLabel: Record<MembershipAction, string> = {
    freeze: 'Congelar',
    unfreeze: 'Descongelar',
    cancel: 'Cancelar',
  };

  protected hasFrozen(m: MemberDetailResponse): boolean {
    return m.memberships.some((ms) => ms.status === 'FROZEN');
  }

  protected actionsFor(ms: MembershipResponse): MembershipAction[] {
    switch (ms.status) {
      case 'ACTIVE':
        return ['freeze', 'cancel'];
      case 'FROZEN':
        return ['unfreeze', 'cancel'];
      case 'SCHEDULED':
        return ['cancel'];
      default:
        return [];
    }
  }

  protected edit(m: MemberDetailResponse): void {
    this.dialog
      .open<MemberDetailResponse>(MemberFormDialog, { data: { member: m }, ariaLabel: 'Editar socio' })
      .closed.subscribe((saved) => {
        if (saved) {
          this.member.set(saved);
          this.toast.success('Datos actualizados');
        }
      });
  }

  protected assign(m: MemberDetailResponse): void {
    const data: AssignMembershipData = {
      memberId: m.id,
      memberName: m.fullName,
      memberships: m.memberships,
      canSetPrice: this.isManager(),
    };
    this.dialog
      .open<MembershipResponse>(AssignMembershipDialog, { data, ariaLabel: 'Asignar membresía' })
      .closed.subscribe((created) => {
        if (created) {
          this.toast.success(`Membresía ${created.planName} registrada`);
          this.member.reload();
        }
      });
  }

  protected async toggleActive(m: MemberDetailResponse): Promise<void> {
    if (m.active) {
      const ok = await this.confirm.ask({
        title: '¿Desactivar socio?',
        message: 'No podrá recibir nuevas membresías. Su historial se conserva y puedes reactivarlo cuando quieras.',
        confirmLabel: 'Desactivar',
        danger: true,
      });
      if (!ok) return;
    }
    this.busy.set('active');
    try {
      this.member.set(await this.api.invoke(setMemberActive, { id: m.id, body: { active: !m.active } }));
      this.toast.success(m.active ? 'Socio desactivado' : 'Socio reactivado');
    } catch (err) {
      this.toast.error(apiErrorMessage(err));
    } finally {
      this.busy.set(null);
    }
  }

  protected async run(action: MembershipAction, ms: MembershipResponse): Promise<void> {
    if (action === 'cancel') {
      const ok = await this.confirm.ask({
        title: '¿Cancelar membresía?',
        message: `La membresía ${ms.planName} dejará de ser válida. Esta acción no se puede deshacer.`,
        confirmLabel: 'Cancelar membresía',
        danger: true,
      });
      if (!ok) return;
    }
    const fn = { freeze: freezeMembership, unfreeze: unfreezeMembership, cancel: cancelMembership }[action];
    this.busy.set(ms.id);
    try {
      const updated = await this.api.invoke(fn, { id: ms.id });
      this.toast.success(
        action === 'unfreeze'
          ? `Membresía descongelada: ahora vence el ${updated.endDate.split('-').reverse().join('/')}`
          : action === 'freeze'
            ? 'Membresía congelada'
            : 'Membresía cancelada',
      );
      this.member.reload();
    } catch (err) {
      this.toast.error(apiErrorMessage(err));
    } finally {
      this.busy.set(null);
    }
  }
}
