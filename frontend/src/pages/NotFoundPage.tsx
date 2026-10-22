import { useTitle } from '../components/layout/Page'
import { ButtonLink } from '../components/ui/Button'

export default function NotFoundPage() {
  useTitle('Page not found')
  return (
    <div className="mx-auto max-w-md px-4 py-28 text-center">
      <p className="font-mono text-label text-muted">404</p>
      <h1 className="mt-3 text-3xl font-extrabold tracking-tight">This page doesn't exist</h1>
      <p className="mt-2 text-muted">The link may be broken, or the page may have moved.</p>
      <div className="mt-8 flex justify-center gap-2">
        <ButtonLink to="/">Home</ButtonLink>
        <ButtonLink to="/jobs" variant="secondary">Browse jobs</ButtonLink>
      </div>
    </div>
  )
}
