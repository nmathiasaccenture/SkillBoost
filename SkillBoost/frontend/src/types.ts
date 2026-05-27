export interface Exercise {
  id: string;
  language: string;
  difficulty: number;
  title: string;
  description: string;
  hint: string | null;
  buggyCode: string;
  tests: TestCase[];
}

export interface AdminExercise extends Exercise {
  solutionCode: string;
  testHarness: string;
}

export interface SolutionResponse {
  solutionCode: string;
}

export interface TestCase {
  input: string;
  expected: string;
}

export interface TestResult {
  input: string;
  expected: string;
  actual: string;
  passed: boolean;
  error: string | null;
}

export interface SubmissionResult {
  compiled: boolean;
  compileError: string | null;
  allPassed: boolean;
  results: TestResult[];
}

export type Role = 'USER' | 'ADMIN';

export interface AuthUser {
  username: string;
  role: Role;
  token: string;
}

export interface AuthResponse {
  token: string;
  username: string;
  role: Role;
  expiresInMs: number;
}

export interface MeResponse {
  username: string;
  email: string;
  role: Role;
  createdAt: string;
}

export interface Progress {
  exerciseId: string;
  solved: boolean;
  firstSolvedAt: string | null;
  attempts: number;
}
