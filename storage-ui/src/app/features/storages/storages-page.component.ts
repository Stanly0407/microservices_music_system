import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { HasRolesDirective } from 'keycloak-angular';

import { StorageService } from '../../core/storage.service';
import { StorageResponse } from '../../core/storage.model';
import { StorageFormComponent } from './storage-form.component';

@Component({
  selector: 'app-storages-page',
  imports: [HasRolesDirective, StorageFormComponent],
  templateUrl: './storages-page.component.html',
  styleUrl: './storages-page.component.css',
})
export class StoragesPageComponent {
  private readonly storageService = inject(StorageService);

  readonly storages = signal<StorageResponse[]>([]);
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);

  constructor() {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.storageService.list().subscribe({
      next: (storages) => {
        this.storages.set(storages);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.errorMessage.set(this.toMessage(err));
        this.loading.set(false);
      },
    });
  }

  delete(storage: StorageResponse): void {
    if (!confirm(`Delete storage "${storage.bucket}${storage.path}"?`)) {
      return;
    }
    this.storageService.delete(storage.id).subscribe({
      next: () => this.reload(),
      error: (err: HttpErrorResponse) => this.errorMessage.set(this.toMessage(err)),
    });
  }

  private toMessage(err: HttpErrorResponse): string {
    if (err.status === 401) {
      return 'Your session has expired. Refresh the page and sign in again.';
    }
    if (err.status === 403) {
      return 'You do not have permission to perform this action.';
    }
    return err.error?.errorMessage ?? 'The request could not be completed.';
  }
}
