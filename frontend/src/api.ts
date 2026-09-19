const API = (import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api/v1').replace(/\/$/, '')
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

export type AnswerCheck = {
  correct: boolean
  explanation: string
  correctOptionIds: number[]
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

export type AdaptiveFocusService = {
  awsService: string
  answered: number
  wrongCount: number
  accuracyPercent: number
  priorityScore: number
  reason: string
}

export type AdaptiveNextAction = {
  awsService: string
  difficulty: Difficulty
  questionCount: number
  mode: 'study' | 'exam'
  reason: string
}

export type AdaptivePlan = {
  generatedAt: string
  overallLevel: 'FOUNDATION' | 'DEVELOPING' | 'CONSOLIDATING' | 'STRONG'
  focusServices: AdaptiveFocusService[]
  nextAction: AdaptiveNextAction
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

export type StudyMaterial = {
  id: number
  title: string
  author: string
  materialType: 'PDF' | 'DOCX' | 'EPUB'
  licenseType: 'OWNED' | 'LICENSED' | 'PUBLIC_DOMAIN' | 'OPEN_LICENSE' | 'INTERNAL'
  awsService: string
  saaDomain: string
  estimatedMinutes: number
  fileSize: number
  chapterCount: number
  completedChapters: number
  progressPercent: number
}

export type StudyChapter = {
  id: number
  chapterNumber: number
  title: string
  description?: string
  awsService: string
  saaDomain: string
  estimatedMinutes: number
  completed: boolean
}

export type MaterialAiFlashcard = {
  id: number
  question: string
  answer: string
}

export type MaterialAiOverview = {
  processed: boolean
  bedrockEnabled: boolean
  generationMode: string
  chunkCount: number
  summary?: string
  keyPoints?: string
  flashcards: MaterialAiFlashcard[]
  processedAt?: string
}

export type MaterialAiAnswer = {
  answer: string
  generatedByBedrock: boolean
  generationMode: string
  retrievalMode: string
  sources: {chunkNumber: number; excerpt: string}[]
}

export type StudyMaterialDetail = {
  material: StudyMaterial
  description?: string
  publisher?: string
  edition?: string
  publicationYear?: number
  isbn?: string
  sourceUrl?: string
  chapters: StudyChapter[]
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

  if (init.body && !(init.body instanceof FormData) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  const response = await fetch(`${API}${path}`, {...init, headers})

  const publicAuthPath = [
    '/auth/login',
    '/auth/register',
    '/auth/forgot-password',
    '/auth/reset-password'
  ].some(publicPath => path.startsWith(publicPath))

  if (response.status === 401) {
    if (path.startsWith('/auth/login')) {
      throw new Error('E-mail ou senha inválidos.')
    }

    if (!publicAuthPath) {
      clearSession()
      window.dispatchEvent(new Event('sisaws:unauthorized'))
      throw new Error('Sua sessão expirou. Entre novamente.')
    }
  }

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

export function requestPasswordReset(email: string): Promise<{message: string}> {
  return request<{message: string}>('/auth/forgot-password', {
    method: 'POST',
    body: JSON.stringify({email})
  })
}

export function resetPassword(token: string, newPassword: string): Promise<void> {
  return request<void>('/auth/reset-password', {
    method: 'POST',
    body: JSON.stringify({token, newPassword})
  })
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

export function checkAnswer(questionId: number, selectedOptionIds: number[]): Promise<AnswerCheck> {
  return request<AnswerCheck>(`/questions/${questionId}/check`, {
    method: 'POST',
    body: JSON.stringify({selectedOptionIds})
  })
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

export function getAdaptivePlan(): Promise<AdaptivePlan> {
  return request<AdaptivePlan>('/learning/adaptive/plan')
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


export function getStudyMaterials(): Promise<StudyMaterial[]> {
  return request<StudyMaterial[]>('/materials')
}

export function getStudyMaterial(id: number): Promise<StudyMaterialDetail> {
  return request<StudyMaterialDetail>(`/materials/${id}`)
}

export function getStudyMaterialAccess(id: number): Promise<{url: string; expiresInSeconds: number}> {
  return request<{url: string; expiresInSeconds: number}>(`/materials/${id}/access`)
}

export function completeStudyChapter(materialId: number, chapterId: number): Promise<StudyMaterialDetail> {
  return request<StudyMaterialDetail>(`/materials/${materialId}/chapters/${chapterId}/complete`, {
    method: 'POST'
  })
}

export function uploadStudyMaterial(form: FormData): Promise<StudyMaterialDetail> {
  return request<StudyMaterialDetail>('/materials/upload', {
    method: 'POST',
    body: form
  })
}


export function getMaterialAi(id: number): Promise<MaterialAiOverview> {
  return request<MaterialAiOverview>(`/materials/${id}/ai`)
}

export function processMaterialAi(id: number): Promise<MaterialAiOverview> {
  return request<MaterialAiOverview>(`/materials/${id}/ai/process`, {method: 'POST'})
}

export function askMaterialAi(id: number, question: string): Promise<MaterialAiAnswer> {
  return request<MaterialAiAnswer>(`/materials/${id}/ai/ask`, {
    method: 'POST',
    body: JSON.stringify({question})
  })
}
