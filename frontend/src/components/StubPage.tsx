type Props = { title: string; note: string }

export default function StubPage({ title, note }: Props) {
  return (
    <div className="flex-1 flex items-center justify-center p-10">
      <div className="text-center max-w-[38ch]">
        <div className="font-mono text-[11px] text-charcoal tracking-wide mb-3">
          NOT BUILT YET
        </div>
        <h1 className="text-[26px] font-extrabold tracking-tight mb-2">{title}</h1>
        <p className="text-[14px] text-charcoal leading-relaxed">{note}</p>
      </div>
    </div>
  )
}
