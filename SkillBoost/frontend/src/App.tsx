import { useEffect, useState } from 'react';
import Editor from '@monaco-editor/react';
import { fetchExercises, submit } from './api';
import type { Exercise, SubmissionResult } from './types';

export default function App() {
  const [exercises, setExercises] = useState<Exercise[]>([]);
  const [selected, setSelected] = useState<Exercise | null>(null);
  const [code, setCode] = useState<string>('');
  const [result, setResult] = useState<SubmissionResult | null>(null);
  const [running, setRunning] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(null);

  useEffect(() => {
    fetchExercises()
      .then((list) => {
        setExercises(list);
        if (list.length > 0) selectExercise(list[0]);
      })
      .catch((e) => setLoadError(String(e)));
  }, []);

  function selectExercise(ex: Exercise) {
    setSelected(ex);
    setCode(ex.buggyCode);
    setResult(null);
  }

  async function runTests() {
    if (!selected) return;
    setRunning(true);
    setResult(null);
    try {
      const r = await submit(selected.id, code);
      setResult(r);
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

  return (
    <div className="app">
      <header className="header">
        <h1>SkillBoost</h1>
        <span className="tagline">Find the bug. Make the tests pass.</span>
      </header>

      <div className="layout">
        <aside className="sidebar">
          <h2>Exercises</h2>
          {loadError && <div className="error">{loadError}</div>}
          <ul>
            {exercises.map((ex) => (
              <li
                key={ex.id}
                className={selected?.id === ex.id ? 'active' : ''}
                onClick={() => selectExercise(ex)}
              >
                <div className="ex-title">{ex.title}</div>
                <div className="ex-meta">
                  {ex.language} · difficulty {ex.difficulty}
                </div>
              </li>
            ))}
          </ul>
        </aside>

        <main className="main">
          {selected ? (
            <>
              <section className="problem">
                <h2>{selected.title}</h2>
                <p>{selected.description}</p>
              </section>

              <section className="editor">
                <Editor
                  height="380px"
                  defaultLanguage="java"
                  language={selected.language === 'java' ? 'java' : 'plaintext'}
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
              </section>

              {result && <Results result={result} />}
            </>
          ) : (
            <div className="empty">Select an exercise to begin.</div>
          )}
        </main>
      </div>
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
