import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import {
  DeletedStorageIdsResponse,
  StorageIdResponse,
  StorageRequest,
  StorageResponse,
} from './storage.model';

@Injectable({ providedIn: 'root' })
export class StorageService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/storages`;

  list(): Observable<StorageResponse[]> {
    return this.http.get<StorageResponse[]>(this.baseUrl);
  }

  create(request: StorageRequest): Observable<StorageIdResponse> {
    return this.http.post<StorageIdResponse>(this.baseUrl, request);
  }

  delete(id: number): Observable<DeletedStorageIdsResponse> {
    const params = new HttpParams().set('id', String(id));
    return this.http.delete<DeletedStorageIdsResponse>(this.baseUrl, { params });
  }
}
