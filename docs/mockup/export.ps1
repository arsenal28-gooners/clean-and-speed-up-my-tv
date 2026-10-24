# Export the public mockup to PNGs in docs/screenshots with a headless Edge or Chrome (Windows).
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$out = Join-Path (Split-Path -Parent $here) 'screenshots'
New-Item -ItemType Directory -Force $out | Out-Null
$browser = @(
  "${env:ProgramFiles(x86)}\Microsoft\Edge\Application\msedge.exe",
  "$env:ProgramFiles\Microsoft\Edge\Application\msedge.exe",
  "$env:ProgramFiles\Google\Chrome\Application\chrome.exe"
) | Where-Object { Test-Path $_ } | Select-Object -First 1
$base = 'file:///' + ($here -replace '\\', '/')

function shot([string]$page, [string]$name, [int]$w = 1920, [int]$h = 1080) {
  $png = Join-Path $out "$name.png"
  & $browser --headless=new --disable-gpu --hide-scrollbars --allow-file-access-from-files --virtual-time-budget=3000 `
    "--window-size=$w,$h" "--screenshot=$png" "$base/$page" 2>$null | Out-Null
  "{0,-22} {1,8:N0} KB" -f "$name.png", ((Get-Item $png).Length / 1KB)
}

shot 'index.html?view=home&phase=malam' 'home'
shot 'index.html?view=apps&phase=malam' 'all-apps'
shot 'index.html?view=idle&phase=pagi' 'idle'
shot 'times.html' 'times-home' 2000 1260
shot 'times.html?view=idle' 'times-idle' 2000 1260
