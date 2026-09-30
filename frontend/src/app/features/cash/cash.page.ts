import { ChangeDetectionStrategy, Component, computed, inject, resource, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Dialog } from '@angular/cdk/dialog';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Api } from '../../api/api';
import { getCurrentCash, openCash } from '../../api/functions';
import { CashSessionDetailResponse, PaymentResponse } from '../../api/models';
import { AuthStore } from '../../core/auth/auth.store';
import { apiErrorMessage } from '../../core/http/api-error';
import { ButtonComponent } from '../../shared/ui/button/button.component';
import { FormFieldComponent } from '../../shared/ui/form-field/form-field.component';
import { ToastService } from '../../shared/ui/toast/toast.service';
import { CashSessionViewComponent } from './cash-session-view.component';
import { CloseCashDialog } from './close-cash.dialog';
import { VoidPaymentDialog } from './void-payment.dialog';

@Component({
  selector: 'gf-cash-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, RouterLink, ReactiveFormsModule, ButtonComponent, FormFieldComponent, CashSessionViewComponent],
  template: `
    <div class="mx-auto max-w-5xl">
      <div class="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h2 class="text-2xl font-semibold tracking-tight text-neutral-900">Caja</h2>
          @if (current(); as c) {
            <p class="mt-1 text-sm text-neutral-600">
              <span class="mr-1 inline-block size-2 rounded-full bg-emerald-500 align-middle"></span>
              Abierta por {{ c.session.openedByName }} el {{ c.session.openedAt | date: "dd/MM 'a las' HH:mm" }}
            </p>
          } @else {
            <p class="mt-1 text-sm text-neutral-600">Registra aquí los cobros del turno y cuadra el efectivo al cerrar.</p>
          }
        </div>
        <div class="flex gap-2">
          @if (isManager()) {
            <a routerLink="/caja/historial" class="rounded-lg px-4 py-2.5 text-sm font-semibold text-neutral-700 ring-1 ring-neutral-300 hover:bg-neutral-50">Historial</a>
          }
          @if (current(); as c) {
            <button gfButton type="button" (click)="close(c)">Cerrar caja</button>
          }
        </div>
      </div>

      @if (status.isLoading() && !status.hasValue()) {
        <p class="mt-6 text-sm text-neutral-500">Cargando…</p>
      } @else if (status.error()) {
        <p class="mt-6 text-sm text-red-700">{{ loadError() }}
          <button type="button" class="ml-2 font-semibold underline" (click)="status.reload()">Reintentar</button></p>
      } @else if (current(); as c) {
        <gf-cash-session-view class="mt-6 block" [detail]="c" [canVoid]="isManager()" (voidPayment)="voidPayment($event)" />
      } @else {
        <!-- [formGroup] es obligatorio: sin él Angular no captura el submit y el navegador recarga la página -->
        <form class="mt-6 max-w-md rounded-xl bg-white p-6 ring-1 ring-neutral-200" [formGroup]="openForm" (ngSubmit)="open()">
          <h3 class="font-semibold text-neutral-900">La caja está cerrada</h3>
          <p class="mt-1 text-sm text-neutral-600">Ábrela con el efectivo que hay en el cajón para empezar a cobrar.</p>
          <gf-form-field class="mt-4" label="Monto inicial (S/)" for="opening">
            <input id="opening" type="number" min="0" step="0.10" class="gf-input" formControlName="opening" />
          </gf-form-field>
          <button gfButton type="submit" class="mt-4" [block]="true" [loading]="opening.disabled" [disabled]="opening.invalid">Abrir caja</button>
        </form>
      }
    </div>
  `,
})
export class CashPage {
  private readonly api = inject(Api);
  private readonly store = inject(AuthStore);
  private readonly dialog = inject(Dialog);
  private readonly toast = inject(ToastService);

  protected readonly status = resource({ loader: () => this.api.invoke(getCurrentCash) });
  protected readonly current = computed(() => this.status.value()?.current ?? null);
  protected readonly isManager = computed(() => this.store.hasRole('OWNER', 'ADMIN'));
  protected readonly loadError = computed(() => apiErrorMessage(this.status.error(), 'No se pudo cargar la caja'));
  protected readonly opening = new FormControl<number>(0, { nonNullable: true, validators: [Validators.required, Validators.min(0)] });
  protected readonly openForm = new FormGroup({ opening: this.opening });
  private readonly busy = signal(false);

  protected async open(): Promise<void> {
    if (this.opening.invalid || this.busy()) return;
    this.busy.set(true);
    this.opening.disable();
    try {
      const opened = await this.api.invoke(openCash, { body: { openingAmount: Number(this.opening.value) } });
      this.status.set({ current: opened });
      this.toast.success('Caja abierta');
    } catch (err) {
      this.toast.error(apiErrorMessage(err));
      this.status.reload();
    } finally {
      this.opening.enable();
      this.busy.set(false);
    }
  }

  protected async close(c: CashSessionDetailResponse): Promise<void> {
    // el esperado de la pantalla puede ser viejo (otra recepción pudo cobrar mientras tanto): se pide el actual
    let expectedCash = c.session.expectedCash ?? null;
    try {
      const fresh = await this.api.invoke(getCurrentCash);
      this.status.set(fresh);
      if (!fresh.current) {
        this.toast.error('La caja ya fue cerrada');
        return;
      }
      expectedCash = fresh.current.session.expectedCash ?? null;
    } catch (err) {
      this.toast.error(apiErrorMessage(err));
      return;
    }
    this.dialog
      .open<CashSessionDetailResponse>(CloseCashDialog, {
        data: { expectedCash },
        ariaLabel: 'Cerrar caja',
      })
      .closed.subscribe((closed) => {
        if (closed) {
          const diff = closed.session.difference;
          this.toast.success(
            diff === null || diff === undefined
              ? 'Caja cerrada'
              : diff === 0
                ? 'Caja cerrada: cuadra'
                : `Caja cerrada con diferencia de S/ ${diff.toFixed(2)}`,
          );
          this.status.set({ current: undefined });
        }
      });
  }

  protected voidPayment(p: PaymentResponse): void {
    this.dialog
      .open<PaymentResponse>(VoidPaymentDialog, { data: p, ariaLabel: 'Anular pago' })
      .closed.subscribe((voided) => {
        if (voided) {
          this.toast.success('Pago anulado y membresía cancelada');
          this.status.reload();
        }
      });
  }
}
