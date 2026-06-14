param(
    [switch]$RunSmoke,
    [string]$ApiBase = "http://localhost:8082/api",
    [string]$Email = "",
    [string]$Password = ""
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot

function Step($message) {
    Write-Host ""
    Write-Host "==> $message" -ForegroundColor Cyan
}

function Fail($message) {
    Write-Host ""
    Write-Host "PRE-FLIGHT FAILED: $message" -ForegroundColor Red
    exit 1
}

function Assert-Exists($path, $message) {
    if (!(Test-Path $path)) {
        Fail $message
    }
}

function Invoke-JsonRequest($method, $url, $headers = @{}, $body = $null) {
    $params = @{
        Method = $method
        Uri = $url
        Headers = $headers
        ContentType = "application/json"
    }
    if ($null -ne $body) {
        $params.Body = ($body | ConvertTo-Json -Depth 8)
    }
    return Invoke-RestMethod @params
}

function Test-WebSocket($apiBase, $token) {
    Add-Type -AssemblyName System.Net.WebSockets.Client
    $wsUrl = $apiBase -replace "^http", "ws"
    $wsUrl = "$wsUrl/realtime/ws?token=$([uri]::EscapeDataString($token))"

    $socket = [System.Net.WebSockets.ClientWebSocket]::new()
    $cts = [System.Threading.CancellationTokenSource]::new()
    $cts.CancelAfter([TimeSpan]::FromSeconds(8))

    try {
        $socket.ConnectAsync([uri]$wsUrl, $cts.Token).GetAwaiter().GetResult()
        if ($socket.State -ne [System.Net.WebSockets.WebSocketState]::Open) {
            Fail "WebSocket did not open. State: $($socket.State)"
        }
        $socket.CloseAsync(
            [System.Net.WebSockets.WebSocketCloseStatus]::NormalClosure,
            "preflight complete",
            [System.Threading.CancellationToken]::None
        ).GetAwaiter().GetResult()
        Write-Host "WebSocket handshake OK: $wsUrl" -ForegroundColor Green
    } finally {
        $socket.Dispose()
        $cts.Dispose()
    }
}

Step "Checking repository"
Set-Location $repoRoot
git status --short

Step "Building backend WAR"
Set-Location "$repoRoot\backend"
.\mvnw.cmd clean package -DskipTests
Assert-Exists "$repoRoot\backend\target\ROOT.war" "Backend WAR was not created."

Step "Building frontend production bundle"
Set-Location "$repoRoot\frontend"
npm run build

$browserDist = "$repoRoot\frontend\dist\angular\browser"
Assert-Exists "$browserDist\index.html" "Frontend production index.html was not created."

Step "Checking production frontend assets"
$distTextFiles = Get-ChildItem $browserDist -Recurse -File -Include *.js,*.css,*.html

$localhostHits = $distTextFiles | Select-String -Pattern "localhost:8080|localhost:8082|localhost:9090" -SimpleMatch
if ($localhostHits) {
    $localhostHits | Select-Object Path, LineNumber, Line | Format-List
    Fail "Production bundle contains localhost API references."
}

$eventSourceHits = $distTextFiles | Select-String -Pattern "EventSource|realtime/stream" -SimpleMatch
if ($eventSourceHits) {
    $eventSourceHits | Select-Object Path, LineNumber, Line | Format-List
    Fail "Production bundle still contains SSE/EventSource fallback."
}

$wsHits = $distTextFiles | Select-String -Pattern "realtime/ws" -SimpleMatch
if (!$wsHits) {
    Fail "Production bundle does not contain realtime WebSocket endpoint."
}

Write-Host "Production asset checks OK" -ForegroundColor Green

if ($RunSmoke) {
    Step "Running API smoke test against $ApiBase"
    if ([string]::IsNullOrWhiteSpace($Email) -or [string]::IsNullOrWhiteSpace($Password)) {
        Fail "Provide -Email and -Password when using -RunSmoke."
    }

    $login = Invoke-JsonRequest "POST" "$ApiBase/auth/login" @{} @{ email = $Email; password = $Password }
    if (!$login.success -or [string]::IsNullOrWhiteSpace($login.token)) {
        Fail "Login smoke test failed."
    }
    $headers = @{ Authorization = "Bearer $($login.token)" }

    Invoke-JsonRequest "GET" "$ApiBase/auth/me" $headers | Out-Null
    Invoke-JsonRequest "GET" "$ApiBase/notifications" $headers | Out-Null
    Invoke-JsonRequest "GET" "$ApiBase/realtime/status?token=$([uri]::EscapeDataString($login.token))" @{} | Out-Null
    Test-WebSocket $ApiBase $login.token

    Write-Host "API smoke checks OK" -ForegroundColor Green
}

Set-Location $repoRoot
Write-Host ""
Write-Host "PRE-FLIGHT PASSED" -ForegroundColor Green
