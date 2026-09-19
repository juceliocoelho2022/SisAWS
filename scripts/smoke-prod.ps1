param(
    [string]$ApiBase = "http://sisaws-dev-api-688089831.sa-east-1.elb.amazonaws.com/api/v1",
    [string]$Email,
    [string]$Name = "SisAWS Smoke Test",
    [switch]$Register
)

$ErrorActionPreference = "Stop"

function Write-Step {
    param([string]$Message)
    Write-Host ""
    Write-Host "==> $Message" -ForegroundColor Cyan
}

function Invoke-SisAws {
    param(
        [Parameter(Mandatory)]
        [string]$Method,

        [Parameter(Mandatory)]
        [string]$Uri,

        [hashtable]$Headers,

        [object]$Body
    )

    $params = @{
        Method      = $Method
        Uri         = $Uri
        ErrorAction = "Stop"
    }

    if ($Headers) {
        $params.Headers = $Headers
    }

    if ($null -ne $Body) {
        $params.ContentType = "application/json"
        $params.Body = $Body | ConvertTo-Json -Depth 10
    }

    Invoke-RestMethod @params
}

if ([string]::IsNullOrWhiteSpace($Email)) {
    $Email = Read-Host "E-mail da conta SisAWS"
}

$securePassword = Read-Host "Senha da conta SisAWS" -AsSecureString
$password = [System.Net.NetworkCredential]::new("", $securePassword).Password

try {
    Write-Step "1/10 - Autenticacao"

    if ($Register) {
        $auth = Invoke-SisAws -Method POST -Uri "$ApiBase/auth/register" -Body @{
            name     = $Name
            email    = $Email
            password = $password
        }
    }
    else {
        $auth = Invoke-SisAws -Method POST -Uri "$ApiBase/auth/login" -Body @{
            email    = $Email
            password = $password
        }
    }

    if ([string]::IsNullOrWhiteSpace($auth.token)) {
        throw "A API nao retornou um JWT."
    }

    $headers = @{
        Authorization = "Bearer $($auth.token)"
    }

    Write-Host "OK - usuario autenticado: $($auth.user.email)" -ForegroundColor Green

    Write-Step "2/10 - JWT e /auth/me"
    $me = Invoke-SisAws -Method GET -Uri "$ApiBase/auth/me" -Headers $headers
    Write-Host "OK - $($me.name) / role=$($me.role)" -ForegroundColor Green

    Write-Step "3/10 - Certificacoes"
    $certifications = @(Invoke-SisAws -Method GET -Uri "$ApiBase/certifications" -Headers $headers)
    if ($certifications.Count -lt 1) {
        throw "Nenhuma certificacao foi retornada."
    }
    Write-Host "OK - certificacoes: $($certifications.Count)" -ForegroundColor Green

    Write-Step "4/10 - Banco de questoes"
    $questions = @(Invoke-SisAws -Method GET -Uri "$ApiBase/questions?certification=SAA-C03&limit=2" -Headers $headers)
    if ($questions.Count -lt 2) {
        throw "O smoke test precisa de pelo menos 2 questoes."
    }
    Write-Host "OK - 2 questoes carregadas" -ForegroundColor Green

    Write-Step "5/10 - Correcao de resposta"
    $firstCheck = Invoke-SisAws -Method POST -Uri "$ApiBase/questions/$($questions[0].id)/check" -Headers $headers -Body @{
        selectedOptionIds = @()
    }

    if (@($firstCheck.correctOptionIds).Count -lt 1) {
        throw "A API nao retornou a alternativa correta da primeira questao."
    }

    Write-Host "OK - endpoint de correcao respondeu" -ForegroundColor Green

    Write-Step "6/10 - Finalizar mini-simulado"
    $simulation = Invoke-SisAws -Method POST -Uri "$ApiBase/simulations/finish" -Headers $headers -Body @{
        certificationCode = "SAA-C03"
        answers = @(
            @{
                questionId        = $questions[0].id
                selectedOptionIds = @($firstCheck.correctOptionIds)
            },
            @{
                questionId        = $questions[1].id
                selectedOptionIds = @()
            }
        )
    }

    if ($simulation.totalQuestions -ne 2) {
        throw "O simulado nao registrou as 2 questoes esperadas."
    }

    Write-Host "OK - tentativa=$($simulation.attemptId), score=$($simulation.scorePercent)%" -ForegroundColor Green

    Write-Step "7/10 - Historico e dashboard"
    $history = @(Invoke-SisAws -Method GET -Uri "$ApiBase/simulations/history" -Headers $headers)
    $dashboard = Invoke-SisAws -Method GET -Uri "$ApiBase/dashboard" -Headers $headers

    Write-Host "OK - historico=$($history.Count), tentativas=$($dashboard.attempts), melhor=$($dashboard.bestScore)%" -ForegroundColor Green

    Write-Step "8/10 - Progresso e recomendacao"
    $progress = @(Invoke-SisAws -Method GET -Uri "$ApiBase/learning/progress" -Headers $headers)
    $recommendation = Invoke-SisAws -Method GET -Uri "$ApiBase/learning/recommendation" -Headers $headers

    Write-Host "OK - servicos acompanhados=$($progress.Count), recomendacao=$($recommendation.awsService)" -ForegroundColor Green

    Write-Step "9/10 - Plano adaptativo"
    $adaptive = Invoke-SisAws -Method GET -Uri "$ApiBase/learning/adaptive/plan" -Headers $headers
    Write-Host "OK - nivel=$($adaptive.overallLevel), proximo=$($adaptive.nextAction.awsService)" -ForegroundColor Green

    Write-Step "10/10 - Caderno de erros"
    $errors = @(Invoke-SisAws -Method GET -Uri "$ApiBase/error-notebook" -Headers $headers)
    if ($errors.Count -lt 1) {
        throw "O caderno de erros deveria conter pelo menos uma entrada apos a resposta propositalmente incorreta."
    }

    Write-Host "OK - entradas no caderno de erros: $($errors.Count)" -ForegroundColor Green

    Write-Host ""
    Write-Host "============================================" -ForegroundColor Green
    Write-Host "SISAWS PRODUCTION SMOKE TEST: SUCCESS" -ForegroundColor Green
    Write-Host "============================================" -ForegroundColor Green
    Write-Host "API:          $ApiBase"
    Write-Host "Usuario:      $($me.email)"
    Write-Host "Questões:     $($dashboard.questionBank)"
    Write-Host "Tentativas:   $($dashboard.attempts)"
    Write-Host "Melhor score: $($dashboard.bestScore)%"
    Write-Host "Erros:        $($dashboard.errorNotebookCount)"
}
catch {
    Write-Host ""
    Write-Host "SISAWS PRODUCTION SMOKE TEST: FAILED" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    exit 1
}
finally {
    $password = $null
    $securePassword = $null
}
