import { useEffect, useMemo, useState } from 'react'
import { Award, BookOpenCheck, Cloud, Gauge, History, PlayCircle } from 'lucide-react'
import { Dashboard, Question, SimulationResult, finishSimulation, getDashboard, getQuestions } from './api'

type Page = 'dashboard' | 'simulation'
type Theme = 'light' | 'moderate' | 'dark'

const THEME_KEY = 'sisaws-theme'

function initialTheme(): Theme {
  if (typeof window === 'undefined') return 'moderate'
  const saved = window.localStorage.getItem(THEME_KEY)
  return saved === 'light' || saved === 'moderate' || saved === 'dark' ? saved : 'moderate'
}

export default function App() {
  const [page, setPage] = useState<Page>('dashboard')
  const [theme, setTheme] = useState<Theme>(initialTheme)
  const [dashboard, setDashboard] = useState<Dashboard | null>(null)
  const [questions, setQuestions] = useState<Question[]>([])
  const [index, setIndex] = useState(0)
  const [answers, setAnswers] = useState<Record<number, number>>({})
  const [result, setResult] = useState<SimulationResult | null>(null)
  const [error, setError] = useState('')

  const current = questions[index]
  const answered = Object.keys(answers).length
  const progress = questions.length ? Math.round((answered / questions.length) * 100) : 0

  useEffect(() => {
    document.documentElement.dataset.theme = theme
    document.documentElement.style.colorScheme = theme === 'dark' ? 'dark' : 'light'
    window.localStorage.setItem(THEME_KEY, theme)
  }, [theme])

  async function refreshDashboard() {
    try {
      setDashboard(await getDashboard())
      setError('')
    } catch {
      setError('Inicie o backend Spring Boot em http://localhost:8080 para carregar os dados.')
    }
  }

  useEffect(() => { refreshDashboard() }, [])

  async function startSimulation() {
    try {
      setQuestions(await getQuestions(10))
      setAnswers({})
      setIndex(0)
      setResult(null)
      setPage('simulation')
      setError('')
    } catch {
      setError('Não foi possível iniciar. Verifique se o backend está executando.')
    }
  }

  async function finish() {
    if (answered !== questions.length) return
    const payload = questions.map(q => ({ questionId: q.id, selectedOptionIds: [answers[q.id]] }))
    const response = await finishSimulation(payload)
    setResult(response)
    await refreshDashboard()
  }

  const resultMap = useMemo(() => new Map(result?.results.map(r => [r.questionId, r]) ?? []), [result])

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand"><Cloud size={28}/><span>SisAWS</span></div>
        <nav>
          <button className={page === 'dashboard' ? 'active' : ''} onClick={() => setPage('dashboard')}><Gauge/> Dashboard</button>
          <button className={page === 'simulation' ? 'active' : ''} onClick={startSimulation}><BookOpenCheck/> Simulados</button>
          <button disabled><Award/> Certificações</button>
          <button disabled><History/> Histórico</button>
        </nav>
        <div className="version">v1.1 • AWS Learning Lab</div>
      </aside>

      <main>
        <header>
          <div className="header-title">
            <p className="eyebrow">AWS LEARNING PLATFORM</p>
            <h1>{page === 'dashboard' ? 'Seu progresso na AWS' : 'Simulado SAA-C03'}</h1>
          </div>

          <div className="header-actions">
            <div className="theme-switcher" role="group" aria-label="Tema da interface">
              <button className={theme === 'light' ? 'active' : ''} onClick={() => setTheme('light')} aria-pressed={theme === 'light'}>☀ <span>Light</span></button>
              <button className={theme === 'moderate' ? 'active' : ''} onClick={() => setTheme('moderate')} aria-pressed={theme === 'moderate'}>◐ <span>Moderado</span></button>
              <button className={theme === 'dark' ? 'active' : ''} onClick={() => setTheme('dark')} aria-pressed={theme === 'dark'}>☾ <span>Dark</span></button>
            </div>
            <span className="badge">Java 21 + Spring Boot</span>
          </div>
        </header>

        {error && <div className="alert">{error}</div>}

        {page === 'dashboard' && (
          <div className="page-content dashboard-content">
            <section className="hero">
              <div>
                <span className="pill">AWS Certified Solutions Architect</span>
                <h2>Prepare-se construindo, respondendo e revisando.</h2>
                <p>O MVP começa com SAA-C03 e será expandido com trilhas, laboratórios, caderno de erros, flashcards e modo adaptativo.</p>
                <button className="primary" onClick={startSimulation}><PlayCircle/> Iniciar simulado de 10 questões</button>
              </div>
              <div className="hero-mark">AWS</div>
            </section>

            <section className="cards">
              <Metric label="Banco de questões" value={dashboard?.questionBank ?? 0} suffix=" questões" />
              <Metric label="Simulados feitos" value={dashboard?.attempts ?? 0} />
              <Metric label="Média" value={dashboard?.averageScore ?? 0} suffix="%" />
              <Metric label="Melhor nota" value={dashboard?.bestScore ?? 0} suffix="%" />
            </section>

            <section className="panel">
              <div>
                <p className="eyebrow">PRÓXIMA ETAPA</p>
                <h3>Domine arquitetura por cenários</h3>
                <p>IAM → VPC → EC2 → S3 → RDS → Serverless → Mensageria → Containers.</p>
              </div>
              <div className="domain-list">
                <span>Segurança</span><span>Resiliência</span><span>Performance</span><span>Custos</span>
              </div>
            </section>
          </div>
        )}

        {page === 'simulation' && current && !result && (
          <div className="page-content simulation-content">
            <section className="quiz-card">
              <div className="quiz-top">
                <div><span className="pill">{current.awsService}</span><span className="difficulty">{current.difficulty}</span></div>
                <strong>{index + 1} / {questions.length}</strong>
              </div>
              <div className="progress"><span style={{width: `${progress}%`}} /></div>
              <p className="domain">{current.domain}</p>
              <h2>{current.prompt}</h2>
              <div className="options">
                {current.options.map((option, i) => (
                  <button key={option.id}
                    className={answers[current.id] === option.id ? 'selected' : ''}
                    onClick={() => setAnswers({...answers, [current.id]: option.id})}>
                    <b>{String.fromCharCode(65 + i)}</b>{option.text}
                  </button>
                ))}
              </div>
              <div className="quiz-actions">
                <button disabled={index === 0} onClick={() => setIndex(index - 1)}>Anterior</button>
                {index < questions.length - 1
                  ? <button className="primary" onClick={() => setIndex(index + 1)}>Próxima</button>
                  : <button className="primary" disabled={answered !== questions.length} onClick={finish}>Finalizar simulado</button>}
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
              <p>Revise as explicações antes do próximo simulado.</p>
              <div className="review-list">
                {questions.map((q, idx) => {
                  const r = resultMap.get(q.id)
                  return <div className={r?.correct ? 'review ok' : 'review fail'} key={q.id}>
                    <strong>Questão {idx + 1} • {q.awsService}</strong>
                    <span>{r?.correct ? 'Correta' : 'Revisar'}</span>
                    <p>{r?.explanation}</p>
                  </div>
                })}
              </div>
              <div className="quiz-actions">
                <button onClick={() => setPage('dashboard')}>Voltar ao dashboard</button>
                <button className="primary" onClick={startSimulation}>Novo simulado</button>
              </div>
            </section>
          </div>
        )}
      </main>
    </div>
  )
}

function Metric({label, value, suffix = ''}: {label: string; value: number; suffix?: string}) {
  return <div className="metric"><span>{label}</span><strong>{value}{suffix}</strong></div>
}
