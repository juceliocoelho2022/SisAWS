import { FormEvent, useEffect, useMemo, useState } from 'react'
import {
  Award,
  BarChart3,
  BookOpen,
  BookOpenCheck,
  Brain,
  Cloud,
  CheckCircle2,
  Clock3,
  Flag,
  Gauge,
  GraduationCap,
  History,
  Lightbulb,
  LibraryBig,
  FileText,
  ExternalLink,
  Upload,
  LogOut,
  NotebookPen,
  PlayCircle,
  ShieldCheck,
  SlidersHorizontal,
  Sparkles,
  Target,
  UserCircle,
  XCircle
} from 'lucide-react'
import {
  AdaptivePlan,
  AnswerCheck,
  AuthUser,
  Dashboard,
  Difficulty,
  ErrorNotebookEntry,
  Flashcard,
  HistoryAttempt,
  Question,
  Recommendation,
  ServiceProgress,
  SimulationResult,
  StudyTrail,
  StudyMaterial,
  StudyMaterialDetail,
  checkAnswer,
  clearSession,
  finishSimulation,
  getAdaptivePlan,
  getDashboard,
  getErrorNotebook,
  getFlashcards,
  getHistory,
  getLearningProgress,
  getMe,
  getQuestions,
  getRecommendation,
  getStoredToken,
  getTrails,
  getStudyMaterials,
  getStudyMaterial,
  getStudyMaterialAccess,
  completeStudyChapter,
  uploadStudyMaterial,
  login,
  register,
  requestPasswordReset,
  resetPassword
} from './api'

type Page = 'dashboard' | 'simulation' | 'errors' | 'trails' | 'library' | 'flashcards' | 'progress' | 'adaptive'
type Theme = 'light' | 'moderate' | 'dark'
type SimulationMode = 'study' | 'exam'

const THEME_KEY = 'sisaws-theme'
const SERVICES = ['', 'IAM', 'VPC', 'EC2', 'Auto Scaling', 'S3', 'CloudFront', 'RDS', 'DynamoDB', 'SQS', 'KMS', 'Route 53']
const EXAM_DURATION_SECONDS = 130 * 60

const DIFFICULTIES: {value: '' | Difficulty; label: string}[] = [
  {value: '', label: 'Todas'},
  {value: 'EASY', label: 'Fácil'},
  {value: 'MEDIUM', label: 'Médio'},
  {value: 'HARD', label: 'Difícil'},
  {value: 'EXAM', label: 'Estilo prova'}
]

function initialTheme(): Theme {
  if (typeof window === 'undefined') return 'moderate'
  const saved = window.localStorage.getItem(THEME_KEY)
  return saved === 'light' || saved === 'moderate' || saved === 'dark' ? saved : 'moderate'
}

export default function App() {
  const [page, setPage] = useState<Page>('dashboard')
  const [theme, setTheme] = useState<Theme>(initialTheme)
  const [user, setUser] = useState<AuthUser | null>(null)
  const [checkingSession, setCheckingSession] = useState(true)
  const [dashboard, setDashboard] = useState<Dashboard | null>(null)
  const [recommendation, setRecommendation] = useState<Recommendation | null>(null)
  const [adaptivePlan, setAdaptivePlan] = useState<AdaptivePlan | null>(null)
  const [questions, setQuestions] = useState<Question[]>([])
  const [errorEntries, setErrorEntries] = useState<ErrorNotebookEntry[]>([])
  const [trails, setTrails] = useState<StudyTrail[]>([])
  const [materials, setMaterials] = useState<StudyMaterial[]>([])
  const [selectedMaterial, setSelectedMaterial] = useState<StudyMaterialDetail | null>(null)
  const [materialBusy, setMaterialBusy] = useState(false)
  const [flashcards, setFlashcards] = useState<Flashcard[]>([])
  const [serviceProgress, setServiceProgress] = useState<ServiceProgress[]>([])
  const [history, setHistory] = useState<HistoryAttempt[]>([])
  const [revealedCards, setRevealedCards] = useState<Set<number>>(new Set())
  const [simulationService, setSimulationService] = useState('')
  const [simulationDifficulty, setSimulationDifficulty] = useState<'' | Difficulty>('')
  const [simulationMode, setSimulationMode] = useState<SimulationMode>('study')
  const [questionLimit, setQuestionLimit] = useState(10)
  const [index, setIndex] = useState(0)
  const [answers, setAnswers] = useState<Record<number, number>>({})
  const [studyFeedback, setStudyFeedback] = useState<Record<number, AnswerCheck>>({})
  const [reviewQuestionIds, setReviewQuestionIds] = useState<Set<number>>(new Set())
  const [secondsRemaining, setSecondsRemaining] = useState(EXAM_DURATION_SECONDS)
  const [finishing, setFinishing] = useState(false)
  const [result, setResult] = useState<SimulationResult | null>(null)
  const [error, setError] = useState('')

  const current = questions[index]
  const answered = Object.keys(answers).length
  const progress = questions.length ? Math.round((answered / questions.length) * 100) : 0
  const currentFeedback = current ? studyFeedback[current.id] : undefined

  useEffect(() => {
    document.documentElement.dataset.theme = theme
    document.documentElement.style.colorScheme = theme === 'dark' ? 'dark' : 'light'
    window.localStorage.setItem(THEME_KEY, theme)
  }, [theme])

  useEffect(() => {
    if (!getStoredToken()) {
      setCheckingSession(false)
      return
    }

    getMe()
      .then(setUser)
      .catch(() => clearSession())
      .finally(() => setCheckingSession(false))
  }, [])

  useEffect(() => {
    if (user) refreshDashboard()
  }, [user])

  useEffect(() => {
    if (simulationMode !== 'exam' || questions.length === 0 || result || finishing) return

    const timer = window.setInterval(() => {
      setSecondsRemaining(previous => Math.max(0, previous - 1))
    }, 1000)

    return () => window.clearInterval(timer)
  }, [simulationMode, questions.length, result, finishing])

  useEffect(() => {
    if (
      simulationMode === 'exam'
      && questions.length > 0
      && secondsRemaining === 0
      && !result
      && !finishing
    ) {
      void finish(true)
    }
  }, [secondsRemaining, simulationMode, questions.length, result, finishing])

  async function refreshDashboard() {
    try {
      const [dashboardResponse, recommendationResponse] = await Promise.all([
        getDashboard(),
        getRecommendation()
      ])
      setDashboard(dashboardResponse)
      setRecommendation(recommendationResponse)
      setError('')
    } catch {
      setError('Não foi possível carregar seu progresso. Verifique o backend.')
    }
  }

  function openSimulationSetup(service = '', difficulty: '' | Difficulty = '') {
    setSimulationService(service)
    setSimulationDifficulty(difficulty)
    setQuestions([])
    setAnswers({})
    setStudyFeedback({})
    setReviewQuestionIds(new Set())
    setIndex(0)
    setSecondsRemaining(EXAM_DURATION_SECONDS)
    setResult(null)
    setPage('simulation')
    setError('')
  }

  async function startSimulation(service = simulationService, difficulty = simulationDifficulty) {
    try {
      const loaded = await getQuestions(questionLimit, service, difficulty)

      if (loaded.length === 0) {
        setError('Nenhuma questão encontrada para estes filtros. Tente outro serviço ou dificuldade.')
        return
      }

      setSimulationService(service)
      setSimulationDifficulty(difficulty)
      setQuestions(loaded)
      setAnswers({})
      setStudyFeedback({})
      setReviewQuestionIds(new Set())
      setIndex(0)
      setSecondsRemaining(EXAM_DURATION_SECONDS)
      setResult(null)
      setPage('simulation')
      setError('')
    } catch {
      setError('Não foi possível iniciar o simulado.')
    }
  }

  async function openErrorNotebook() {
    try {
      setErrorEntries(await getErrorNotebook())
      setPage('errors')
      setError('')
    } catch {
      setError('Não foi possível carregar o Caderno de Erros.')
    }
  }

  async function openLibrary() {
    try {
      const loaded = await getStudyMaterials()
      setMaterials(loaded)
      setSelectedMaterial(null)
      setPage('library')
      setError('')
    } catch {
      setError('Não foi possível carregar a Biblioteca Acadêmica.')
    }
  }

  async function viewMaterial(id: number) {
    try {
      setSelectedMaterial(await getStudyMaterial(id))
      setError('')
    } catch {
      setError('Não foi possível carregar os detalhes do material.')
    }
  }

  async function openMaterialFile(id: number) {
    try {
      const access = await getStudyMaterialAccess(id)
      window.open(access.url, '_blank', 'noopener,noreferrer')
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Não foi possível abrir o material.')
    }
  }

  async function completeMaterialChapter(materialId: number, chapterId: number) {
    try {
      const detail = await completeStudyChapter(materialId, chapterId)
      setSelectedMaterial(detail)
      setMaterials(await getStudyMaterials())
      setError('')
    } catch {
      setError('Não foi possível atualizar o progresso da leitura.')
    }
  }

  async function publishMaterial(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = event.currentTarget
    setMaterialBusy(true)
    setError('')

    try {
      const detail = await uploadStudyMaterial(new FormData(form))
      form.reset()
      setMaterials(await getStudyMaterials())
      setSelectedMaterial(detail)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Não foi possível publicar o material.')
    } finally {
      setMaterialBusy(false)
    }
  }

  async function openTrails() {
    try {
      setTrails(await getTrails())
      setPage('trails')
      setError('')
    } catch {
      setError('Não foi possível carregar as trilhas de estudo.')
    }
  }

  async function openFlashcards() {
    try {
      setFlashcards(await getFlashcards())
      setRevealedCards(new Set())
      setPage('flashcards')
      setError('')
    } catch {
      setError('Não foi possível carregar os flashcards.')
    }
  }

  async function openProgress() {
    try {
      const [progressResponse, historyResponse] = await Promise.all([
        getLearningProgress(),
        getHistory()
      ])
      setServiceProgress(progressResponse)
      setHistory(historyResponse)
      setPage('progress')
      setError('')
    } catch {
      setError('Não foi possível carregar sua análise de progresso.')
    }
  }

  async function openAdaptive() {
    try {
      const plan = await getAdaptivePlan()
      setAdaptivePlan(plan)
      setPage('adaptive')
      setError('')
    } catch {
      setError('Não foi possível gerar sua revisão inteligente.')
    }
  }

  async function startAdaptiveSession() {
    if (!adaptivePlan) return

    const next = adaptivePlan.nextAction

    try {
      const loaded = await getQuestions(next.questionCount, next.awsService, next.difficulty)

      if (loaded.length === 0) {
        setError('O plano foi gerado, mas ainda não há questões suficientes para este foco.')
        return
      }

      setSimulationMode(next.mode)
      setSimulationService(next.awsService)
      setSimulationDifficulty(next.difficulty)
      setQuestionLimit(next.questionCount)
      setQuestions(loaded)
      setAnswers({})
      setStudyFeedback({})
      setReviewQuestionIds(new Set())
      setIndex(0)
      setSecondsRemaining(EXAM_DURATION_SECONDS)
      setResult(null)
      setPage('simulation')
      setError('')
    } catch {
      setError('Não foi possível iniciar a sessão adaptativa.')
    }
  }

  async function selectAnswer(optionId: number) {
    if (!current) return

    setAnswers(previous => ({...previous, [current.id]: optionId}))

    if (simulationMode === 'study') {
      try {
        const feedback = await checkAnswer(current.id, [optionId])
        setStudyFeedback(previous => ({...previous, [current.id]: feedback}))
      } catch {
        setError('Não foi possível verificar esta resposta.')
      }
    }
  }

  function toggleReview(questionId: number) {
    setReviewQuestionIds(previous => {
      const next = new Set(previous)
      if (next.has(questionId)) next.delete(questionId)
      else next.add(questionId)
      return next
    })
  }

  async function finish(force = false) {
    if (!force && simulationMode === 'study' && answered !== questions.length) return

    setFinishing(true)

    try {
      const payload = questions.map(q => ({
        questionId: q.id,
        selectedOptionIds: answers[q.id] ? [answers[q.id]] : []
      }))

      const response = await finishSimulation(payload)
      setResult(response)
      await refreshDashboard()
    } catch {
      setError('Não foi possível corrigir o simulado.')
    } finally {
      setFinishing(false)
    }
  }

  function toggleFlashcard(id: number) {
    setRevealedCards(previous => {
      const next = new Set(previous)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  }

  function logout() {
    clearSession()
    setUser(null)
    setDashboard(null)
    setRecommendation(null)
    setAdaptivePlan(null)
    setQuestions([])
    setErrorEntries([])
    setTrails([])
    setMaterials([])
    setSelectedMaterial(null)
    setFlashcards([])
    setServiceProgress([])
    setHistory([])
    setStudyFeedback({})
    setReviewQuestionIds(new Set())
    setResult(null)
    setPage('dashboard')
    setError('')
  }

  const resultMap = useMemo(
    () => new Map(result?.results.map(item => [item.questionId, item]) ?? []),
    [result]
  )

  if (checkingSession) {
    return <div className="session-splash"><Cloud size={42}/><strong>SisAWS</strong><span>Carregando ambiente de estudos...</span></div>
  }

  if (!user) {
    return <AuthScreen theme={theme} setTheme={setTheme} onAuthenticated={setUser} />
  }

  const pageTitle: Record<Page, string> = {
    dashboard: `Olá, ${user.name.split(' ')[0]}`,
    simulation: 'Simulados SAA-C03',
    errors: 'Caderno de Erros',
    trails: 'Trilhas AWS',
    library: 'Biblioteca Acadêmica',
    flashcards: 'Flashcards',
    progress: 'Meu Progresso',
    adaptive: 'Revisão Inteligente'
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand"><Cloud size={28}/><span>SisAWS</span></div>

        <nav>
          <button className={page === 'dashboard' ? 'active' : ''} onClick={() => setPage('dashboard')}><Gauge/> Dashboard</button>
          <button className={page === 'simulation' ? 'active' : ''} onClick={() => openSimulationSetup()}><BookOpenCheck/> Simulados</button>
          <button className={page === 'trails' ? 'active' : ''} onClick={openTrails}><BookOpen/> Trilhas AWS</button>
          <button className={page === 'library' ? 'active' : ''} onClick={openLibrary}><LibraryBig/> Biblioteca Acadêmica</button>
          <button className={page === 'flashcards' ? 'active' : ''} onClick={openFlashcards}><Brain/> Flashcards</button>
          <button className={page === 'progress' ? 'active' : ''} onClick={openProgress}><BarChart3/> Meu Progresso</button>
          <button className={page === 'adaptive' ? 'active' : ''} onClick={openAdaptive}><Sparkles/> Revisão Inteligente</button>
          <button className={page === 'errors' ? 'active' : ''} onClick={openErrorNotebook}><NotebookPen/> Caderno de Erros</button>
          <button disabled><Award/> Certificações</button>
          <button disabled><History/> Histórico completo</button>
        </nav>

        <div className="sidebar-user">
          <UserCircle size={20}/>
          <div>
            <strong>{user.name}</strong>
            <span>{user.email}</span>
          </div>
        </div>
        <div className="version">v2.2 • Learning Library</div>
      </aside>

      <main>
        <header>
          <div className="header-title">
            <p className="eyebrow">AWS LEARNING PLATFORM</p>
            <h1>{pageTitle[page]}</h1>
          </div>

          <div className="header-actions">
            <ThemeSwitcher theme={theme} setTheme={setTheme}/>
            <button className="logout-button" onClick={logout} title="Sair"><LogOut size={17}/> Sair</button>
          </div>
        </header>

        {error && <div className="alert">{error}</div>}

        {page === 'dashboard' && (
          <div className="page-content dashboard-content">
            <section className="hero">
              <div>
                <span className="pill">AWS Certified Solutions Architect</span>
                <h2>Estude com base no seu desempenho real.</h2>
                <p>O SisAWS agora mede seus acertos por serviço e direciona a próxima revisão com base no que você responde.</p>
                <div className="hero-buttons">
                  <button className="primary" onClick={() => openSimulationSetup()}><PlayCircle/> Novo simulado</button>
                  <button className="secondary" onClick={openProgress}><Target/> Ver meu progresso</button>
                </div>
              </div>
              <div className="hero-mark">AWS</div>
            </section>

            <section className="cards five-cards">
              <Metric label="Banco de questões" value={dashboard?.questionBank ?? 0} />
              <Metric label="Simulados" value={dashboard?.attempts ?? 0} />
              <Metric label="Média" value={dashboard?.averageScore ?? 0} suffix="%" />
              <Metric label="Melhor nota" value={dashboard?.bestScore ?? 0} suffix="%" />
              <Metric label="Para revisar" value={dashboard?.errorNotebookCount ?? 0} />
            </section>

            <section className="panel recommendation-panel">
              <div className="recommendation-copy">
                <p className="eyebrow">RECOMENDAÇÃO DO SISAWS</p>
                <h3>{recommendation?.title ?? 'Continue praticando'}</h3>
                <p>{recommendation?.reason ?? 'Faça um simulado para gerar uma recomendação personalizada.'}</p>
                <small>{recommendation?.recommendedAction}</small>
              </div>
              <div className="recommendation-action">
                <span className="focus-service">{recommendation?.awsService ?? 'IAM'}</span>
                <button className="secondary" onClick={() => openSimulationSetup(recommendation?.awsService ?? 'IAM')}>
                  <Target size={16}/> Praticar serviço
                </button>
                <button className="secondary adaptive-shortcut" onClick={openAdaptive}>
                  <Sparkles size={16}/> Revisão inteligente
                </button>
              </div>
            </section>
          </div>
        )}

        {page === 'simulation' && !current && !result && (
          <div className="page-content setup-content">
            <section className="setup-card">
              <div className="setup-heading">
                <SlidersHorizontal size={26}/>
                <div>
                  <p className="eyebrow">SIMULADO PERSONALIZADO</p>
                  <h2>Escolha como quer praticar</h2>
                  <p>Deixe os filtros em “Todos” para um simulado misto ou foque exatamente no serviço que precisa revisar.</p>
                </div>
              </div>

              <div className="mode-grid">
                <button
                  className={simulationMode === 'study' ? 'mode-card active' : 'mode-card'}
                  onClick={() => setSimulationMode('study')}
                >
                  <GraduationCap size={24}/>
                  <strong>Modo Estudo</strong>
                  <span>Feedback imediato, resposta correta e explicação após cada escolha.</span>
                </button>
                <button
                  className={simulationMode === 'exam' ? 'mode-card active' : 'mode-card'}
                  onClick={() => setSimulationMode('exam')}
                >
                  <Clock3 size={24}/>
                  <strong>Modo Prova</strong>
                  <span>Sem revelar a correção durante a prova e com cronômetro de 130 minutos.</span>
                </button>
              </div>

              <div className="filter-grid three-filters">
                <label>
                  Serviço AWS
                  <select value={simulationService} onChange={event => setSimulationService(event.target.value)}>
                    {SERVICES.map(service => <option value={service} key={service || 'all'}>{service || 'Todos os serviços'}</option>)}
                  </select>
                </label>

                <label>
                  Dificuldade
                  <select value={simulationDifficulty} onChange={event => setSimulationDifficulty(event.target.value as '' | Difficulty)}>
                    {DIFFICULTIES.map(item => <option value={item.value} key={item.value || 'all'}>{item.label}</option>)}
                  </select>
                </label>

                <label>
                  Questões
                  <select value={questionLimit} onChange={event => setQuestionLimit(Number(event.target.value))}>
                    <option value={5}>5 questões</option>
                    <option value={10}>10 questões</option>
                    <option value={65}>Banco completo (até 65)</option>
                  </select>
                </label>
              </div>

              <div className="exam-reference">
                <Clock3 size={17}/>
                <span>Referência atual do SAA-C03: 65 questões em 130 minutos. O SisAWS usa somente as questões autorais disponíveis no banco atual.</span>
              </div>

              <div className="setup-summary four-summary">
                <div><span>Modo</span><strong>{simulationMode === 'study' ? 'Estudo' : 'Prova'}</strong></div>
                <div><span>Certificação</span><strong>SAA-C03</strong></div>
                <div><span>Serviço</span><strong>{simulationService || 'Misto'}</strong></div>
                <div><span>Questões solicitadas</span><strong>{questionLimit === 65 ? 'Banco completo' : questionLimit}</strong></div>
              </div>

              <button className="primary setup-start" onClick={() => startSimulation()}>
                <PlayCircle/> Iniciar prática
              </button>
            </section>
          </div>
        )}

        {page === 'simulation' && current && !result && (
          <div className="page-content simulation-content">
            <section className="quiz-card">
              <div className="quiz-top">
                <div className="quiz-labels">
                  <span className="pill">{current.awsService}</span>
                  <span className="difficulty">{current.difficulty}</span>
                  <span className="mode-pill">{simulationMode === 'study' ? 'ESTUDO' : 'PROVA'}</span>
                </div>
                <div className="quiz-meta">
                  {simulationMode === 'exam' && (
                    <span className={secondsRemaining <= 600 ? 'exam-timer warning' : 'exam-timer'}>
                      <Clock3 size={16}/> {formatTime(secondsRemaining)}
                    </span>
                  )}
                  <button
                    className={reviewQuestionIds.has(current.id) ? 'review-flag active' : 'review-flag'}
                    onClick={() => toggleReview(current.id)}
                  >
                    <Flag size={16}/> {reviewQuestionIds.has(current.id) ? 'Marcada' : 'Revisar'}
                  </button>
                  <strong>{index + 1} / {questions.length}</strong>
                </div>
              </div>
              <div className="progress"><span style={{width: `${progress}%`}} /></div>
              <p className="domain">{current.domain}</p>
              <h2>{current.prompt}</h2>
              <div className="options">
                {current.options.map((option, optionIndex) => {
                  const selected = answers[current.id] === option.id
                  const correctOption = simulationMode === 'study'
                    && currentFeedback?.correctOptionIds.includes(option.id)
                  const wrongSelected = simulationMode === 'study'
                    && Boolean(currentFeedback)
                    && selected
                    && !currentFeedback?.correct

                  const classes = [
                    selected ? 'selected' : '',
                    correctOption ? 'correct-option' : '',
                    wrongSelected ? 'wrong-option' : ''
                  ].filter(Boolean).join(' ')

                  return (
                    <button
                      key={option.id}
                      className={classes}
                      onClick={() => selectAnswer(option.id)}
                    >
                      <b>{String.fromCharCode(65 + optionIndex)}</b>
                      <span className="option-text">{option.text}</span>
                      {correctOption ? (
                        <span className="selected-check correct-check" aria-label="Resposta correta">
                          <CheckCircle2 size={21}/>
                        </span>
                      ) : wrongSelected ? (
                        <span className="selected-check wrong-check" aria-label="Resposta incorreta">
                          <XCircle size={21}/>
                        </span>
                      ) : selected ? (
                        <span className="selected-check" aria-label="Resposta selecionada">
                          <CheckCircle2 size={21}/>
                        </span>
                      ) : null}
                    </button>
                  )
                })}
              </div>

              {simulationMode === 'study' && currentFeedback && (
                <div className={currentFeedback.correct ? 'study-feedback correct' : 'study-feedback incorrect'}>
                  <div>
                    {currentFeedback.correct ? <CheckCircle2 size={21}/> : <XCircle size={21}/>}
                    <strong>{currentFeedback.correct ? 'Resposta correta' : 'Resposta incorreta'}</strong>
                  </div>
                  <p>{currentFeedback.explanation}</p>
                </div>
              )}
              <div className="quiz-actions">
                <button disabled={index === 0} onClick={() => setIndex(index - 1)}>Anterior</button>
                {index < questions.length - 1
                  ? <button className="primary" onClick={() => setIndex(index + 1)}>Próxima</button>
                  : <button
                      className="primary"
                      disabled={finishing || (simulationMode === 'study' && answered !== questions.length)}
                      onClick={() => finish(false)}
                    >
                      {finishing ? 'Finalizando...' : 'Finalizar simulado'}
                    </button>}
              </div>
            </section>
          </div>
        )}

        {page === 'simulation' && result && (
          <div className="page-content simulation-content">
            <section className="result-card">
              <p className="eyebrow">RESULTADO</p>
              <div className="score">{result.scorePercent}%</div>
              <h2>{result.correctAnswers} de {result.totalQuestions} questões corretas</h2>
              <p>Seu progresso por serviço foi atualizado e os erros foram enviados para o Caderno de Erros.</p>
              {reviewQuestionIds.size > 0 && (
                <div className="review-summary"><Flag size={17}/> {reviewQuestionIds.size} questão(ões) foram marcadas para revisão durante esta tentativa.</div>
              )}
              <div className="review-list">
                {questions.map((question, questionIndex) => {
                  const item = resultMap.get(question.id)
                  return <div className={item?.correct ? 'review ok' : 'review fail'} key={question.id}>
                    <strong>Questão {questionIndex + 1} • {question.awsService}</strong>
                    <span>{item?.correct ? 'Correta' : 'Revisar'}</span>
                    <p>{item?.explanation}</p>
                  </div>
                })}
              </div>
              <div className="quiz-actions">
                <button onClick={openProgress}>Ver progresso</button>
                <button className="primary" onClick={() => startSimulation(simulationService, simulationDifficulty)}>Repetir prática</button>
              </div>
            </section>
          </div>
        )}


        {page === 'library' && (
          <div className="page-content library-page">
            <section className="library-shell">
              <div className="library-heading">
                <div>
                  <p className="eyebrow">BIBLIOTECA ACADÊMICA</p>
                  <h2>Estude o conteúdo antes de ir para o simulado.</h2>
                  <p>Materiais com referência acadêmica, licença registrada, progresso de leitura e prática ligada aos serviços AWS.</p>
                </div>
                <div className="library-stat">
                  <strong>{materials.length}</strong>
                  <span>materiais</span>
                </div>
              </div>

              {user.role === 'INSTRUCTOR' && (
                <form className="material-upload" onSubmit={publishMaterial}>
                  <div className="section-heading compact">
                    <div>
                      <p className="eyebrow">ÁREA DO INSTRUTOR</p>
                      <h3>Publicar material</h3>
                    </div>
                    <Upload size={23}/>
                  </div>
                  <div className="material-form-grid">
                    <label>Título<input name="title" required maxLength={220} placeholder="Ex.: Fundamentos de Amazon S3"/></label>
                    <label>Autor<input name="author" required maxLength={180} placeholder="Autor ou instituição"/></label>
                    <label>Serviço AWS
                      <select name="awsService" required defaultValue="S3">
                        {SERVICES.filter(Boolean).map(service => <option key={service}>{service}</option>)}
                      </select>
                    </label>
                    <label>Domínio SAA-C03
                      <select name="saaDomain" required defaultValue="Design Resilient Architectures">
                        <option>Design Secure Architectures</option>
                        <option>Design Resilient Architectures</option>
                        <option>Design High-Performing Architectures</option>
                        <option>Design Cost-Optimized Architectures</option>
                      </select>
                    </label>
                    <label>Licença
                      <select name="licenseType" required defaultValue="INTERNAL">
                        <option value="INTERNAL">Material interno</option>
                        <option value="OWNED">Autoral / próprio</option>
                        <option value="LICENSED">Licenciado</option>
                        <option value="OPEN_LICENSE">Licença aberta</option>
                        <option value="PUBLIC_DOMAIN">Domínio público</option>
                      </select>
                    </label>
                    <label>Tempo estimado (min)<input name="estimatedMinutes" type="number" min={1} defaultValue={30} required/></label>
                    <label>Editora<input name="publisher" placeholder="Opcional"/></label>
                    <label>Edição<input name="edition" placeholder="Opcional"/></label>
                    <label>Ano<input name="publicationYear" type="number" min={1900} max={2100} placeholder="2026"/></label>
                    <label>ISBN<input name="isbn" placeholder="Opcional"/></label>
                    <label className="wide-field">Fonte / URL<input name="sourceUrl" type="url" placeholder="https://..."/></label>
                    <label className="wide-field">Descrição<textarea name="description" rows={3} placeholder="Objetivos, escopo e orientação de estudo."/></label>
                    <label className="wide-field file-field">Arquivo PDF, DOCX ou EPUB<input name="file" type="file" accept=".pdf,.docx,.epub" required/></label>
                  </div>
                  <button className="primary material-publish" disabled={materialBusy}>
                    <Upload size={17}/> {materialBusy ? 'Publicando...' : 'Publicar na biblioteca'}
                  </button>
                </form>
              )}

              <div className="library-layout">
                <div className="material-list">
                  {materials.length === 0 && (
                    <div className="library-empty">
                      <LibraryBig size={34}/>
                      <strong>Nenhum material publicado ainda.</strong>
                      <span>Quando um instrutor publicar um PDF, DOCX ou EPUB, ele aparecerá aqui.</span>
                    </div>
                  )}

                  {materials.map(material => (
                    <button className={selectedMaterial?.material.id === material.id ? 'material-card active' : 'material-card'} key={material.id} onClick={() => viewMaterial(material.id)}>
                      <div className="material-icon"><FileText size={24}/></div>
                      <div className="material-copy">
                        <div className="material-meta">
                          <span>{material.materialType}</span>
                          <span>{material.awsService}</span>
                          <span>{material.licenseType.replaceAll('_', ' ')}</span>
                        </div>
                        <h3>{material.title}</h3>
                        <p>{material.author} • {material.estimatedMinutes} min</p>
                        <div className="material-progress"><span style={{width: `${material.progressPercent}%`}}/></div>
                        <small>{material.progressPercent}% concluído • {material.completedChapters}/{material.chapterCount} capítulo(s)</small>
                      </div>
                    </button>
                  ))}
                </div>

                <div className="material-detail">
                  {!selectedMaterial && (
                    <div className="library-empty detail-empty">
                      <BookOpen size={34}/>
                      <strong>Selecione um material</strong>
                      <span>Veja capítulos, referência acadêmica e progresso de leitura.</span>
                    </div>
                  )}

                  {selectedMaterial && (
                    <>
                      <div className="material-detail-head">
                        <div>
                          <p className="eyebrow">{selectedMaterial.material.awsService} • {selectedMaterial.material.saaDomain}</p>
                          <h2>{selectedMaterial.material.title}</h2>
                          <p>{selectedMaterial.description || 'Material de estudo da Biblioteca SisAWS.'}</p>
                        </div>
                        <span className="material-score">{selectedMaterial.material.progressPercent}%</span>
                      </div>

                      <div className="academic-reference">
                        <strong>Referência acadêmica</strong>
                        <span>{selectedMaterial.material.author}. <b>{selectedMaterial.material.title}</b>{selectedMaterial.edition ? `. ${selectedMaterial.edition}` : ''}{selectedMaterial.publisher ? `. ${selectedMaterial.publisher}` : ''}{selectedMaterial.publicationYear ? `, ${selectedMaterial.publicationYear}` : ''}.</span>
                        {selectedMaterial.isbn && <small>ISBN: {selectedMaterial.isbn}</small>}
                      </div>

                      <div className="material-actions">
                        <button className="primary" onClick={() => openMaterialFile(selectedMaterial.material.id)}><ExternalLink size={16}/> Abrir material</button>
                        <button className="secondary" onClick={() => openSimulationSetup(selectedMaterial.material.awsService)}><Target size={16}/> Praticar este conteúdo</button>
                      </div>

                      <div className="chapter-list">
                        <div className="section-heading compact">
                          <div><p className="eyebrow">PLANO DE LEITURA</p><h3>Capítulos</h3></div>
                        </div>
                        {selectedMaterial.chapters.map(chapter => (
                          <div className={chapter.completed ? 'chapter-row completed' : 'chapter-row'} key={chapter.id}>
                            <span className="chapter-number">{String(chapter.chapterNumber).padStart(2, '0')}</span>
                            <div>
                              <strong>{chapter.title}</strong>
                              <p>{chapter.description || chapter.saaDomain}</p>
                              <small>{chapter.awsService} • {chapter.estimatedMinutes} min</small>
                            </div>
                            {chapter.completed
                              ? <span className="chapter-done"><CheckCircle2 size={17}/> Concluído</span>
                              : <button className="secondary" onClick={() => completeMaterialChapter(selectedMaterial.material.id, chapter.id)}>Marcar como estudado</button>}
                          </div>
                        ))}
                      </div>
                    </>
                  )}
                </div>
              </div>
            </section>
          </div>
        )}

        {page === 'trails' && (
          <div className="page-content learning-page">
            <section className="learning-shell">
              <div className="section-heading">
                <div><p className="eyebrow">ROADMAP DE ESTUDO</p><h2>Trilhas organizadas por competência</h2></div>
                <span className="notebook-count">{trails.length} trilhas</span>
              </div>
              <div className="trail-grid">
                {trails.map((trail, trailIndex) => (
                  <article className="trail-card" key={trail.id}>
                    <div className="trail-number">{String(trailIndex + 1).padStart(2, '0')}</div>
                    <div>
                      <div className="trail-meta"><span>{trail.level}</span><span>{trail.estimatedMinutes} min</span></div>
                      <h3>{trail.title}</h3>
                      <p>{trail.description}</p>
                      <div className="service-chips">
                        {trail.services.map(service => (
                          <button key={service} onClick={() => openSimulationSetup(service)}>{service}</button>
                        ))}
                      </div>
                    </div>
                  </article>
                ))}
              </div>
            </section>
          </div>
        )}

        {page === 'flashcards' && (
          <div className="page-content learning-page">
            <section className="learning-shell">
              <div className="section-heading">
                <div><p className="eyebrow">REVISÃO RÁPIDA</p><h2>Clique no card para revelar a resposta</h2></div>
                <span className="notebook-count">{flashcards.length} cards</span>
              </div>
              <div className="flashcard-grid">
                {flashcards.map(card => {
                  const revealed = revealedCards.has(card.id)
                  return (
                    <button className={revealed ? 'flashcard revealed' : 'flashcard'} key={card.id} onClick={() => toggleFlashcard(card.id)}>
                      <div className="flashcard-top"><span className="pill">{card.awsService}</span><Lightbulb size={18}/></div>
                      <strong>{card.question}</strong>
                      <p>{revealed ? card.answer : 'Clique para revelar a resposta'}</p>
                    </button>
                  )
                })}
              </div>
            </section>
          </div>
        )}

        {page === 'progress' && (
          <div className="page-content progress-page">
            <section className="progress-shell">
              <div className="section-heading">
                <div><p className="eyebrow">ANÁLISE POR SERVIÇO</p><h2>Onde você está forte e onde precisa revisar</h2></div>
                <button className="secondary" onClick={() => openSimulationSetup(recommendation?.awsService ?? '')}><Target size={16}/> Prática recomendada</button>
              </div>

              {serviceProgress.length === 0 ? (
                <div className="empty-state">
                  <BarChart3 size={46}/>
                  <h3>Ainda não há dados por serviço</h3>
                  <p>Finalize seu primeiro simulado para o SisAWS começar a medir sua evolução.</p>
                  <button className="primary" onClick={() => openSimulationSetup()}>Começar agora</button>
                </div>
              ) : (
                <div className="service-progress-list">
                  {serviceProgress.map(item => (
                    <article className="service-progress-row" key={item.awsService}>
                      <div className="service-progress-title">
                        <strong>{item.awsService}</strong>
                        <span className={`status-badge status-${item.status.toLowerCase()}`}>{statusLabel(item.status)}</span>
                      </div>
                      <div className="accuracy-line">
                        <div className="accuracy-track"><span style={{width: `${item.accuracyPercent}%`}}/></div>
                        <b>{item.accuracyPercent}%</b>
                      </div>
                      <small>{item.correctAnswers} acertos em {item.answered} respostas</small>
                    </article>
                  ))}
                </div>
              )}

              <div className="history-section">
                <div className="section-heading compact"><div><p className="eyebrow">ÚLTIMAS TENTATIVAS</p><h3>Evolução recente</h3></div></div>
                <div className="history-list">
                  {history.length === 0 && <p className="muted">Nenhum simulado finalizado ainda.</p>}
                  {history.map(attempt => (
                    <article className="history-row" key={attempt.id}>
                      <div><strong>{attempt.certificationCode}</strong><span>{new Date(attempt.finishedAt).toLocaleString('pt-BR')}</span></div>
                      <div className="history-score-track"><span style={{width: `${attempt.scorePercent}%`}}/></div>
                      <b>{attempt.scorePercent}%</b>
                    </article>
                  ))}
                </div>
              </div>
            </section>
          </div>
        )}


        {page === 'adaptive' && adaptivePlan && (
          <div className="page-content adaptive-page">
            <section className="adaptive-shell">
              <div className="adaptive-hero">
                <div>
                  <p className="eyebrow">PLANO PERSONALIZADO</p>
                  <h2>O SisAWS analisou seus pontos de atenção</h2>
                  <p>
                    O ranking combina taxa de acerto, quantidade de respostas e reincidência de erros.
                    Quanto maior a prioridade, mais cedo vale revisar aquele serviço.
                  </p>
                </div>
                <div className="adaptive-level">
                  <span>Nível atual</span>
                  <strong>{adaptiveLevelLabel(adaptivePlan.overallLevel)}</strong>
                </div>
              </div>

              <section className="adaptive-next">
                <div className="adaptive-next-icon"><Sparkles size={26}/></div>
                <div className="adaptive-next-copy">
                  <p className="eyebrow">PRÓXIMA AÇÃO</p>
                  <h3>{adaptivePlan.nextAction.awsService} • {adaptivePlan.nextAction.difficulty}</h3>
                  <p>{adaptivePlan.nextAction.reason}</p>
                  <div className="adaptive-tags">
                    <span>Modo Estudo</span>
                    <span>{adaptivePlan.nextAction.questionCount} questões</span>
                    <span>{adaptivePlan.nextAction.difficulty}</span>
                  </div>
                </div>
                <button className="primary adaptive-start" onClick={startAdaptiveSession}>
                  <PlayCircle size={18}/> Iniciar sessão adaptativa
                </button>
              </section>

              <div className="adaptive-ranking-heading">
                <div>
                  <p className="eyebrow">RANKING DE PRIORIDADE</p>
                  <h3>Serviços que merecem mais atenção</h3>
                </div>
                <small>Atualizado em {new Date(adaptivePlan.generatedAt).toLocaleString('pt-BR')}</small>
              </div>

              <div className="adaptive-ranking">
                {adaptivePlan.focusServices.map((item, itemIndex) => (
                  <article className="adaptive-row" key={item.awsService}>
                    <div className="adaptive-rank">{itemIndex + 1}</div>
                    <div className="adaptive-service">
                      <div className="adaptive-service-top">
                        <strong>{item.awsService}</strong>
                        <span>{item.priorityScore}/100 prioridade</span>
                      </div>
                      <div className="adaptive-priority-track">
                        <span style={{width: `${item.priorityScore}%`}}/>
                      </div>
                      <p>{item.reason}</p>
                      <div className="adaptive-stats">
                        <span>{item.accuracyPercent}% de acerto</span>
                        <span>{item.answered} respostas</span>
                        <span>{item.wrongCount} erros registrados</span>
                      </div>
                    </div>
                    <button
                      className="secondary"
                      onClick={() => {
                        setSimulationMode('study')
                        setQuestionLimit(5)
                        openSimulationSetup(item.awsService, item.accuracyPercent < 55 ? 'EASY' : item.accuracyPercent < 75 ? 'MEDIUM' : 'HARD')
                      }}
                    >
                      Praticar
                    </button>
                  </article>
                ))}
              </div>
            </section>
          </div>
        )}

        {page === 'errors' && (
          <div className="page-content notebook-content">
            <section className="notebook-card">
              <div className="notebook-heading">
                <div><p className="eyebrow">REVISÃO INTELIGENTE</p><h2>Questões que merecem sua atenção</h2></div>
                <span className="notebook-count">{errorEntries.length} para revisar</span>
              </div>

              {errorEntries.length === 0 ? (
                <div className="empty-state">
                  <ShieldCheck size={46}/>
                  <h3>Seu caderno está vazio</h3>
                  <p>Quando você errar uma questão em um simulado, ela aparecerá aqui automaticamente.</p>
                  <button className="primary" onClick={() => openSimulationSetup()}>Fazer um simulado</button>
                </div>
              ) : (
                <div className="error-list">
                  {errorEntries.map(entry => (
                    <article className="error-entry" key={entry.id}>
                      <div className="error-entry-top">
                        <div><span className="pill">{entry.awsService}</span><span className="domain-inline">{entry.domain}</span></div>
                        <strong>{entry.wrongCount}x erro{entry.wrongCount > 1 ? 's' : ''}</strong>
                      </div>
                      <h3>{entry.prompt}</h3>
                      <div className="explanation-box"><b>Revisão:</b> {entry.explanation}</div>
                      <div className="error-entry-actions">
                        <small>Último erro: {new Date(entry.lastWrongAt).toLocaleString('pt-BR')}</small>
                        <button className="secondary" onClick={() => openSimulationSetup(entry.awsService)}>Praticar {entry.awsService}</button>
                      </div>
                    </article>
                  ))}
                </div>
              )}
            </section>
          </div>
        )}
      </main>
    </div>
  )
}

function adaptiveLevelLabel(level: AdaptivePlan['overallLevel']) {
  const labels = {
    FOUNDATION: 'Fundamentos',
    DEVELOPING: 'Em desenvolvimento',
    CONSOLIDATING: 'Consolidando',
    STRONG: 'Forte'
  }
  return labels[level]
}

function statusLabel(status: ServiceProgress['status']) {
  const labels = {
    STARTING: 'Começando',
    REVIEW: 'Revisar',
    GOOD: 'Bom',
    STRONG: 'Forte'
  }
  return labels[status]
}

function ThemeSwitcher({theme, setTheme}: {theme: Theme; setTheme: (theme: Theme) => void}) {
  return (
    <div className="theme-switcher" role="group" aria-label="Tema da interface">
      <button className={theme === 'light' ? 'active' : ''} onClick={() => setTheme('light')}>☀ <span>Light</span></button>
      <button className={theme === 'moderate' ? 'active' : ''} onClick={() => setTheme('moderate')}>◐ <span>Moderado</span></button>
      <button className={theme === 'dark' ? 'active' : ''} onClick={() => setTheme('dark')}>☾ <span>Dark</span></button>
    </div>
  )
}

function AuthScreen({
  theme,
  setTheme,
  onAuthenticated
}: {
  theme: Theme
  setTheme: (theme: Theme) => void
  onAuthenticated: (user: AuthUser) => void
}) {
  type AuthMode = 'login' | 'register' | 'forgot' | 'reset'

  const initialResetToken = typeof window === 'undefined'
    ? ''
    : new URLSearchParams(window.location.search).get('resetToken') ?? ''

  const [mode, setMode] = useState<AuthMode>(initialResetToken ? 'reset' : 'login')
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [resetToken] = useState(initialResetToken)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  function changeMode(next: AuthMode) {
    setMode(next)
    setError('')
    setMessage('')
    setPassword('')
    setConfirmPassword('')
  }

  async function submit(event: FormEvent) {
    event.preventDefault()
    setBusy(true)
    setError('')
    setMessage('')

    try {
      if (mode === 'forgot') {
        const response = await requestPasswordReset(email)
        setMessage(response.message)
        return
      }

      if (mode === 'reset') {
        if (!resetToken) throw new Error('Link de redefinição inválido.')
        if (password !== confirmPassword) throw new Error('As senhas não coincidem.')

        await resetPassword(resetToken, password)
        window.history.replaceState({}, '', window.location.pathname)
        setMessage('Senha redefinida com sucesso. Entre com sua nova senha.')
        setMode('login')
        setPassword('')
        setConfirmPassword('')
        return
      }

      const response = mode === 'login'
        ? await login(email, password)
        : await register(name, email, password)

      onAuthenticated(response.user)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Não foi possível concluir a operação.')
    } finally {
      setBusy(false)
    }
  }

  const recovery = mode === 'forgot' || mode === 'reset'

  return (
    <div className="auth-shell">
      <div className="auth-topbar">
        <div className="brand auth-brand"><Cloud size={28}/><span>SisAWS</span></div>
        <ThemeSwitcher theme={theme} setTheme={setTheme}/>
      </div>

      <section className="auth-card">
        <div className="auth-intro">
          <span className="pill">AWS Learning Platform</span>
          <h1>{recovery ? 'Recupere seu acesso.' : 'Estude. Pratique. Evolua.'}</h1>
          <p>{recovery
            ? 'Use um link temporário e de uso único para definir uma nova senha com segurança.'
            : 'Simulados, métricas pessoais e revisão orientada para sua preparação em certificações AWS.'}</p>
        </div>

        <form onSubmit={submit}>
          {!recovery && (
            <div className="auth-tabs">
              <button type="button" className={mode === 'login' ? 'active' : ''} onClick={() => changeMode('login')}>Entrar</button>
              <button type="button" className={mode === 'register' ? 'active' : ''} onClick={() => changeMode('register')}>Criar conta</button>
            </div>
          )}

          {mode === 'register' && (
            <label>Nome<input value={name} onChange={event => setName(event.target.value)} minLength={2} required placeholder="Seu nome"/></label>
          )}

          {(mode === 'login' || mode === 'register' || mode === 'forgot') && (
            <label>E-mail<input value={email} onChange={event => setEmail(event.target.value)} type="email" required placeholder="voce@email.com"/></label>
          )}

          {(mode === 'login' || mode === 'register' || mode === 'reset') && (
            <label>{mode === 'reset' ? 'Nova senha' : 'Senha'}<input value={password} onChange={event => setPassword(event.target.value)} type="password" minLength={8} required placeholder="Mínimo de 8 caracteres"/></label>
          )}

          {mode === 'reset' && (
            <label>Confirmar nova senha<input value={confirmPassword} onChange={event => setConfirmPassword(event.target.value)} type="password" minLength={8} required placeholder="Digite novamente"/></label>
          )}

          {mode === 'login' && (
            <button type="button" className="auth-link" onClick={() => changeMode('forgot')}>Esqueci minha senha</button>
          )}

          {error && <div className="auth-error">{error}</div>}
          {message && <div className="auth-success">{message}</div>}

          <button className="primary auth-submit" disabled={busy}>
            {busy ? 'Processando...' :
              mode === 'login' ? 'Entrar no SisAWS' :
              mode === 'register' ? 'Criar minha conta' :
              mode === 'forgot' ? 'Enviar link de recuperação' :
              'Definir nova senha'}
          </button>

          {recovery && (
            <button type="button" className="auth-link auth-back" onClick={() => changeMode('login')}>Voltar para o login</button>
          )}

          <p className="auth-note">Projeto educacional independente. Não afiliado ou endossado pela Amazon Web Services.</p>
        </form>
      </section>
    </div>
  )
}

function Metric({label, value, suffix = ''}: {label: string; value: number; suffix?: string}) {
  return <div className="metric"><span>{label}</span><strong>{value}{suffix}</strong></div>
}


function formatTime(totalSeconds: number) {
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = totalSeconds % 60
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
}
