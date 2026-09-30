import { ChangeDetectionStrategy, Component, computed, inject, input, resource, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Api } from '../../api/api';
import { getMemberQr, rotateMemberQr } from '../../api/functions';
import { AuthStore } from '../../core/auth/auth.store';
import { apiErrorMessage } from '../../core/http/api-error';
import { ConfirmService } from '../../shared/ui/confirm/confirm.service';
import { QrCodeComponent } from '../../shared/ui/qr-code/qr-code.component';
import { ToastService } from '../../shared/ui/toast/toast.service';
import { publicCardUrl } from '../checkin/check-in-code';

/** Sección "Carnet y QR" de la ficha del socio. */
@Component({
  selector: 'gf-member-qr-card',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, QrCodeComponent],
  host: { class: 'block rounded-xl bg-white p-5 ring-1 ring-neutral-200' },
  template: `
    <h3 class="text-sm font-semibold uppercase tracking-wide text-neutral-500">Carnet y QR</h3>
    @if (qr.value(); as q) {
      <div class="mt-3 flex flex-col items-center gap-4 sm:flex-row sm:items-start">
        <gf-qr-code [value]="q.payload" [size]="128" alt="QR de acceso del socio" class="rounded-lg ring-1 ring-neutral-200" />
        <div class="flex flex-1 flex-col gap-2 text-sm">
          <p class="text-neutral-600">Con este QR el socio entra por recepción. También puede mostrarlo desde su celular.</p>
          <div class="flex flex-wrap gap-2">
            <a [routerLink]="['/imprimir/carnet', memberId()]" target="_blank"
               class="rounded-lg px-3 py-1.5 font-medium text-neutral-700 ring-1 ring-neutral-300 hover:bg-neutral-50">Imprimir carnet</a>
            <button type="button" class="rounded-lg px-3 py-1.5 font-medium text-neutral-700 ring-1 ring-neutral-300 hover:bg-neutral-50"
                    (click)="copyLink(q.payload)">Copiar enlace para el celular</button>
            @if (canRotate()) {
              <button type="button" class="rounded-lg px-3 py-1.5 font-medium text-red-700 hover:bg-red-50 disabled:opacity-50"
                      [disabled]="rotating()" (click)="rotate()">Regenerar QR</button>
            }
          </div>
        </div>
      </div>
    } @else if (qr.error()) {
      <p class="mt-3 text-sm text-red-700">No se pudo cargar el QR.</p>
    } @else {
      <p class="mt-3 text-sm text-neutral-500">Cargando…</p>
    }
  `,
})
export class MemberQrCardComponent {
  private readonly api = inject(Api);
  private readonly store = inject(AuthStore);
  private readonly confirm = inject(ConfirmService);
  private readonly toast = inject(ToastService);

  readonly memberId = input.required<number>();
  protected readonly qr = resource({
    params: () => ({ id: this.memberId() }),
    loader: ({ params }) => this.api.invoke(getMemberQr, params),
  });
  protected readonly canRotate = computed(() => this.store.hasRole('OWNER', 'ADMIN'));
  protected readonly rotating = signal(false);

  protected async copyLink(payload: string): Promise<void> {
    const url = publicCardUrl(payload);
    try {
      await navigator.clipboard.writeText(url);
      this.toast.success('Enlace copiado: envíaselo al socio por WhatsApp');
    } catch {
      window.prompt('Copia el enlace:', url);
    }
  }

  protected async rotate(): Promise<void> {
    const ok = await this.confirm.ask({
      title: '¿Regenerar QR?',
      message: 'El carnet impreso y el enlace que tenga el socio dejarán de funcionar. Úsalo si lo perdió o si alguien más lo está usando.',
      confirmLabel: 'Regenerar',
      danger: true,
    });
    if (!ok) return;
    this.rotating.set(true);
    try {
      this.qr.set(await this.api.invoke(rotateMemberQr, { id: this.memberId() }));
      this.toast.success('QR regenerado: imprime un carnet nuevo o envía el nuevo enlace');
    } catch (err) {
      this.toast.error(apiErrorMessage(err));
    } finally {
      this.rotating.set(false);
    }
  }
}
