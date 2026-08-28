import { HttpErrorResponse } from '@angular/common/http';
import { Component, EventEmitter, Output, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { StorageService } from '../../core/storage.service';
import { StorageType } from '../../core/storage.model';

@Component({
  selector: 'app-storage-form',
  imports: [ReactiveFormsModule],
  templateUrl: './storage-form.component.html',
  styleUrl: './storage-form.component.css',
})
export class StorageFormComponent {
  private readonly fb = inject(FormBuilder);
  private readonly storageService = inject(StorageService);

  @Output() created = new EventEmitter<void>();

  readonly storageTypes: StorageType[] = ['STAGING', 'PERMANENT'];
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    storageType: this.fb.nonNullable.control<StorageType>('STAGING', Validators.required),
    bucket: this.fb.nonNullable.control('', Validators.required),
    path: this.fb.nonNullable.control('', Validators.required),
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.errorMessage.set(null);
    this.storageService.create(this.form.getRawValue()).subscribe({
      next: () => {
        this.submitting.set(false);
        this.form.reset({ storageType: 'STAGING', bucket: '', path: '' });
        this.created.emit();
      },
      error: (err: HttpErrorResponse) => {
        this.submitting.set(false);
        this.errorMessage.set(err.error?.errorMessage ?? 'Failed to create the storage entry.');
      },
    });
  }
}
