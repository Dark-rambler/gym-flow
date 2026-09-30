import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

type Variant = 'primary' | 'secondary' | 'danger' | 'ghost';

const VARIANTS: Record<Variant, string> = {
  primary: 'bg-brand-600 text-white hover:bg-brand-700 focus-visible:outline-brand-600',
  secondary: 'bg-white text-neutral-800 ring-1 ring-inset ring-neutral-300 hover:bg-neutral-50 focus-visible:outline-neutral-400',
  danger: 'bg-red-600 text-white hover:bg-red-700 focus-visible:outline-red-600',
  ghost: 'text-neutral-600 hover:bg-neutral-100 hover:text-neutral-900 focus-visible:outline-neutral-400',
};

/** Uso: <button gfButton variant="primary" [loading]="saving()">Guardar</button> */
@Component({
  selector: 'button[gfButton]',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '[class]': 'classes()',
    '[attr.aria-busy]': 'loading() || null',
    '[disabled]': 'loading() || disabled()',
  },
  template: `
    @if (loading()) {
      <span class="size-4 animate-spin rounded-full border-2 border-current border-r-transparent" aria-hidden="true"></span>
    }
    <ng-content />
  `,
})
export class ButtonComponent {
  readonly variant = input<Variant>('primary');
  readonly loading = input(false);
  readonly disabled = input(false);
  readonly block = input(false);

  protected readonly classes = computed(
    () =>
      'inline-flex items-center justify-center gap-2 rounded-lg px-4 py-2.5 text-sm font-semibold transition ' +
      'focus-visible:outline-2 focus-visible:outline-offset-2 disabled:cursor-not-allowed disabled:opacity-60 ' +
      VARIANTS[this.variant()] +
      (this.block() ? ' w-full' : ''),
  );
}
