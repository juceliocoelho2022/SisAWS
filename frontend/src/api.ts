const API = 'http://localhost:8080/api/v1'
const TOKEN_KEY = 'sisaws-token'

export type AuthUser = {
  id: number
  name: string
  email: string
  role: string
}

export type AuthResponse = {
  token: string
  user: AuthUser
}

export type Option = { id: number; text: string }

export type Difficulty = 'EASY' | 'MEDIUM' | 'HARD' | 'EXAM'

export type Question = {
  id: number
  domain: string
  awsService: string
  difficulty: Difficulty
  prompt: string
  options: Option[]
}

export type Dashboard = {
  questionBank: number
  attempts: number
  averageScore: number
  bestScore: number
  errorNotebookCount: number
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

export type ErrorNotebookEntry = {
  id: number
  questionId: number
  awsService: string
  domain: string
  prompt: string
  explanation: string
  wrongCount: number
  lastWrongAt: string
}

export type ServiceProgress = {
  awsService: string
  answered: number
  correctAnswers: number
  accuracyPercent: number
  status: 'STARTING' | 'REVIEW' | 'GOOD' | 'STRONG'
  lastAnsweredAt: string
}

export type Recommendation = {
  awsService: string
  title: string
  reason: string
  recommendedAction: string
  priority: string
}

export type StudyTrail = {
  id: string
  title: string
  description: string
  services: string[]
  level: string
  estimatedMinutes: number
}

export type Flashcard = {
  id: number
  awsService: string
  question: string
  answer: string
}

export type HistoryAttempt = {
  id: number
  certificationCode: string
  totalQuestions: number
  correctAnswers: number
  scorePercent: number
  finishedAt: string
}

export function getStoredToken() {
  return window.localStorage.getItem(TOKEN_KEY)
}

export function clearSession() {
  window.localStorage.removeItem(TOKEN_KEY)
}

function saveToken(token: string) {
  window.localStorage.setItem(TOKEN_KEY, token)
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const token = getStoredToken()
  const headers = new Headers(init.headers)

  if (init.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  const response = await fetch(`${API}${path}`, {...init, headers})

  if (!response.ok) {
    let message = 'Não foi possível concluir a operação.'

    try {
      const body = await response.json()
      message = body.detail || body.message || body.error || message
    } catch {
      // Resposta sem JSON.
    }

    throw new Error(message)
  }

  if (response.status === 204) {
    return undefined as T
  }

  return response.json()
}

export async function login(email: string, password: string): Promise<AuthResponse> {
  const response = await request<AuthResponse>('/auth/login', {
    method: 'POST',
    body: JSON.stringify({email, password})
  })
  saveToken(response.token)
  return response
}

export async function register(name: string, email: string, password: string): Promise<AuthResponse> {
  const response = await request<AuthResponse>('/auth/register', {
    method: 'POST',
    body: JSON.stringify({name, email, password})
  })
  saveToken(response.token)
  return response
}

export function getMe(): Promise<AuthUser> {
  return request<AuthUser>('/auth/me')
}

export function getDashboard(): Promise<Dashboard> {
  return request<Dashboard>('/dashboard')
}

export function getQuestions(limit = 10, service = '', difficulty = ''): Promise<Question[]> {
  const params = new URLSearchParams({
    certification: 'SAA-C03',
    limit: String(limit)
  })

  if (service) params.set('service', service)
  if (difficulty) params.set('difficulty', difficulty)

  return request<Question[]>(`/questions?${params.toString()}`)
}

export function getErrorNotebook(): Promise<ErrorNotebookEntry[]> {
  return request<ErrorNotebookEntry[]>('/error-notebook')
}

export function getLearningProgress(): Promise<ServiceProgress[]> {
  return request<ServiceProgress[]>('/learning/progress')
}

export function getRecommendation(): Promise<Recommendation> {
  return request<Recommendation>('/learning/recommendation')
}

export function getTrails(): Promise<StudyTrail[]> {
  return request<StudyTrail[]>('/learning/trails')
}

export function getFlashcards(service = ''): Promise<Flashcard[]> {
  const suffix = service ? `?service=${encodeURIComponent(service)}` : ''
  return request<Flashcard[]>(`/learning/flashcards${suffix}`)
}

export function getHistory(): Promise<HistoryAttempt[]> {
  return request<HistoryAttempt[]>('/simulations/history')
}

export function finishSimulation(
  answers: {questionId: number; selectedOptionIds: number[]}[]
): Promise<SimulationResult> {
  return request<SimulationResult>('/simulations/finish', {
    method: 'POST',
    body: JSON.stringify({certificationCode: 'SAA-C03', answers})
  })
}
