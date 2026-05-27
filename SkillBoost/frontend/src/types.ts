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
