import { mockApplications, STAGES, type AppStatus } from '../data/mockApplications'
import StatusPipeline from '../components/StatusPipeline'

function countByStatus(status: AppStatus) {
  return mockApplications.filter((a) => a.status === status).length
}

export default function Applications() {
  const rejectedCount = mockApplications.filter((a) => a.status === 'Rejected').length

  return (
    <div className="flex-1 overflow-y-auto">
      <div className="max-w-[1000px] mx-auto px-8 py-10">
        <div className="font-mono text-[11px] text-charcoal tracking-wide mb-2">
          APPLICATION TRACKER
        </div>
        <h1 className="text-[28px] font-extrabold tracking-tight mb-1">
          {mockApplications.length} roles in motion.
        </h1>
        <p className="text-[14px] text-charcoal mb-7">
          Every application, one pipeline — from saved to offer.
        </p>

        <div className="flex flex-wrap gap-2 mb-8">
          {STAGES.map((stage) => (
            <div
              key={stage}
              className="font-mono text-[11px] px-3 py-1.5 rounded-full border border-border-strong text-charcoal flex items-center gap-2"
            >
              <span className="font-semibold text-ink">{countByStatus(stage)}</span>
              {stage.toUpperCase()}
            </div>
          ))}
          {rejectedCount > 0 && (
            <div className="font-mono text-[11px] px-3 py-1.5 rounded-full border border-miss text-miss flex items-center gap-2">
              <span className="font-semibold">{rejectedCount}</span>
              REJECTED
            </div>
          )}
        </div>

        <div className="border border-border-strong rounded-lg overflow-hidden">
          <div className="hidden md:grid grid-cols-[1.6fr_1fr_1.4fr_1fr] gap-4 px-5 py-3 border-b border-border font-mono text-[10.5px] text-charcoal tracking-wide bg-surface-raised">
            <div>ROLE</div>
            <div>MATCH / APPLIED</div>
            <div>PIPELINE</div>
            <div>NEXT ACTION</div>
          </div>

          {mockApplications.map((app) => (
            <div
              key={app.id}
              className="grid grid-cols-1 md:grid-cols-[1.6fr_1fr_1.4fr_1fr] gap-3 md:gap-4 px-5 py-4 border-b border-border last:border-b-0 hover:bg-surface-raised transition-colors"
            >
              <div className="min-w-0">
                <div className="font-bold text-[14px] truncate">{app.role}</div>
                <div className="font-mono text-[11.5px] text-charcoal mt-1">{app.company}</div>
              </div>

              <div className="flex md:block items-center gap-3">
                <div className="font-mono text-[15px] font-semibold bg-gradient-to-br from-accent-soft to-accent bg-clip-text text-transparent">
                  {app.match}
                </div>
                <div className="font-mono text-[11px] text-charcoal">
                  {app.appliedDate ?? 'Not applied yet'}
                </div>
              </div>

              <div className="overflow-x-auto">
                <StatusPipeline status={app.status} />
              </div>

              <div className="text-[13px] text-charcoal self-center">{app.nextAction}</div>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
