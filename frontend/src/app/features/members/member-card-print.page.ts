import { ChangeDetectionStrategy, Component, computed, effect, inject, input, resource } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Api } from '../../api/api';
import { getMember, getMemberQr } from '../../api/functions';
import { AuthStore } from '../../core/auth/auth.store';
import { QrCodeComponent } from '../../shared/ui/qr-code/qr-code.component';

/** Carnet tamaño tarjeta (CR80, 85.6 × 54 mm) listo para imprimir. Ruta fuera del layout, abre el diálogo de impresión solo. */
@Component({
  selector: 'gf-member-card-print-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, QrCodeComponent],
  styles: `
    @page { size: 85.6mm 54mm; margin: 0; }
    @media print {
      .no-print { display: none !important; }
      :host { padding: 0 !important; background: white !important; }
      .card { box-shadow: none !important; border: none !important; }
    }
  `,
  host: { class: 'flex min-h-screen flex-col items-center gap-6 bg-neutral-100 p-8' },
  template: `
    @if (member.value(); as m) {
      @if (qr.value(); as q) {
        <div class="card flex h-[54mm] w-[85.6mm] overflow-hidden rounded-[3mm] border border-neutral-300 bg-white shadow-lg">
          <div class="flex flex-1 flex-col justify-between p-[4mm]">
            <div>
              <p class="text-[3mm] font-semibold uppercase tracking-wide text-brand-700">{{ gymName() }}</p>
              <p class="mt-[1mm] text-[2.4mm] text-neutral-500">Carnet de socio</p>
            </div>
            <div>
              <p class="text-[4.2mm] font-bold leading-tight text-neutral-900">{{ m.fullName }}</p>
              <p class="mt-[1mm] text-[2.8mm] text-neutral-600">DNI {{ m.dni }}</p>
            </div>
            <p class="text-[2.2mm] text-neutral-400">Muestra este código en recepción</p>
          </div>
          <div class="flex items-center justify-center bg-white pr-[3mm]">
            <gf-qr-code [value]="q.payload" [size]="150" alt="QR de acceso" class="h-[40mm] w-[40mm] [&_img]:h-full [&_img]:w-full" />
          </div>
        </div>
        <div class="no-print flex gap-3">
          <button type="button" class="rounded-lg bg-brand-600 px-4 py-2 text-sm font-semibold text-white" (click)="print()">Imprimir</button>
          <a [routerLink]="['/socios', m.id]" class="rounded-lg px-4 py-2 text-sm font-semibold text-neutral-700 ring-1 ring-neutral-300">Volver a la ficha</a>
        </div>
      }
    } @else if (member.error() || qr.error()) {
      <p class="text-sm text-red-700">No se pudo cargar el carnet.</p>
    } @else {
      <p class="text-sm text-neutral-500">Preparando carnet…</p>
    }
  `,
})
export class MemberCardPrintPage {
  private readonly api = inject(Api);
  private readonly store = inject(AuthStore);
  readonly id = input.required<string>();

  protected readonly member = resource({
    params: () => ({ id: Number(this.id()) }),
    loader: ({ params }) => this.api.invoke(getMember, params),
  });
  protected readonly qr = resource({
    params: () => ({ id: Number(this.id()) }),
    loader: ({ params }) => this.api.invoke(getMemberQr, params),
  });
  protected readonly gymName = computed(() => this.store.user()?.gymName ?? '');
  private printed = false;

  constructor() {
    // imprimir una sola vez cuando todo cargó (el QR se dibuja en el siguiente ciclo, por eso el pequeño retraso)
    effect(() => {
      if (!this.printed && this.member.hasValue() && this.qr.hasValue()) {
        this.printed = true;
        setTimeout(() => window.print(), 400);
      }
    });
  }

  protected print(): void {
    window.print();
  }
}
