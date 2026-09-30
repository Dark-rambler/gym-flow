import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** Marco común de login y registro. */
@Component({
  selector: 'gf-auth-card',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'flex min-h-screen items-center justify-center bg-neutral-100 px-4 py-10' },
  template: `
    <div class="w-full max-w-md">
      <p class="mb-6 text-center text-2xl font-bold tracking-tight text-neutral-900">gym<span class="text-brand-600">Flow</span></p>
      <div class="rounded-2xl bg-white p-6 shadow-sm ring-1 ring-neutral-200 sm:p-8">
        <h1 class="text-xl font-semibold text-neutral-900">{{ title() }}</h1>
        @if (subtitle()) {
          <p class="mt-1 text-sm text-neutral-500">{{ subtitle() }}</p>
        }
        <div class="mt-6">
          <ng-content />
        </div>
      </div>
      <div class="mt-6 text-center text-sm text-neutral-600">
        <ng-content select="[footer]" />
      </div>
    </div>
  `,
})
export class AuthCardComponent {
  readonly title = input.required<string>();
  readonly subtitle = input<string>();
}
