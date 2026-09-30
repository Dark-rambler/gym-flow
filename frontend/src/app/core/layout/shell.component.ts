import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../auth/auth.service';
import { AuthStore, Role } from '../auth/auth.store';
import { IconComponent, IconName } from '../../shared/ui/icon/icon.component';

interface NavItem {
  label: string;
  icon: IconName;
  path?: string;
  roles?: Role[];
  soon?: boolean;
}

const NAV: NavItem[] = [
  { label: 'Dashboard', icon: 'home', path: '/' },
  { label: 'Socios', icon: 'users', path: '/socios' },
  { label: 'Planes', icon: 'card', path: '/planes' },
  { label: 'Caja', icon: 'cash', path: '/caja' },
  { label: 'Check-in', icon: 'qr', soon: true },
  { label: 'Staff', icon: 'shield', path: '/staff', roles: ['OWNER', 'ADMIN'] },
];

const ROLE_LABEL: Record<Role, string> = { OWNER: 'Dueño', ADMIN: 'Administrador', RECEPTIONIST: 'Recepción' };

@Component({
  selector: 'gf-shell',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, IconComponent],
  template: `
    <div class="min-h-screen lg:flex">
      <!-- overlay móvil -->
      @if (menuOpen()) {
        <div class="fixed inset-0 z-30 bg-neutral-900/40 lg:hidden" (click)="menuOpen.set(false)"></div>
      }

      <aside
        class="fixed inset-y-0 left-0 z-40 flex w-64 flex-col bg-neutral-900 text-neutral-300 transition-transform lg:static lg:translate-x-0"
        [class.-translate-x-full]="!menuOpen()"
      >
        <div class="flex h-16 items-center justify-between px-5">
          <span class="text-lg font-bold tracking-tight text-white">gym<span class="text-brand-500">Flow</span></span>
          <button type="button" class="rounded p-1 hover:bg-white/10 lg:hidden" (click)="menuOpen.set(false)" aria-label="Cerrar menú">
            <gf-icon name="close" />
          </button>
        </div>

        <nav class="flex-1 space-y-1 px-3 py-2" aria-label="Principal">
          @for (item of nav(); track item.label) {
            @if (item.path) {
              <a
                [routerLink]="item.path"
                routerLinkActive="bg-white/10 text-white"
                [routerLinkActiveOptions]="{ exact: item.path === '/' }"
                class="flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium hover:bg-white/5 hover:text-white"
                (click)="menuOpen.set(false)"
              >
                <gf-icon [name]="item.icon" />
                {{ item.label }}
              </a>
            } @else {
              <span class="flex cursor-not-allowed items-center gap-3 rounded-lg px-3 py-2 text-sm text-neutral-500" [attr.aria-disabled]="true">
                <gf-icon [name]="item.icon" />
                {{ item.label }}
                <span class="ml-auto rounded bg-white/5 px-1.5 py-0.5 text-[10px] font-semibold uppercase tracking-wide">Pronto</span>
              </span>
            }
          }
        </nav>
      </aside>

      <div class="flex min-w-0 flex-1 flex-col">
        <header class="sticky top-0 z-20 flex h-16 items-center gap-3 border-b border-neutral-200 bg-white/90 px-4 backdrop-blur sm:px-6">
          <button type="button" class="-ml-1 rounded p-1.5 text-neutral-600 hover:bg-neutral-100 lg:hidden" (click)="menuOpen.set(true)" aria-label="Abrir menú">
            <gf-icon name="menu" />
          </button>
          <h1 class="truncate text-base font-semibold text-neutral-900">{{ user()?.gymName }}</h1>
          <div class="ml-auto flex items-center gap-3">
            <div class="hidden text-right sm:block">
              <p class="text-sm font-medium text-neutral-900">{{ user()?.fullName }}</p>
              <p class="text-xs text-neutral-500">{{ roleLabel() }}</p>
            </div>
            <button type="button" (click)="logout()" class="flex items-center gap-1.5 rounded-lg px-2.5 py-1.5 text-sm text-neutral-600 hover:bg-neutral-100 hover:text-neutral-900">
              <gf-icon name="logout" [size]="18" />
              <span class="hidden sm:inline">Salir</span>
            </button>
          </div>
        </header>

        <main class="flex-1 px-4 py-6 sm:px-6 lg:px-8">
          <router-outlet />
        </main>
      </div>
    </div>
  `,
})
export class ShellComponent {
  private readonly store = inject(AuthStore);
  private readonly auth = inject(AuthService);

  protected readonly menuOpen = signal(false);
  protected readonly user = this.store.user;
  protected readonly roleLabel = computed(() => {
    const role = this.store.role();
    return role ? ROLE_LABEL[role] : '';
  });
  protected readonly nav = computed(() =>
    NAV.filter((item) => !item.roles || this.store.hasRole(...item.roles)),
  );

  protected logout(): void {
    void this.auth.logout();
  }
}
