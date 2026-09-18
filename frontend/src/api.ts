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

export function getQuestions(limit = 10): Promise<Question[]> {
  return request<Question[]>(`/questions?certification=SAA-C03&limit=${limit}`)
}

export function getErrorNotebook(): Promise<ErrorNotebookEntry[]> {
  return request<ErrorNotebookEntry[]>('/error-notebook')
}

export function finishSimulation(
  answers: {questionId: number; selectedOptionIds: number[]}[]
): Promise<SimulationResult> {
  return request<SimulationResult>('/simulations/finish', {
    method: 'POST',
    body: JSON.stringify({certificationCode: 'SAA-C03', answers})
  })
}
