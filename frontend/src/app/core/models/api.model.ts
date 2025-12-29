/**
 * Common API models and types
 */

export interface ApiResponse<T> {
  data: T;
  message?: string;
  success: boolean;
  timestamp: Date;
}

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface ErrorResponse {
  error: string;
  message: string;
  status: number;
  timestamp: Date;
  path: string;
}

export interface ValidationError {
  field: string;
  message: string;
  rejectedValue?: any;
}

export interface ApiError {
  status: number;
  message: string;
  errors?: ValidationError[];
  timestamp: Date;
}

export interface FileUploadResponse {
  filename: string;
  originalName: string;
  size: number;
  mimeType: string;
  url: string;
}

export interface SearchCriteria {
  page?: number;
  size?: number;
  sort?: string;
  direction?: 'ASC' | 'DESC';
  filters?: { [key: string]: any };
}

export interface StatusUpdate {
  id: number;
  status: string;
  comment?: string;
  updatedBy: number;
  updatedAt: Date;
}