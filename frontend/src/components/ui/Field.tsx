import { forwardRef, useId, type InputHTMLAttributes, type ReactNode, type SelectHTMLAttributes, type TextareaHTMLAttributes } from 'react'

const control =
  'w-full rounded-lg border bg-surface px-3 text-sm text-ink placeholder:text-muted/70 ' +
  'transition-colors focus:outline-none focus:border-accent focus:ring-2 focus:ring-accent/20 ' +
  'disabled:opacity-60'

function borderFor(error?: string) {
  return error ? 'border-miss/70' : 'border-border-strong hover:border-muted/50'
}

interface FieldProps {
  label: string
  error?: string
  hint?: ReactNode
  optional?: boolean
  children: (id: string, describedBy: string | undefined) => ReactNode
}

/** Label, control, hint and error, with the accessibility wiring (ids, aria-describedby) done once. */
export function Field({ label, error, hint, optional, children }: FieldProps) {
  const id = useId()
  const hintId = hint ? `${id}-hint` : undefined
  const errorId = error ? `${id}-error` : undefined
  const describedBy = [hintId, errorId].filter(Boolean).join(' ') || undefined
  return (
    <div className="space-y-1.5">
      <label htmlFor={id} className="flex items-baseline justify-between text-[13px] font-semibold">
        {label}
        {optional && <span className="font-mono text-label text-muted">OPTIONAL</span>}
      </label>
      {children(id, describedBy)}
      {hint && !error && <p id={hintId} className="text-xs text-muted">{hint}</p>}
      {error && <p id={errorId} className="text-xs font-medium text-miss">{error}</p>}
    </div>
  )
}

type InputProps = InputHTMLAttributes<HTMLInputElement> & { error?: string }

export const Input = forwardRef<HTMLInputElement, InputProps>(function Input({ error, className = '', ...rest }, ref) {
  return <input ref={ref} aria-invalid={!!error || undefined} className={`${control} h-10 ${borderFor(error)} ${className}`} {...rest} />
})

type TextareaProps = TextareaHTMLAttributes<HTMLTextAreaElement> & { error?: string }

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaProps>(function Textarea({ error, className = '', ...rest }, ref) {
  return <textarea ref={ref} aria-invalid={!!error || undefined} className={`${control} py-2.5 leading-relaxed ${borderFor(error)} ${className}`} {...rest} />
})

type SelectProps = SelectHTMLAttributes<HTMLSelectElement> & { error?: string }

export const Select = forwardRef<HTMLSelectElement, SelectProps>(function Select({ error, className = '', children, ...rest }, ref) {
  return (
    <select ref={ref} aria-invalid={!!error || undefined} className={`${control} h-10 pr-8 ${borderFor(error)} ${className}`} {...rest}>
      {children}
    </select>
  )
})

/** Toggleable chip used for multi-select filters. */
export function ToggleChip({ checked, onChange, children }: { checked: boolean; onChange: (next: boolean) => void; children: ReactNode }) {
  return (
    <button
      type="button"
      role="checkbox"
      aria-checked={checked}
      onClick={() => onChange(!checked)}
      className={`h-8 rounded-full border px-3 text-[13px] font-medium transition-colors ${
        checked
          ? 'border-accent bg-accent/10 text-accent-text'
          : 'border-border-strong text-muted hover:border-muted/60 hover:text-ink'
      }`}
    >
      {children}
    </button>
  )
}
