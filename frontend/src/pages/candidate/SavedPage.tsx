import { useSearchParams } from 'react-router-dom'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { Bookmark } from 'lucide-react'
import { savedApi } from '../../lib/api'
import { Page } from '../../components/layout/Page'
import { JobCard } from '../../components/jobs/JobCard'
import { ButtonLink } from '../../components/ui/Button'
import { EmptyState, ErrorState, ListSkeleton } from '../../components/ui/Feedback'
import { Pagination } from '../../components/ui/Pagination'

export default function SavedPage() {
  const [params, setParams] = useSearchParams()
  const page = Math.max(0, Number(params.get('page') ?? 0) || 0)
  const saved = useQuery({ queryKey: ['saved', page], queryFn: () => savedApi.list(page), placeholderData: keepPreviousData })

  return (
    <Page title="Saved jobs" eyebrow="SAVED" description="Roles you bookmarked, newest first. They're stored with your account.">
      {saved.isLoading && <ListSkeleton rows={4} className="h-[150px]" />}
      {saved.isError && <ErrorState error={saved.error} onRetry={() => saved.refetch()} />}
      {saved.data && saved.data.content.length === 0 && (
        <EmptyState icon={<Bookmark size={18} />} title="No saved jobs yet" action={<ButtonLink to="/jobs">Browse jobs</ButtonLink>}>
          Tap the bookmark on any job to save it here.
        </EmptyState>
      )}
      {saved.data && saved.data.content.length > 0 && (
        <div className="space-y-3">
          {saved.data.content.map((job, i) => <JobCard key={job.id} job={job} index={i} />)}
          <Pagination page={saved.data.page} totalPages={saved.data.totalPages}
            onChange={(p) => setParams(p ? { page: String(p) } : {})} />
        </div>
      )}
    </Page>
  )
}
