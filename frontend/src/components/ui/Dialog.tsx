import { useEffect, useRef, type ReactNode } from 'react'
import { X } from 'lucide-react'

interface DialogProps {
  open: boolean
  onClose: () => void
  title: string
  description?: ReactNode
  children: ReactNode
  footer?: ReactNode
}

/**
 * Wraps the native <dialog> element, which handles focus trapping, Escape to
 * close and the backdrop, with no extra library.
 */
export function Dialog({ open, onClose, title, description, children, footer }: DialogProps) {
  const ref = useRef<HTMLDialogElement>(null)

  useEffect(() => {
    const dialog = ref.current
    if (!dialog) return
    if (open && !dialog.open) dialog.showModal()
    if (!open && dialog.open) dialog.close()
  }, [open])

  return (
    <dialog
      ref={ref}
      onClose={onClose}
      onClick={(e) => { if (e.target === ref.current) onClose() }}   // click on the backdrop
      className="w-[calc(100%-2rem)] max-w-lg rounded-xl border border-border-strong bg-surface p-0 text-ink shadow-pop"
      aria-labelledby="dialog-title"
    >
      <div className="flex items-start justify-between gap-4 border-b border-border px-5 py-4">
        <div>
          <h2 id="dialog-title" className="text-base font-bold">{title}</h2>
          {description && <p className="mt-0.5 text-sm text-muted">{description}</p>}
        </div>
        <button onClick={onClose} aria-label="Close" className="rounded-md p-1 text-muted hover:bg-raised hover:text-ink">
          <X size={16} />
        </button>
      </div>
      <div className="px-5 py-4">{children}</div>
      {footer && <div className="flex justify-end gap-2 border-t border-border px-5 py-3.5">{footer}</div>}
    </dialog>
  )
}
