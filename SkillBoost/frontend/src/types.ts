export interface Exercise {
  id: string;
  language: string;
  difficulty: number;
  title: string;
  description: string;
  buggyCode: string;
  tests: TestCase[];
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
