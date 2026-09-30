import { ChangeDetectionStrategy, Component, ElementRef, OnDestroy, afterNextRender, inject, signal, viewChild } from '@angular/core';
import { DialogRef } from '@angular/cdk/dialog';
import { ButtonComponent } from '../../shared/ui/button/button.component';

/**
 * Lee un QR con la cámara (trasera por defecto). La librería (~150 kB) se carga solo al abrir este diálogo.
 * Cierra con el texto leído, o undefined si se cancela.
 */
@Component({
  selector: 'gf-camera-scanner-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ButtonComponent],
  host: { class: 'block w-[calc(100vw-2rem)] max-w-md rounded-2xl bg-white p-4 shadow-xl' },
  template: `
    <div class="relative overflow-hidden rounded-xl bg-neutral-900">
      <video #video class="aspect-square w-full object-cover" muted playsinline></video>
      <div class="pointer-events-none absolute inset-8 rounded-2xl border-4 border-white/70"></div>
    </div>
    @if (error()) {
      <p class="mt-3 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700" role="alert">{{ error() }}</p>
    } @else {
      <p class="mt-3 text-center text-sm text-neutral-600">{{ starting() ? 'Activando cámara…' : 'Apunta al QR del socio' }}</p>
    }
    <div class="mt-4 flex justify-end">
      <button gfButton type="button" variant="secondary" (click)="ref.close()">Cerrar</button>
    </div>
  `,
})
export class CameraScannerDialog implements OnDestroy {
  protected readonly ref = inject<DialogRef<string>>(DialogRef);
  private readonly video = viewChild.required<ElementRef<HTMLVideoElement>>('video');
  protected readonly starting = signal(true);
  protected readonly error = signal<string | null>(null);
  private controls: { stop(): void } | null = null;
  private done = false;

  constructor() {
    afterNextRender(() => void this.start());
  }

  private async start(): Promise<void> {
    try {
      const { BrowserQRCodeReader } = await import('@zxing/browser');
      const reader = new BrowserQRCodeReader();
      this.controls = await reader.decodeFromConstraints(
        { video: { facingMode: 'environment' } },
        this.video().nativeElement,
        (result) => {
          if (result && !this.done) {
            this.done = true;
            this.stop();
            this.ref.close(result.getText());
          }
        },
      );
      this.starting.set(false);
    } catch (err) {
      this.starting.set(false);
      const name = err instanceof DOMException ? err.name : '';
      this.error.set(
        name === 'NotAllowedError'
          ? 'Permiso de cámara denegado. Actívalo en el navegador o usa el lector/DNI.'
          : 'No se pudo usar la cámara en este dispositivo.',
      );
    }
  }

  private stop(): void {
    this.controls?.stop();
    this.controls = null;
  }

  ngOnDestroy(): void {
    this.stop();
  }
}
