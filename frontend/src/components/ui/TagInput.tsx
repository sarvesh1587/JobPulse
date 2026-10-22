import { useEffect, useId, useState, type KeyboardEvent } from 'react'
import { X } from 'lucide-react'
import { useQuery } from '@tanstack/react-query'
import { jobsApi } from '../../lib/api'

interface TagInputProps {
  value: string[]
  onChange: (next: string[]) => void
  placeholder?: string
  max?: number
  /** Suggest names from the skill catalogue as the user types. */
  suggestSkills?: boolean
  ariaLabel: string
}

function useDebounced<T>(value: T, ms: number): T {
  const [debounced, setDebounced] = useState(value)
  useEffect(() => {
    const t = window.setTimeout(() => setDebounced(value), ms)
    return () => window.clearTimeout(t)
  }, [value, ms])
  return debounced
}

/** Type and press Enter or comma to add. Backspace on an empty input removes the last tag. */
export function TagInput({ value, onChange, placeholder, max = 50, suggestSkills, ariaLabel }: TagInputProps) {
  const [draft, setDraft] = useState('')
  const listId = useId()
  const query = useDebounced(draft.trim(), 200)

  const suggestions = useQuery({
    queryKey: ['skill-suggest', query],
    queryFn: () => jobsApi.skillSuggestions(query),
    enabled: !!suggestSkills && query.length >= 1,
    staleTime: 60_000,
  })

  const add = (raw: string) => {
    const tag = raw.trim()
    if (!tag || value.length >= max) return
    if (value.some((v) => v.toLowerCase() === tag.toLowerCase())) { setDraft(''); return }
    onChange([...value, tag])
    setDraft('')
  }

  const onKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter' || e.key === ',') {
      e.preventDefault()
      add(draft)
    } else if (e.key === 'Backspace' && !draft && value.length) {
      onChange(value.slice(0, -1))
    }
  }

  const options = (suggestions.data ?? []).filter((s) => !value.some((v) => v.toLowerCase() === s.toLowerCase()))

  return (
    <div className="rounded-lg border border-border-strong bg-surface p-1.5 transition-colors focus-within:border-accent focus-within:ring-2 focus-within:ring-accent/20">
      <ul className="flex flex-wrap gap-1.5" aria-label={ariaLabel}>
        {value.map((tag) => (
          <li key={tag} className="inline-flex h-7 items-center gap-1 rounded-md bg-raised pl-2.5 pr-1 text-[13px] font-medium">
            {tag}
            <button type="button" onClick={() => onChange(value.filter((t) => t !== tag))}
              aria-label={`Remove ${tag}`} className="rounded p-0.5 text-muted hover:bg-border hover:text-ink">
              <X size={12} />
            </button>
          </li>
        ))}
        <li className="min-w-[10rem] flex-1">
          <input
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
            onKeyDown={onKeyDown}
            onBlur={() => add(draft)}
            placeholder={value.length >= max ? `Limit of ${max} reached` : placeholder}
            disabled={value.length >= max}
            list={suggestSkills ? listId : undefined}
            aria-label={ariaLabel}
            className="h-7 w-full bg-transparent px-1.5 text-sm outline-none placeholder:text-muted/70"
          />
          {suggestSkills && (
            <datalist id={listId}>
              {options.map((o) => <option key={o} value={o} />)}
            </datalist>
          )}
        </li>
      </ul>
    </div>
  )
}
