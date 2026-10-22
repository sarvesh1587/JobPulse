import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Bookmark } from 'lucide-react'
import { useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../../auth/AuthProvider'
import { savedApi } from '../../lib/api'
import { useToast } from '../ui/Toast'
import { errorMessage } from '../ui/Feedback'

interface SaveButtonProps {
  jobId: string
  saved: boolean
  variant?: 'icon' | 'full'
}

/**
 * Save and unsave a job. The icon fills only after the server confirms,
 * so it never shows a state that wasn't stored. Afterwards every cached
 * list showing this job is refetched.
 */
export function SaveButton({ jobId, saved, variant = 'icon' }: SaveButtonProps) {
  const { user } = useAuth()
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const location = useLocation()
  const toast = useToast()

  const mutation = useMutation({
    mutationFn: () => (saved ? savedApi.unsave(jobId) : savedApi.save(jobId)),
    onSuccess: () => {
      toast.success(saved ? 'Removed from saved jobs' : 'Saved. Find it under Saved.')
      queryClient.invalidateQueries({ queryKey: ['jobs'] })
      queryClient.invalidateQueries({ queryKey: ['job', jobId] })
      queryClient.invalidateQueries({ queryKey: ['saved'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
      queryClient.invalidateQueries({ queryKey: ['company'] })
    },
    onError: (e) => toast.error(errorMessage(e)),
  })

  if (user && user.role !== 'CANDIDATE') return null

  const onClick = (e: React.MouseEvent) => {
    e.preventDefault()
    e.stopPropagation()
    if (!user) {
      navigate(`/login?next=${encodeURIComponent(location.pathname + location.search)}`)
      return
    }
    mutation.mutate()
  }

  const label = saved ? 'Remove from saved' : 'Save job'

  if (variant === 'full') {
    return (
      <button
        onClick={onClick}
        disabled={mutation.isPending}
        aria-pressed={saved}
        className={`inline-flex h-10 items-center gap-2 rounded-lg border px-4 text-sm font-semibold transition-colors disabled:opacity-60 ${
          saved ? 'border-accent/50 bg-accent/10 text-accent-text' : 'border-border-strong hover:bg-raised'
        }`}
      >
        <Bookmark size={15} className={saved ? 'fill-current' : ''} />
        {saved ? 'Saved' : 'Save'}
      </button>
    )
  }

  return (
    <button
      onClick={onClick}
      disabled={mutation.isPending}
      aria-pressed={saved}
      aria-label={label}
      title={label}
      className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-lg transition-colors disabled:opacity-60 ${
        saved ? 'text-accent-text hover:bg-accent/10' : 'text-muted hover:bg-raised hover:text-ink'
      }`}
    >
      <Bookmark size={17} className={`transition-transform ${saved ? 'fill-current scale-110' : ''}`} />
    </button>
  )
}
