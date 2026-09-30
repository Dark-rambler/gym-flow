import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

export type BadgeTone = 'green' | 'blue' | 'amber' | 'red' | 'neutral';

const TONES: Record<BadgeTone, string> = {
  green: 'bg-emerald-50 text-emerald-700 ring-emerald-600/20',
  blue: 'bg-sky-50 text-sky-700 ring-sky-600/20',
  amber: 'bg-amber-50 text-amber-800 ring-amber-600/20',
  red: 'bg-red-50 text-red-700 ring-red-600/20',
  neutral: 'bg-neutral-100 text-neutral-600 ring-neutral-500/20',
};

@Component({
  selector: 'gf-badge',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '[class]': 'classes()' },
  template: `<ng-content />`,
})
export class BadgeComponent {
  readonly tone = input<BadgeTone>('neutral');
  protected readonly classes = computed(
    () => 'inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ring-1 ring-inset ' + TONES[this.tone()],
  );
}
