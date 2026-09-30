import { ChangeDetectionStrategy, Component, effect, input, signal } from '@angular/core';
import QRCode from 'qrcode';

/** QR como imagen (data URL PNG). Nivel de corrección M: aguanta carnets gastados o pantallas con brillo. */
@Component({
  selector: 'gf-qr-code',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'inline-block' },
  template: `
    @if (src()) {
      <img [src]="src()" [width]="size()" [height]="size()" [alt]="alt()" class="block [image-rendering:pixelated]" />
    } @else {
      <div class="animate-pulse rounded bg-neutral-100" [style.width.px]="size()" [style.height.px]="size()"></div>
    }
  `,
})
export class QrCodeComponent {
  readonly value = input.required<string>();
  readonly size = input(200);
  readonly alt = input('Código QR');
  protected readonly src = signal<string | null>(null);

  constructor() {
    effect(() => {
      const value = this.value();
      const size = this.size();
      this.src.set(null);
      QRCode.toDataURL(value, { errorCorrectionLevel: 'M', margin: 1, width: size * 2 })
        .then((url) => this.value() === value && this.src.set(url))
        .catch(() => this.src.set(null));
    });
  }
}
