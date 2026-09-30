import { ChangeDetectionStrategy, Component, computed, inject, resource, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { Dialog } from '@angular/cdk/dialog';
import { debounceTime, distinctUntilChanged } from 'rxjs';
import { Api } from '../../api/api';
import { searchMembers } from '../../api/functions';
import { MemberDetailResponse } from '../../api/models';
import { apiErrorMessage } from '../../core/http/api-error';
import { BadgeComponent } from '../../shared/ui/badge/badge.component';
import { ButtonComponent } from '../../shared/ui/button/button.component';
import { IconComponent } from '../../shared/ui/icon/icon.component';
import { PaginationComponent } from '../../shared/ui/pagination/pagination.component';
import { MemberFormDialog } from './member-form.dialog';
import { STATUS_LABEL, STATUS_TONE, expiresSoon, remainingLabel } from './membership-status';

const PAGE_SIZE = 20;

@Component({
  selector: 'gf-members-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, BadgeComponent, ButtonComponent, IconComponent, PaginationComponent],
  template: `
    <div class="mx-auto max-w-5xl">
      <div class="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h2 class="text-2xl font-semibold tracking-tight text-neutral-900">Socios</h2>
          <p class="mt-1 text-sm text-neutral-600">Busca por nombre o DNI.</p>
        </div>
        <button gfButton type="button" (click)="openCreate()"><gf-icon name="plus" [size]="18" /> Nuevo socio</button>
      </div>

      <input type="search" class="gf-input mt-6" placeholder="Buscar socio…" aria-label="Buscar socio"
             [value]="query()" (input)="onSearch($any($event.target).value)" />

      <div class="mt-4 overflow-hidden rounded-xl bg-white ring-1 ring-neutral-200">
        @if (members.isLoading() && !members.hasValue()) {
          <p class="p-6 text-sm text-neutral-500">Cargando…</p>
        } @else if (members.error()) {
          <p class="p-6 text-sm text-red-700">{{ loadError() }}
            <button type="button" class="ml-2 font-semibold underline" (click)="members.reload()">Reintentar</button></p>
        } @else {
          <ul class="divide-y divide-neutral-100" [class.opacity-60]="members.isLoading()">
            @for (member of members.value()?.items; track member.id) {
              <li>
                <a [routerLink]="['/socios', member.id]" class="flex flex-col gap-1 px-4 py-3.5 hover:bg-neutral-50 sm:flex-row sm:items-center sm:gap-4 sm:px-6">
                  <div class="min-w-0 flex-1">
                    <p class="truncate font-medium text-neutral-900">
                      {{ member.fullName }}
                      @if (!member.active) {
                        <gf-badge class="ml-1" tone="neutral">Desactivado</gf-badge>
                      }
                    </p>
                    <p class="text-sm text-neutral-500">DNI {{ member.dni }}{{ member.phone ? ' · ' + member.phone : '' }}</p>
                  </div>
                  @if (member.currentMembership; as ms) {
                    <div class="flex items-center gap-2 sm:flex-col sm:items-end sm:gap-1">
                      <gf-badge [tone]="statusTone[ms.status]">{{ ms.planName }} · {{ statusLabel[ms.status] }}</gf-badge>
                      <span class="text-xs" [class]="expiresSoon(ms) ? 'font-semibold text-amber-700' : 'text-neutral-500'">
                        {{ remainingLabel(ms) }}
                      </span>
                    </div>
                  } @else {
                    <span class="text-xs text-neutral-400">Sin membresía</span>
                  }
                </a>
              </li>
            } @empty {
              <li class="p-8 text-center text-sm text-neutral-500">
                {{ debouncedQuery() ? 'Ningún socio coincide con "' + debouncedQuery() + '".' : 'Aún no hay socios. Registra el primero.' }}
              </li>
            }
          </ul>
        }
      </div>

      @if (members.value(); as result) {
        <gf-pagination class="mt-4 block" [page]="result.page" [totalPages]="result.totalPages"
                       [totalItems]="result.totalItems" (pageChange)="page.set($event)" />
      }
    </div>
  `,
})
export class MembersPage {
  private readonly api = inject(Api);
  private readonly dialog = inject(Dialog);
  private readonly router = inject(Router);

  protected readonly query = signal('');
  protected readonly debouncedQuery = toSignal(
    toObservable(this.query).pipe(debounceTime(300), distinctUntilChanged()),
    { initialValue: '' },
  );
  protected readonly page = signal(0);

  protected readonly members = resource({
    params: () => ({ q: this.debouncedQuery().trim(), page: this.page() }),
    loader: ({ params }) =>
      this.api.invoke(searchMembers, { q: params.q || undefined, page: params.page, size: PAGE_SIZE }),
  });
  protected readonly loadError = computed(() => apiErrorMessage(this.members.error(), 'No se pudieron cargar los socios'));

  protected readonly statusLabel = STATUS_LABEL;
  protected readonly statusTone = STATUS_TONE;
  protected readonly remainingLabel = remainingLabel;
  protected readonly expiresSoon = expiresSoon;

  protected onSearch(value: string): void {
    this.query.set(value);
    this.page.set(0);
  }

  protected openCreate(): void {
    const ref = this.dialog.open<MemberDetailResponse>(MemberFormDialog, { data: {}, ariaLabel: 'Nuevo socio' });
    ref.closed.subscribe((created) => {
      if (created) {
        void this.router.navigate(['/socios', created.id], { state: { justCreated: true } });
      }
    });
  }
}
