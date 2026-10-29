# Creates the UV Glow upload keystore for Google Play and prints the GitHub Actions secrets the
# Release AAB workflow needs. Run from the repo folder:
#   powershell -ExecutionPolicy Bypass -File scripts\make-upload-keystore.ps1
# The keystore goes to your home folder, never into the repo. The password is never printed.

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$keystore = Join-Path $HOME 'upload-keystore.jks'
$base64File = Join-Path $HOME 'keystore-base64.txt'
$secretsUrl = 'https://github.com/shahidothman06-tech/UV-GLow/settings/secrets/actions'

function Find-Keytool {
    if ($env:JAVA_HOME) {
        $k = Join-Path $env:JAVA_HOME 'bin\keytool.exe'
        if (Test-Path $k) { return $k }
    }
    $k = 'C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe'
    if (Test-Path $k) { return $k }
    Write-Host 'keytool was not found. Set JAVA_HOME to a JDK, or install Android Studio in its default folder.' -ForegroundColor Red
    exit 1
}

function To-Plain([Security.SecureString]$s) {
    $b = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($s)
    try { [Runtime.InteropServices.Marshal]::PtrToStringBSTR($b) } finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($b) }
}

$keytool = Find-Keytool

if (Test-Path $keystore) {
    Write-Host "$keystore already exists. Not overwriting an upload key; move it away first if you really want a new one." -ForegroundColor Red
    exit 1
}

# ---- questions ---------------------------------------------------------------
$alias = Read-Host 'Key alias (Enter for "uvglow-upload")'
if ([string]::IsNullOrWhiteSpace($alias)) { $alias = 'uvglow-upload' }
$alias = $alias.Trim()

while ($true) {
    $password = To-Plain (Read-Host 'Keystore password (at least 6 characters)' -AsSecureString)
    $again = To-Plain (Read-Host 'Type the password again' -AsSecureString)
    if ($password -ne $again) { Write-Host 'Passwords do not match, try again.' -ForegroundColor Yellow; continue }
    if ($password.Length -lt 6) { Write-Host 'Too short, try again.' -ForegroundColor Yellow; continue }
    break
}

$productId = ''
while ($productId -eq '') {
    $productId = (Read-Host 'Premium monthly product ID (exactly as in Play Console, e.g. uvglow_premium_monthly)').Trim()
    if ($productId -notmatch '^[a-z0-9][a-z0-9._]*$') {
        Write-Host 'Product IDs use only lowercase letters, numbers, "_" and ".". Try again.' -ForegroundColor Yellow
        $productId = ''
    }
}

# ---- keystore ----------------------------------------------------------------
# The password goes to keytool through an environment variable (:env), so it never appears on a
# command line or on screen.
$env:UVGLOW_TMP_PW = $password
try {
    & $keytool -genkeypair -v -keystore $keystore -storetype JKS -alias $alias `
        -keyalg RSA -keysize 2048 -validity 10000 `
        -dname 'CN=UV Glow, O=TubersTech, C=PK' `
        -storepass:env UVGLOW_TMP_PW -keypass:env UVGLOW_TMP_PW
    if ($LASTEXITCODE -ne 0) { throw 'keytool failed to create the keystore.' }
} finally {
    Remove-Item Env:\UVGLOW_TMP_PW -ErrorAction SilentlyContinue
}
Write-Host "`nUpload keystore created: $keystore" -ForegroundColor Green

[IO.File]::WriteAllText($base64File, [Convert]::ToBase64String([IO.File]::ReadAllBytes($keystore)))
Write-Host "Base64 copy for GitHub: $base64File"

# ---- gradle.properties -------------------------------------------------------
$props = Join-Path $repoRoot 'gradle.properties'
$text = [IO.File]::ReadAllText($props)
if ($text -match '(?m)^uvglow\.billing\.monthlyProductId=.*$') {
    $text = [regex]::Replace($text, '(?m)^uvglow\.billing\.monthlyProductId=.*$', "uvglow.billing.monthlyProductId=$productId")
} else {
    $text = $text.TrimEnd() + "`nuvglow.billing.monthlyProductId=$productId`n"
}
[IO.File]::WriteAllText($props, $text, (New-Object Text.UTF8Encoding($false)))
Write-Host "gradle.properties: uvglow.billing.monthlyProductId=$productId"

# ---- what to do next ---------------------------------------------------------
Write-Host "`nAdd these repository secrets at:" -ForegroundColor Cyan
Write-Host "  $secretsUrl  (New repository secret)"
Write-Host "  UVGLOW_KEYSTORE_BASE64 = the whole contents of $base64File"
Write-Host "  UVGLOW_STORE_PASSWORD  = the password you just typed"
Write-Host "  UVGLOW_KEY_PASSWORD    = the same password"
Write-Host "  UVGLOW_KEY_ALIAS       = $alias"
Write-Host "Then delete $base64File (the .jks itself stays)."
Write-Host "`nCommit and push the gradle.properties change, then run GitHub > Actions > Release AAB."

Write-Host "`nIMPORTANT: back up $keystore and its password somewhere safe (password manager plus an" -ForegroundColor Yellow
Write-Host 'offline copy). Without them you cannot sign updates, and an upload-key reset through Play' -ForegroundColor Yellow
Write-Host 'support takes days. Never commit the .jks file.' -ForegroundColor Yellow
