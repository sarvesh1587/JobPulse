type Props = {
  label: string
  value: string
  trend?: string
  accentValue?: boolean
}

export default function StatCard({ label, value, trend, accentValue }: Props) {
  return (
    <div className="border border-border-strong rounded-lg p-5 bg-surface-raised">
      <div
        className={`text-[30px] font-extrabold tracking-tight leading-none ${
          accentValue
            ? 'bg-gradient-to-br from-accent-soft to-accent bg-clip-text text-transparent'
            : ''
        }`}
      >
        {value}
      </div>
      <div className="font-mono text-[10.5px] text-charcoal tracking-wide mt-2">
        {label.toUpperCase()}
      </div>
      {trend && <div className="text-[12px] text-good mt-1.5">{trend}</div>}
    </div>
  )
}
