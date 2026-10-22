import { createContext, useCallback, useContext, useMemo, useRef, useState, type ReactNode } from 'react'
import { CheckCircle2, AlertTriangle, X } from 'lucide-react'

type Tone = 'success' | 'error'
interface Toast { id: number; tone: Tone; message: string }

interface ToastApi {
  success: (message: string) => void
  error: (message: string) => void
}

const ToastContext = createContext<ToastApi | null>(null)

/**
 * Short confirmations ("Saved", "Application sent") shown after a mutation
 * actually succeeds. The live region announces them to screen readers.
 */
export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([])
  const nextId = useRef(1)

  const dismiss = useCallback((id: number) => setToasts((t) => t.filter((x) => x.id !== id)), [])

  const push = useCallback((tone: Tone, message: string) => {
    const id = nextId.current++
    setToasts((t) => [...t.slice(-2), { id, tone, message }])
    window.setTimeout(() => dismiss(id), tone === 'error' ? 6000 : 3500)
  }, [dismiss])

  const api = useMemo<ToastApi>(() => ({
    success: (m) => push('success', m),
    error: (m) => push('error', m),
  }), [push])

  return (
    <ToastContext.Provider value={api}>
      {children}
      <div aria-live="polite" className="pointer-events-none fixed inset-x-0 bottom-4 z-[60] flex flex-col items-center gap-2 px-4 sm:items-end sm:pr-6">
        {toasts.map((t) => (
          <div
            key={t.id}
            className="pointer-events-auto flex w-full max-w-sm items-start gap-2.5 rounded-lg border border-border-strong bg-surface px-3.5 py-3 text-sm shadow-pop"
            style={{ animation: 'toast-in 220ms cubic-bezier(0.2,0.7,0.2,1)' }}
          >
            {t.tone === 'success'
              ? <CheckCircle2 size={16} className="mt-0.5 shrink-0 text-good" />
              : <AlertTriangle size={16} className="mt-0.5 shrink-0 text-miss" />}
            <span className="flex-1">{t.message}</span>
            <button onClick={() => dismiss(t.id)} aria-label="Dismiss" className="text-muted hover:text-ink">
              <X size={14} />
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}

export function useToast(): ToastApi {
  const ctx = useContext(ToastContext)
  if (!ctx) throw new Error('useToast must be used inside ToastProvider')
  return ctx
}
