export type SkillStatus = {
  name: string
  ready: boolean
  level: number // 0-100, illustrative strength against the target role
}

export type SkillGapItem = {
  name: string
  priority: number
  effortEstimate: string // a suggested range, never a guarantee
  resources: string[]
  projectIdea: string
}

export const targetRole = 'Java Backend Developer'

// Demo data — illustrative only. Real effort varies a lot by background;
// treat these as a starting point, not a promise.
export const skillStatuses: SkillStatus[] = [
  { name: 'Java', ready: true, level: 92 },
  { name: 'Spring Boot', ready: true, level: 88 },
  { name: 'MySQL', ready: true, level: 80 },
  { name: 'Docker', ready: false, level: 30 },
  { name: 'Kafka', ready: false, level: 12 },
  { name: 'Redis', ready: false, level: 15 },
]

export const skillGapPlan: SkillGapItem[] = [
  {
    name: 'Docker',
    priority: 1,
    effortEstimate: '8–12 hrs (suggested estimate)',
    resources: [
      "Docker's official \"Get Started\" guide",
      'freeCodeCamp — Docker for beginners (YouTube)',
      'Play with Docker — browser sandbox, no install needed',
    ],
    projectIdea:
      'Containerize an existing Spring Boot + MySQL project with a multi-stage Dockerfile, then wire it up with docker-compose so the API and database run together with one command.',
  },
  {
    name: 'Kafka',
    priority: 2,
    effortEstimate: '12–18 hrs (suggested estimate)',
    resources: [
      'Apache Kafka official quickstart docs',
      'Confluent Developer — free Kafka fundamentals course',
      'Spring for Apache Kafka reference documentation',
    ],
    projectIdea:
      'Build a small order-processing pipeline: a producer service publishes order events, a separate consumer service processes them asynchronously using Spring Kafka.',
  },
  {
    name: 'Redis',
    priority: 3,
    effortEstimate: '5–8 hrs (suggested estimate)',
    resources: [
      'Redis University — free official courses',
      'Try Redis — interactive in-browser tutorial',
      'Spring Data Redis reference documentation',
    ],
    projectIdea:
      'Add a Redis caching layer in front of a frequently-read REST endpoint (e.g. product listing) and measure the response-time difference with and without the cache.',
  },
]
