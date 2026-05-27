import { useEffect, useState } from 'react';
import Editor from '@monaco-editor/react';
import {
  adminCreateExercise,
  adminDeleteExercise,
  adminListExercises,
  adminUpdateExercise,
} from '../api';
import type { AdminExercise, TestCase } from '../types';

const LANGUAGES = ['java', 'javascript', 'python'] as const;
type Lang = (typeof LANGUAGES)[number];

function monacoLanguage(lang: string): string {
  if (lang === 'java' || lang === 'javascript' || lang === 'python') return lang;
  return 'plaintext';
}

function emptyExercise(): AdminExercise {
  return {
    id: '',
    language: 'java',
    difficulty: 1,
    title: '',
    description: '',
    hint: '',
    buggyCode: '',
    solutionCode: '',
    testHarness: '',
    tests: [],
  };
}

interface Props {
  onClose: () => void;
}

export function AdminPanel({ onClose }: Props) {
  const [list, setList] = useState<AdminExercise[]>([]);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [editing, setEditing] = useState<AdminExercise | null>(null);
  const [isNew, setIsNew] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [loaded, setLoaded] = useState(false);

  async function refresh() {
    try {
      const items = await adminListExercises();
      setList(items);
      setLoaded(true);
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    }
  }

  useEffect(() => {
    refresh();
  }, []);

  function startEdit(ex: AdminExercise) {
    setSelectedId(ex.id);
    setEditing({ ...ex, tests: [...ex.tests] });
    setIsNew(false);
    setError(null);
  }

  function startNew() {
    setSelectedId(null);
    setEditing(emptyExercise());
    setIsNew(true);
    setError(null);
  }

  async function save() {
    if (!editing) return;
    setBusy(true);
    setError(null);
    try {
      const saved = isNew
        ? await adminCreateExercise(editing)
        : await adminUpdateExercise(editing.id, editing);
      await refresh();
      setSelectedId(saved.id);
      setEditing({ ...saved, tests: [...saved.tests] });
      setIsNew(false);
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setBusy(false);
    }
  }

  async function remove() {
    if (!editing || isNew) return;
    if (!confirm(`Delete exercise "${editing.id}"? This cannot be undone.`)) return;
    setBusy(true);
    setError(null);
    try {
      await adminDeleteExercise(editing.id);
      await refresh();
      setSelectedId(null);
      setEditing(null);
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setBusy(false);
    }
  }

  function updateField<K extends keyof AdminExercise>(key: K, value: AdminExercise[K]) {
    if (!editing) return;
    setEditing({ ...editing, [key]: value });
  }

  function updateTest(idx: number, patch: Partial<TestCase>) {
    if (!editing) return;
    const next = editing.tests.map((t, i) => (i === idx ? { ...t, ...patch } : t));
    setEditing({ ...editing, tests: next });
  }

  function addTest() {
    if (!editing) return;
    setEditing({ ...editing, tests: [...editing.tests, { input: '', expected: '' }] });
  }

  function removeTest(idx: number) {
    if (!editing) return;
    setEditing({ ...editing, tests: editing.tests.filter((_, i) => i !== idx) });
  }

  return (
    <div className="admin-panel">
      <div className="admin-header">
        <h2>Exercise admin</h2>
        <div className="admin-header-actions">
          <button onClick={startNew}>New exercise</button>
          <button className="secondary" onClick={onClose}>
            Back to practice
          </button>
        </div>
      </div>

      <div className="admin-layout">
        <aside className="admin-sidebar">
          <h3>Exercises ({list.length})</h3>
          {!loaded && <div className="empty">Loading…</div>}
          <ul>
            {list.map((ex) => (
              <li
                key={ex.id}
                className={selectedId === ex.id ? 'active' : ''}
                onClick={() => startEdit(ex)}
              >
                <div className="ex-title">{ex.title || ex.id}</div>
                <div className="ex-meta">
                  {ex.language} · difficulty {ex.difficulty}
                </div>
              </li>
            ))}
          </ul>
        </aside>

        <section className="admin-main">
          {!editing && <div className="empty">Pick an exercise or click "New exercise".</div>}
          {editing && (
            <>
              <div className="admin-form-grid">
                <label>
                  ID
                  <input
                    type="text"
                    value={editing.id}
                    onChange={(e) => updateField('id', e.target.value)}
                    disabled={!isNew}
                    placeholder="e.g. java-binary-search"
                  />
                </label>
                <label>
                  Language
                  <select
                    value={editing.language}
                    onChange={(e) => updateField('language', e.target.value as Lang)}
                  >
                    {LANGUAGES.map((l) => (
                      <option key={l} value={l}>
                        {l}
                      </option>
                    ))}
                  </select>
                </label>
                <label>
                  Difficulty
                  <input
                    type="number"
                    min={1}
                    max={5}
                    value={editing.difficulty}
                    onChange={(e) =>
                      updateField('difficulty', parseInt(e.target.value, 10) || 1)
                    }
                  />
                </label>
                <label className="full">
                  Title
                  <input
                    type="text"
                    value={editing.title}
                    onChange={(e) => updateField('title', e.target.value)}
                  />
                </label>
                <label className="full">
                  Description
                  <textarea
                    rows={3}
                    value={editing.description}
                    onChange={(e) => updateField('description', e.target.value)}
                  />
                </label>
                <label className="full">
                  Hint
                  <textarea
                    rows={2}
                    value={editing.hint ?? ''}
                    onChange={(e) => updateField('hint', e.target.value)}
                  />
                </label>
              </div>

              <CodeField
                label="Buggy code (shown to the user)"
                language={editing.language}
                value={editing.buggyCode}
                onChange={(v) => updateField('buggyCode', v)}
              />
              <CodeField
                label="Solution code (hidden from users)"
                language={editing.language}
                value={editing.solutionCode}
                onChange={(v) => updateField('solutionCode', v)}
              />
              <CodeField
                label="Test harness (runner — emits PASS/FAIL/ERROR lines)"
                language={editing.language}
                value={editing.testHarness}
                onChange={(v) => updateField('testHarness', v)}
              />

              <div className="admin-tests">
                <div className="admin-tests-header">
                  <strong>Test cases (informational only)</strong>
                  <button className="secondary" onClick={addTest}>
                    Add row
                  </button>
                </div>
                {editing.tests.length === 0 && (
                  <div className="empty">No rows. The harness file above is what actually runs.</div>
                )}
                {editing.tests.map((t, i) => (
                  <div key={i} className="admin-test-row">
                    <input
                      type="text"
                      placeholder="input"
                      value={t.input}
                      onChange={(e) => updateTest(i, { input: e.target.value })}
                    />
                    <input
                      type="text"
                      placeholder="expected"
                      value={t.expected}
                      onChange={(e) => updateTest(i, { expected: e.target.value })}
                    />
                    <button className="secondary" onClick={() => removeTest(i)}>
                      Remove
                    </button>
                  </div>
                ))}
              </div>

              {error && <div className="error">{error}</div>}

              <div className="admin-actions">
                <button onClick={save} disabled={busy}>
                  {busy ? 'Saving…' : isNew ? 'Create' : 'Save changes'}
                </button>
                {!isNew && (
                  <button className="secondary danger" onClick={remove} disabled={busy}>
                    Delete
                  </button>
                )}
              </div>
            </>
          )}
        </section>
      </div>
    </div>
  );
}

interface CodeFieldProps {
  label: string;
  language: string;
  value: string;
  onChange: (v: string) => void;
}

function CodeField({ label, language, value, onChange }: CodeFieldProps) {
  return (
    <section className="admin-code-field">
      <label>{label}</label>
      <div className="editor">
        <Editor
          height="220px"
          language={monacoLanguage(language)}
          value={value}
          onChange={(v) => onChange(v ?? '')}
          theme="vs-dark"
          options={{ fontSize: 13, minimap: { enabled: false } }}
        />
      </div>
    </section>
  );
}
