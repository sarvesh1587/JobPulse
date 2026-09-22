type Props = {
  score: number
  size?: 'sm' | 'lg'
}

function label(score: number) {
  if (score >= 85) return 'Strong target'
  if (score >= 65) return 'Worth a look'
  return 'Weak fit'
}

export default function MatchScore({ score, size = 'sm' }: Props) {
  const big = size === 'lg'
  return (
    <div className="text-right leading-none">
      <div
        className={`font-mono font-semibold bg-gradient-to-br from-accent-soft to-accent bg-clip-text text-transparent ${
          big ? 'text-[40px]' : 'text-[20px]'
        }`}
      >
        {score}
      </div>
      <div className={`font-mono text-charcoal ${big ? 'text-[11px] mt-1' : 'text-[9px]'} tracking-wide`}>
        {big ? label(score).toUpperCase() : 'MATCH'}
      </div>
    </div>
  )
}
