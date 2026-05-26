import type { Exercise, SubmissionResult } from './types';

export async function fetchExercises(): Promise<Exercise[]> {
  const res = await fetch('/api/exercises');
  if (!res.ok) throw new Error(`Failed to load exercises: ${res.status}`);
  return res.json();
}

export async function fetchExercise(id: string): Promise<Exercise> {
  const res = await fetch(`/api/exercises/${id}`);
  if (!res.ok) throw new Error(`Failed to load exercise: ${res.status}`);
  return res.json();
}

export async function submit(exerciseId: string, code: string): Promise<SubmissionResult> {
  const res = await fetch('/api/submissions', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ exerciseId, code }),
  });
  if (!res.ok) throw new Error(`Submission failed: ${res.status}`);
  return res.json();
}
