import { ChangeDetectionStrategy, Component, computed, inject, resource, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Dialog } from '@angular/cdk/dialog';
import { Api } from '../../api/api';
import { listStaff, updateStaff } from '../../api/functions';
import { StaffResponse, UpdateStaffRequest } from '../../api/models';
import { AuthStore, Role } from '../../core/auth/auth.store';
import { apiErrorMessage } from '../../core/http/api-error';
import { ButtonComponent } from '../../shared/ui/button/button.component';
import { IconComponent } from '../../shared/ui/icon/icon.component';
import { ToastService } from '../../shared/ui/toast/toast.service';
import { StaffFormData, StaffFormDialog } from './staff-form.dialog';
import { ROLE_LABEL, assignableRoles, canManage } from './staff-roles';

@Component({
  selector: 'gf-staff-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, ButtonComponent, IconComponent],
  template: `
    <div class="mx-auto max-w-5xl">
      <div class="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h2 class="text-2xl font-semibold tracking-tight text-neutral-900">Staff</h2>
          <p class="mt-1 text-sm text-neutral-600">Personas con acceso al panel de tu gimnasio.</p>
        </div>
        <button gfButton type="button" (click)="openCreate()">
          <gf-icon name="plus" [size]="18" /> Nuevo
        </button>
      </div>

      <div class="mt-6 overflow-hidden rounded-xl bg-white ring-1 ring-neutral-200">
        @if (staff.isLoading() && !staff.hasValue()) {
          <p class="p-6 text-sm text-neutral-500">Cargando…</p>
        } @else if (staff.error()) {
          <div class="p-6 text-sm text-red-700">
            {{ loadError() }}
            <button type="button" class="ml-2 font-semibold underline" (click)="staff.reload()">Reintentar</button>
          </div>
        } @else {
          <ul class="divide-y divide-neutral-100">
            @for (member of staff.value(); track member.id) {
              <li class="flex flex-col gap-3 px-4 py-4 sm:flex-row sm:items-center sm:px-6" [class.opacity-60]="!member.active">
                <div class="min-w-0 flex-1">
                  <p class="truncate font-medium text-neutral-900">
                    {{ member.fullName }}
                    @if (member.id === currentUserId()) {
                      <span class="ml-1 text-xs font-normal text-neutral-500">(tú)</span>
                    }
                  </p>
                  <p class="truncate text-sm text-neutral-500">{{ member.email }} · desde {{ member.createdAt | date: 'dd/MM/yyyy' }}</p>
                </div>

                <div class="flex items-center gap-2">
                  @if (editable(member)) {
                    <select class="gf-input !w-auto !py-1.5" [attr.aria-label]="'Rol de ' + member.fullName"
                            [value]="member.role" [disabled]="busyId() === member.id"
                            (change)="changeRole(member, $any($event.target).value)">
                      @for (role of roleOptions(); track role) {
                        <option [value]="role">{{ roleLabel[role] }}</option>
                      }
                    </select>
                    <button gfButton type="button" variant="secondary" [loading]="busyId() === member.id"
                            (click)="toggleActive(member)">
                      {{ member.active ? 'Desactivar' : 'Activar' }}
                    </button>
                  } @else {
                    <span class="rounded-full bg-neutral-100 px-2.5 py-1 text-xs font-medium text-neutral-700">{{ roleLabel[member.role] }}</span>
                  }
                  @if (!member.active) {
                    <span class="rounded-full bg-amber-100 px-2.5 py-1 text-xs font-medium text-amber-800">Inactivo</span>
                  }
                </div>
              </li>
            } @empty {
              <li class="p-6 text-sm text-neutral-500">Aún no hay staff.</li>
            }
          </ul>
        }
      </div>
    </div>
  `,
})
export class StaffPage {
  private readonly api = inject(Api);
  private readonly store = inject(AuthStore);
  private readonly dialog = inject(Dialog);
  private readonly toast = inject(ToastService);

  protected readonly staff = resource({ loader: () => this.api.invoke(listStaff) });
  protected readonly busyId = signal<number | null>(null);
  protected readonly roleLabel = ROLE_LABEL;
  protected readonly currentUserId = computed(() => this.store.user()?.id);
  protected readonly roleOptions = computed(() => assignableRoles(this.store.role()));
  protected readonly loadError = computed(() => apiErrorMessage(this.staff.error(), 'No se pudo cargar el staff'));

  protected editable(member: StaffResponse): boolean {
    return member.id !== this.currentUserId() && canManage(this.store.role(), member.role);
  }

  protected openCreate(): void {
    const ref = this.dialog.open<StaffResponse, StaffFormData>(StaffFormDialog, {
      data: { roles: assignableRoles(this.store.role()) },
      ariaLabel: 'Nuevo miembro del staff',
    });
    ref.closed.subscribe((created) => {
      if (created) {
        this.toast.success(`${created.fullName} ya puede iniciar sesión`);
        this.staff.reload();
      }
    });
  }

  protected changeRole(member: StaffResponse, role: Role): void {
    if (role !== member.role) {
      void this.update(member, { role }, 'Rol actualizado');
    }
  }

  protected toggleActive(member: StaffResponse): void {
    void this.update(member, { active: !member.active }, member.active ? 'Usuario desactivado' : 'Usuario activado');
  }

  private async update(member: StaffResponse, body: UpdateStaffRequest, okMessage: string): Promise<void> {
    this.busyId.set(member.id);
    try {
      await this.api.invoke(updateStaff, { id: member.id, body });
      this.toast.success(okMessage);
    } catch (err) {
      this.toast.error(apiErrorMessage(err));
    } finally {
      this.busyId.set(null);
      this.staff.reload(); // también revierte el <select> si el cambio falló
    }
  }
}
