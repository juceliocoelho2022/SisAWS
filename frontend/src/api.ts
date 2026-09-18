const API = 'http://localhost:8080/api/v1'

export type Option = { id: number; text: string }
export type Question = {
  id: number
  domain: string
  awsService: string
  difficulty: 'EASY' | 'MEDIUM' | 'HARD' | 'EXAM'
  prompt: string
  options: Option[]
}

export type Dashboard = {
  questionBank: number
  attempts: number
  averageScore: number
  bestScore: number
}

export type ResultItem = {
  questionId: number
  correct: boolean
  explanation: string
  correctOptionIds: number[]
}

export type SimulationResult = {
  attemptId: number
  totalQuestions: number
  correctAnswers: number
  scorePercent: number
  results: ResultItem[]
}

export async function getDashboard(): Promise<Dashboard> {
  const r = await fetch(`${API}/dashboard`)
  if (!r.ok) throw new Error('Backend indisponível')
  return r.json()
}

export async function getQuestions(limit = 10): Promise<Question[]> {
  const r = await fetch(`${API}/questions?certification=SAA-C03&limit=${limit}`)
  if (!r.ok) throw new Error('Não foi possível carregar as questões')
  return r.json()
}

export async function finishSimulation(answers: {questionId: number; selectedOptionIds: number[]}[]): Promise<SimulationResult> {
  const r = await fetch(`${API}/simulations/finish`, {
    method: 'POST',
    headers: {'Content-Type': 'application/json'},
    body: JSON.stringify({ certificationCode: 'SAA-C03', answers })
  })
  if (!r.ok) throw new Error('Não foi possível corrigir o simulado')
  return r.json()
}
