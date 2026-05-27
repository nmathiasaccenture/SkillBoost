import { useCallback, useEffect, useMemo, useState } from 'react';
import Editor from '@monaco-editor/react';
import { fetchExercises, fetchProgress, fetchSolution, submit } from './api';
import { useAuth } from './auth';
import { AuthDialog } from './components/AuthForms';
import { AdminPanel } from './components/AdminPanel';
import type { Exercise, Progress, SubmissionResult } from './types';

const LANGUAGES: { id: string; label: string }[] = [
  { id: 'java', label: 'Java' },
  { id: 'javascript', label: 'JavaScript' },
  { id: 'python', label: 'Python' },
];

function monacoLanguage(lang: string): string {
  if (lang === 'java' || lang === 'javascript' || lang === 'python') return lang;
  return 'plaintext';
}

export default function App() {
  const { user, loading: authLoading, logout } = useAuth();
  const [exercises, setExercises] = useState<Exercise[]>([]);
  const [language, setLanguage] = useState<string>('java');
  const [selected, setSelected] = useState<Exercise | null>(null);
  const [code, setCode] = useState<string>('');
  const [result, setResult] = useState<SubmissionResult | null>(null);
  const [running, setRunning] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [hintShown, setHintShown] = useState(false);
  const [solutionCode, setSolutionCode] = useState<string | null>(null);
  const [solutionError, setSolutionError] = useState<string | null>(null);
  const [confirmingSolution, setConfirmingSolution] = useState(false);
  const [progress, setProgress] = useState<Progress[]>([]);
  const [authDialogOpen, setAuthDialogOpen] = useState(false);
  const [view, setView] = useState<'practice' | 'admin'>('practice');

  const visible = useMemo(
    () => exercises.filter((ex) => ex.language === language),
    [exercises, language],
  );

  const solvedSet = useMemo(
    () => new Set(progress.filter((p) => p.solved).map((p) => p.exerciseId)),
    [progress],
  );

  const attemptsByExercise = useMemo(() => {
    const map = new Map<string, number>();
    for (const p of progress) map.set(p.exerciseId, p.attempts);
    return map;
  }, [progress]);

  const refreshProgress = useCallback(() => {
    if (!user) return;
    fetchProgress()
      .then(setProgress)
      .catch(() => setProgress([]));
  }, [user]);

  useEffect(() => {
    fetchExercises()
      .then((list) => {
        setExercises(list);
        const first = list.find((ex) => ex.language === 'java') ?? list[0];
        if (first) {
          setLanguage(first.language);
          selectExercise(first);
        }
      })
      .catch((e) => setLoadError(String(e)));
  }, []);

  useEffect(() => {
    if (user) refreshProgress();
    else setProgress([]);
  }, [user, refreshProgress]);

  function pickLanguage(lang: string) {
    if (lang === language) return;
    setLanguage(lang);
    const first = exercises.find((ex) => ex.language === lang);
    if (first) {
      selectExercise(first);
    } else {
      setSelected(null);
      setCode('');
      setResult(null);
      setHintShown(false);
      setSolutionCode(null);
      setSolutionError(null);
    }
  }

  function selectExercise(ex: Exercise) {
    setSelected(ex);
    setCode(ex.buggyCode);
    setResult(null);
    setHintShown(false);
    setSolutionCode(null);
    setSolutionError(null);
    setConfirmingSolution(false);
  }

  async function confirmRevealSolution() {
    if (!selected) return;
    setConfirmingSolution(false);
    try {
      const r = await fetchSolution(selected.id);
      setSolutionCode(r.solutionCode);
      setSolutionError(null);
    } catch (e) {
      setSolutionError(String(e));
    }
  }

  async function runTests() {
    if (!selected) return;
    setRunning(true);
    setResult(null);
    try {
      const r = await submit(selected.id, code);
      setResult(r);
      if (user) refreshProgress();
    } catch (e) {
      setResult({
        compiled: false,
        compileError: String(e),
        allPassed: false,
        results: [],
      });
    } finally {
      setRunning(false);
    }
  }

  if (view === 'admin' && user?.role === 'ADMIN') {
    return (
      <div className="app">
        <header className="header">
          <h1>SkillBoost</h1>
          <span className="tagline">Admin</span>
          <div className="header-right">
            <span className="user-chip">
              {user.username} <span className="role-badge admin">ADMIN</span>
            </span>
            <button className="secondary" onClick={logout}>
              Sign out
            </button>
          </div>
        </header>
        <AdminPanel
          onClose={() => {
            setView('practice');
            // Re-fetch exercises in case admin made changes.
            fetchExercises().then(setExercises).catch(() => {});
          }}
        />
      </div>
    );
  }

  return (
    <div className="app">
      <header className="header">
        <h1>SkillBoost</h1>
        <span className="tagline">Find the bug. Make the tests pass.</span>
        <nav className="lang-tabs">
          {LANGUAGES.map((l) => (
            <button
              key={l.id}
              className={`lang-tab ${language === l.id ? 'active' : ''}`}
              onClick={() => pickLanguage(l.id)}
            >
              {l.label}
            </button>
          ))}
        </nav>
        <div className="header-right">
          {authLoading ? null : user ? (
            <>
              <span className="user-chip">
                {user.username}
                {user.role === 'ADMIN' && <span className="role-badge admin">ADMIN</span>}
              </span>
              {user.role === 'ADMIN' && (
                <button className="secondary" onClick={() => setView('admin')}>
                  Admin
                </button>
              )}
              <button className="secondary" onClick={logout}>
                Sign out
              </button>
            </>
          ) : (
            <button className="secondary" onClick={() => setAuthDialogOpen(true)}>
              Sign in
            </button>
          )}
        </div>
      </header>

      <div className="layout">
        <aside className="sidebar">
          <h2>Exercises</h2>
          {user && (
            <div className="progress-summary">
              {solvedSet.size} / {visible.length} solved in {language}
            </div>
          )}
          {loadError && <div className="error">{loadError}</div>}
          {visible.length === 0 && !loadError && (
            <div className="empty">No exercises for this language yet.</div>
          )}
          <ul>
            {visible.map((ex) => {
              const solved = solvedSet.has(ex.id);
              const attempts = attemptsByExercise.get(ex.id);
              return (
                <li
                  key={ex.id}
                  className={selected?.id === ex.id ? 'active' : ''}
                  onClick={() => selectExercise(ex)}
                >
                  <div className="ex-title">
                    {user && (
                      <span
                        className={`solved-marker ${solved ? 'solved' : ''}`}
                        aria-label={solved ? 'solved' : 'unsolved'}
                        title={solved ? 'Solved' : 'Not yet solved'}
                      >
                        {solved ? '✓' : '○'}
                      </span>
                    )}
                    {ex.title}
                  </div>
                  <div className="ex-meta">
                    {ex.language} · difficulty {ex.difficulty}
                    {user && attempts ? ` · ${attempts} attempt${attempts === 1 ? '' : 's'}` : ''}
                  </div>
                </li>
              );
            })}
          </ul>
        </aside>

        <main className="main">
          {selected ? (
            <>
              <section className="problem">
                <h2>{selected.title}</h2>
                <p>{selected.description}</p>
                {!user && (
                  <p className="muted">
                    <button className="link" onClick={() => setAuthDialogOpen(true)}>
                      Sign in
                    </button>{' '}
                    to save progress across sessions.
                  </p>
                )}
              </section>

              <section className="editor">
                <Editor
                  height="380px"
                  defaultLanguage="java"
                  language={monacoLanguage(selected.language)}
                  value={code}
                  onChange={(v) => setCode(v ?? '')}
                  theme="vs-dark"
                  options={{ fontSize: 14, minimap: { enabled: false } }}
                />
              </section>

              <section className="actions">
                <button onClick={runTests} disabled={running}>
                  {running ? 'Running…' : 'Run tests'}
                </button>
                <button
                  className="secondary"
                  onClick={() => setCode(selected.buggyCode)}
                >
                  Reset code
                </button>
                {selected.hint && (
                  <button
                    className="secondary"
                    onClick={() => setHintShown((s) => !s)}
                  >
                    {hintShown ? 'Hide hint' : 'Show hint'}
                  </button>
                )}
                <button
                  className="secondary"
                  onClick={
                    solutionCode
                      ? () => setSolutionCode(null)
                      : () => setConfirmingSolution(true)
                  }
                >
                  {solutionCode ? 'Hide solution' : 'Show solution'}
                </button>
              </section>

              {confirmingSolution && (
                <div
                  className="modal-backdrop"
                  onClick={() => setConfirmingSolution(false)}
                >
                  <div
                    className="modal"
                    role="dialog"
                    aria-modal="true"
                    aria-labelledby="confirm-solution-title"
                    onClick={(e) => e.stopPropagation()}
                  >
                    <h3 id="confirm-solution-title">Show the full solution?</h3>
                    <p>
                      You will see the complete answer below the editor. Try the
                      hint first if you have not already.
                    </p>
                    <div className="modal-actions">
                      <button
                        className="secondary"
                        onClick={() => setConfirmingSolution(false)}
                      >
                        Cancel
                      </button>
                      <button onClick={confirmRevealSolution}>
                        Show solution
                      </button>
                    </div>
                  </div>
                </div>
              )}

              {hintShown && selected.hint && (
                <section className="hint">
                  <strong>Hint:</strong> {selected.hint}
                </section>
              )}

              {solutionError && (
                <section className="results">
                  <pre className="error-output">{solutionError}</pre>
                </section>
              )}

              {solutionCode && (
                <section className="solution">
                  <h3>Solution (read-only — type it into the editor above)</h3>
                  <Editor
                    height="280px"
                    defaultLanguage="java"
                    language={monacoLanguage(selected.language)}
                    value={solutionCode}
                    theme="vs-dark"
                    options={{
                      fontSize: 14,
                      minimap: { enabled: false },
                      readOnly: true,
                      domReadOnly: true,
                    }}
                  />
                </section>
              )}

              {result && <Results result={result} />}
            </>
          ) : (
            <div className="empty">Select an exercise to begin.</div>
          )}
        </main>
      </div>

      {authDialogOpen && <AuthDialog onClose={() => setAuthDialogOpen(false)} />}
    </div>
  );
}

function Results({ result }: { result: SubmissionResult }) {
  if (!result.compiled) {
    return (
      <section className="results">
        <h3 className="fail">Compilation failed</h3>
        <pre className="error-output">{result.compileError}</pre>
      </section>
    );
  }
  return (
    <section className="results">
      <h3 className={result.allPassed ? 'pass' : 'fail'}>
        {result.allPassed
          ? `All ${result.results.length} tests passed`
          : `${result.results.filter((r) => r.passed).length} / ${result.results.length} tests passed`}
      </h3>
      <ul className="test-list">
        {result.results.map((r, i) => (
          <li key={i} className={r.passed ? 'pass' : 'fail'}>
            <span className="status">{r.passed ? 'PASS' : r.error ? 'ERROR' : 'FAIL'}</span>
            <code className="input">{r.input}</code>
            {!r.passed && !r.error && (
              <span className="detail">
                expected <code>{r.expected}</code>, got <code>{r.actual}</code>
              </span>
            )}
            {r.error && <span className="detail">{r.error}</span>}
          </li>
        ))}
      </ul>
    </section>
  );
}
