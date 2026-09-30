import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ToastService } from './toast.service';

@Component({
  selector: 'gf-toast-outlet',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'pointer-events-none fixed inset-x-4 bottom-4 z-[1100] flex flex-col items-center gap-2 sm:inset-x-auto sm:right-4 sm:items-end' },
  template: `
    @for (toast of toasts.toasts(); track toast.id) {
      <div
        role="status"
        class="pointer-events-auto flex w-full max-w-sm items-start gap-3 rounded-lg px-4 py-3 text-sm shadow-lg ring-1"
        [class]="toast.kind === 'error' ? 'bg-red-50 text-red-800 ring-red-200' : 'bg-white text-neutral-800 ring-neutral-200'"
      >
        <span class="flex-1">{{ toast.message }}</span>
        <button type="button" class="text-neutral-400 hover:text-neutral-700" (click)="toasts.dismiss(toast.id)" aria-label="Cerrar">✕</button>
      </div>
    }
  `,
})
export class ToastOutletComponent {
  protected readonly toasts = inject(ToastService);
}
