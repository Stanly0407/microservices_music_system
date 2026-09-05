export type StorageType = 'STAGING' | 'PERMANENT';

export interface StorageResponse {
  id: number;
  storageType: StorageType;
  bucket: string;
  path: string;
}

export interface StorageRequest {
  storageType: StorageType;
  bucket: string;
  path: string;
}

export interface StorageIdResponse {
  id: number;
}

export interface DeletedStorageIdsResponse {
  ids: number[];
}
