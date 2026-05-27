import type {
  AdminExercise,
  AuthResponse,
  Exercise,
  MeResponse,
  Progress,
  SolutionResponse,
  SubmissionResult,
} from './types';

const TOKEN_KEY = 'skillboost.token';

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string | null) {
  if (token) localStorage.setItem(TOKEN_KEY, token);
  else localStorage.removeItem(TOKEN_KEY);
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers: Record<string, string> = {
    ...(init.headers as Record<string, string> | undefined),
  };
  if (init.body && !headers['Content-Type']) {
    headers['Content-Type'] = 'application/json';
  }
  const token = getToken();
  if (token) headers['Authorization'] = `Bearer ${token}`;
  const res = await fetch(path, { ...init, headers });
  if (!res.ok) {
    let detail = '';
    try {
      const data = await res.json();
      detail = data?.error ?? JSON.stringify(data);
    } catch {
      detail = await res.text();
    }
    throw new Error(detail || `${res.status} ${res.statusText}`);
  }
  if (res.status === 204) return undefined as T;
  return res.json() as Promise<T>;
}

// --- Auth -----------------------------------------------------------------
export function login(username: string, password: string): Promise<AuthResponse> {
  return request<AuthResponse>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  });
}

export function register(
  username: string,
  password: string,
  email: string,
): Promise<AuthResponse> {
  return request<AuthResponse>('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify({ username, password, email }),
  });
}

export function fetchMe(): Promise<MeResponse> {
  return request<MeResponse>('/api/me');
}

// --- Exercises (public) ---------------------------------------------------
export function fetchExercises(): Promise<Exercise[]> {
  return request<Exercise[]>('/api/exercises');
}

export function fetchExercise(id: string): Promise<Exercise> {
  return request<Exercise>(`/api/exercises/${id}`);
}

export function fetchSolution(id: string): Promise<SolutionResponse> {
  return request<SolutionResponse>(`/api/exercises/${id}/solution`);
}

export function submit(exerciseId: string, code: string): Promise<SubmissionResult> {
  return request<SubmissionResult>('/api/submissions', {
    method: 'POST',
    body: JSON.stringify({ exerciseId, code }),
  });
}

// --- Progress (user) ------------------------------------------------------
export function fetchProgress(): Promise<Progress[]> {
  return request<Progress[]>('/api/me/progress');
}

// --- Admin ---------------------------------------------------------------
export function adminListExercises(): Promise<AdminExercise[]> {
  return request<AdminExercise[]>('/api/admin/exercises');
}

export function adminGetExercise(id: string): Promise<AdminExercise> {
  return request<AdminExercise>(`/api/admin/exercises/${id}`);
}

export function adminCreateExercise(ex: AdminExercise): Promise<AdminExercise> {
  return request<AdminExercise>('/api/admin/exercises', {
    method: 'POST',
    body: JSON.stringify(ex),
  });
}

export function adminUpdateExercise(id: string, ex: AdminExercise): Promise<AdminExercise> {
  return request<AdminExercise>(`/api/admin/exercises/${id}`, {
    method: 'PUT',
    body: JSON.stringify(ex),
  });
}

export function adminDeleteExercise(id: string): Promise<void> {
  return request<void>(`/api/admin/exercises/${id}`, { method: 'DELETE' });
}
