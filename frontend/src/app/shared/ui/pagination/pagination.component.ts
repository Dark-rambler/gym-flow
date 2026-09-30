import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

/** page empieza en 0 (igual que la API). */
@Component({
  selector: 'gf-pagination',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (totalPages() > 1) {
      <nav class="flex items-center justify-between gap-3 text-sm text-neutral-600" aria-label="Paginación">
        <span>{{ totalItems() }} resultados · página {{ page() + 1 }} de {{ totalPages() }}</span>
        <div class="flex gap-2">
          <button type="button" class="rounded-lg px-3 py-1.5 ring-1 ring-neutral-300 hover:bg-neutral-50 disabled:opacity-40"
                  [disabled]="page() === 0" (click)="pageChange.emit(page() - 1)">Anterior</button>
          <button type="button" class="rounded-lg px-3 py-1.5 ring-1 ring-neutral-300 hover:bg-neutral-50 disabled:opacity-40"
                  [disabled]="page() >= totalPages() - 1" (click)="pageChange.emit(page() + 1)">Siguiente</button>
        </div>
      </nav>
    }
  `,
})
export class PaginationComponent {
  readonly page = input.required<number>();
  readonly totalPages = input.required<number>();
  readonly totalItems = input.required<number>();
  readonly pageChange = output<number>();
}
