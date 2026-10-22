import { useEffect, type ReactNode } from 'react'

/** Sets document.title, so each route reads correctly in tabs, history and screen readers. */
export function useTitle(title: string) {
  useEffect(() => {
    document.title = title ? `${title} · JobPulse` : 'JobPulse'
  }, [title])
}

interface PageProps {
  title: string
  eyebrow?: string
  description?: ReactNode
  actions?: ReactNode
  children: ReactNode
  width?: 'page' | 'narrow'
}

/** Standard page frame: eyebrow, heading, optional actions, content. */
export function Page({ title, eyebrow, description, actions, children, width = 'page' }: PageProps) {
  useTitle(title)
  return (
    <div className={`mx-auto px-4 pb-10 pt-8 sm:px-6 sm:pt-10 ${width === 'narrow' ? 'max-w-3xl' : 'max-w-page'}`}>
      <div className="mb-8 flex flex-wrap items-end justify-between gap-4">
        <div className="min-w-0">
          {eyebrow && <p className="mb-2 font-mono text-label text-muted">{eyebrow}</p>}
          <h1 className="text-[26px] font-extrabold leading-tight tracking-tight sm:text-[30px] text-balance">{title}</h1>
          {description && <p className="mt-1.5 max-w-[62ch] text-[15px] text-muted text-pretty">{description}</p>}
        </div>
        {actions && <div className="flex flex-wrap gap-2">{actions}</div>}
      </div>
      {children}
    </div>
  )
}

/** A titled section used inside pages. Plain borders, no card-in-card nesting. */
export function Section({ title, action, children, className = '' }: { title: string; action?: ReactNode; children: ReactNode; className?: string }) {
  return (
    <section className={className}>
      <div className="mb-3 flex items-center justify-between gap-3">
        <h2 className="text-[15px] font-bold tracking-tight">{title}</h2>
        {action}
      </div>
      {children}
    </section>
  )
}
