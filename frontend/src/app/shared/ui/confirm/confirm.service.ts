import { ChangeDetectionStrategy, Component, Injectable, inject } from '@angular/core';
import { DIALOG_DATA, Dialog, DialogRef } from '@angular/cdk/dialog';
import { firstValueFrom } from 'rxjs';
import { ButtonComponent } from '../button/button.component';

export interface ConfirmOptions {
  title: string;
  message: string;
  confirmLabel?: string;
  danger?: boolean;
}

@Component({
  selector: 'gf-confirm-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ButtonComponent],
  host: { class: 'block w-[calc(100vw-2rem)] max-w-sm rounded-2xl bg-white p-6 shadow-xl' },
  template: `
    <h2 class="text-lg font-semibold text-neutral-900">{{ data.title }}</h2>
    <p class="mt-2 text-sm text-neutral-600">{{ data.message }}</p>
    <div class="mt-6 flex justify-end gap-2">
      <button gfButton type="button" variant="secondary" (click)="ref.close(false)">Volver</button>
      <button gfButton type="button" [variant]="data.danger ? 'danger' : 'primary'" (click)="ref.close(true)">
        {{ data.confirmLabel ?? 'Confirmar' }}
      </button>
    </div>
  `,
})
class ConfirmDialogComponent {
  protected readonly data = inject<ConfirmOptions>(DIALOG_DATA);
  protected readonly ref = inject<DialogRef<boolean>>(DialogRef);
}

/** const ok = await confirm.ask({ title: '¿Cancelar?', message: '...', danger: true }); */
@Injectable({ providedIn: 'root' })
export class ConfirmService {
  private readonly dialog = inject(Dialog);

  async ask(options: ConfirmOptions): Promise<boolean> {
    const ref = this.dialog.open<boolean, ConfirmOptions>(ConfirmDialogComponent, {
      data: options,
      role: 'alertdialog',
      ariaLabel: options.title,
    });
    return (await firstValueFrom(ref.closed)) === true;
  }
}
