import { STAGES, type AppStatus } from '../data/mockApplications'

type Props = { status: AppStatus }

export default function StatusPipeline({ status }: Props) {
  if (status === 'Rejected') {
    return (
      <div className="flex items-center gap-2">
        <span className="w-2 h-2 rounded-full bg-miss shrink-0" />
        <span className="font-mono text-[11px] text-miss tracking-wide">REJECTED</span>
      </div>
    )
  }

  const currentIndex = STAGES.indexOf(status)

  return (
    <div className="flex items-center">
      {STAGES.map((stage, i) => {
        const done = i <= currentIndex
        const isLast = i === STAGES.length - 1
        return (
          <div key={stage} className="flex items-center">
            <div className="flex flex-col items-center gap-1.5">
              <span
                className={`w-2 h-2 rounded-full shrink-0 ${
                  done
                    ? 'bg-gradient-to-br from-accent-soft to-accent'
                    : 'bg-border-strong'
                }`}
              />
              <span
                className={`font-mono text-[8.5px] tracking-wide whitespace-nowrap ${
                  i === currentIndex ? 'text-ink font-semibold' : 'text-charcoal'
                }`}
              >
                {stage.slice(0, 4).toUpperCase()}
              </span>
            </div>
            {!isLast && (
              <span
                className={`h-[1.5px] w-6 md:w-8 -mt-3.5 ${
                  i < currentIndex ? 'bg-accent' : 'bg-border-strong'
                }`}
              />
            )}
          </div>
        )
      })}
    </div>
  )
}
