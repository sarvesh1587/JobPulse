import { Link } from 'react-router-dom'
import { Logo } from './Logo'

export function SiteFooter() {
  return (
    <footer className="mt-24 border-t border-border">
      <div className="mx-auto grid max-w-page gap-10 px-4 pb-6 pt-12 sm:px-6 md:grid-cols-[1.4fr_1fr_1fr]">
        <div>
          <Logo />
          <p className="mt-3 max-w-[38ch] text-sm text-muted text-pretty">
            Job discovery for students and early-career developers. Every match score comes with the reasons behind it.
          </p>
        </div>
        <div>
          <h2 className="font-mono text-label text-muted">CANDIDATES</h2>
          <ul className="mt-3 space-y-2 text-sm">
            <li><Link className="hover:text-accent-text" to="/jobs">Browse jobs</Link></li>
            <li><Link className="hover:text-accent-text" to="/companies">Companies</Link></li>
            <li><Link className="hover:text-accent-text" to="/register">Create a profile</Link></li>
          </ul>
        </div>
        <div>
          <h2 className="font-mono text-label text-muted">EMPLOYERS</h2>
          <ul className="mt-3 space-y-2 text-sm">
            <li><Link className="hover:text-accent-text" to="/register?role=EMPLOYER">Post a job</Link></li>
            <li><a className="hover:text-accent-text" href="https://github.com/Sarvesh1587/jobpulse" target="_blank" rel="noreferrer">Source on GitHub</a></li>
          </ul>
        </div>
      </div>
      <div aria-hidden className="mx-auto max-w-page select-none overflow-hidden px-4 sm:px-6">
        <div className="translate-y-[18%] text-[clamp(64px,17vw,220px)] font-extrabold leading-none tracking-[-0.06em] text-ink/[0.06]">
          JOBPULSE
        </div>
      </div>
      <div className="border-t border-border">
        <div className="mx-auto flex max-w-page flex-wrap justify-between gap-2 px-4 py-4 font-mono text-label text-muted sm:px-6">
          <span>© {new Date().getFullYear()} JOBPULSE</span>
          <span>INGESTED LISTINGS LINK TO EACH COMPANY'S OWN CAREERS PAGE</span>
        </div>
      </div>
    </footer>
  )
}
