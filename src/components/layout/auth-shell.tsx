import type { ReactNode } from 'react';
import { BrainCircuit } from 'lucide-react';
import { Link } from 'react-router-dom';

export function AuthShell({ children }: { children: ReactNode }) {
  return (
    <main className="auth-shell">
      <aside className="auth-visual" aria-label="Refine 智能错题">
        <Link to="/" className="auth-brand">
          <BrainCircuit className="size-5" />
          <span>Refine</span>
        </Link>
        <div className="auth-statement">
          <p>LEARN FROM EVERY MISTAKE</p>
          <h1>
            把不会的，
            <br />
            变成会的。
          </h1>
        </div>
        <div className="formula-board" aria-hidden="true">
          <span>01</span>
          <strong>y = ax² + bx + c</strong>
          <span>02</span>
          <strong>∫ f(x) dx</strong>
          <span>03</span>
          <strong>F = ma</strong>
        </div>
      </aside>
      <section className="auth-form-side">
        <div className="auth-mobile-brand">
          <BrainCircuit className="size-5" /> Refine
        </div>
        <div className="auth-form-wrap">{children}</div>
      </section>
    </main>
  );
}
