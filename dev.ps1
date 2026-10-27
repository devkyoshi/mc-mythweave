<#
.SYNOPSIS
  Mythweave dev launcher: build the mod and open Minecraft with it, in one command.

.DESCRIPTION
  Run it from anywhere (or double-click dev.cmd for a menu):

    .\dev                 menu
    .\dev client          Minecraft with the mod, as Player1 (singleplayer)
    .\dev client2         a second Minecraft, as Player2
    .\dev server          the dedicated server only
    .\dev multiplayer     server + Player1 + Player2, each in its own window;
                          both clients join the server automatically
    .\dev build           build the JAR into build\libs
    .\dev help            this text

  Finds JDK 25 by itself (JAVA_HOME, then the usual install folders).
  The first time you start the server it asks you to accept the Minecraft EULA,
  sets online-mode=false (dev clients have offline accounts), and makes
  Player1 and Player2 operators so commands like /summon work.
#>
param(
	[Parameter(Position = 0)][string]$Command = '',
	# Internal: used by the multiplayer windows so the clients join the local server.
	[switch]$Join
)

$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

$ServerDir = Join-Path $PSScriptRoot 'run-server'
$ServerPort = 25565
$Players = 'Player1', 'Player2'

function Test-Jdk25([string]$dir) {
	$release = Join-Path $dir 'release'
	return (Test-Path $release) -and [bool]((Get-Content $release) -match '^JAVA_VERSION="25')
}

function Use-Jdk25 {
	if ($env:JAVA_HOME -and (Test-Jdk25 $env:JAVA_HOME)) { return }
	$candidates = 'C:\Program Files\Microsoft', 'C:\Program Files\Eclipse Adoptium', 'C:\Program Files\Java' |
		Where-Object { Test-Path $_ } |
		ForEach-Object { Get-ChildItem $_ -Directory -Filter 'jdk-25*' } |
		Sort-Object Name -Descending
	foreach ($jdk in $candidates) {
		if (Test-Jdk25 $jdk.FullName) {
			$env:JAVA_HOME = $jdk.FullName
			return
		}
	}
	throw 'JDK 25 not found. Install it (winget install Microsoft.OpenJDK.25) or set JAVA_HOME.'
}

function Invoke-Gradle([string[]]$arguments) {
	Use-Jdk25
	& .\gradlew.bat @arguments --console=plain
	if ($LASTEXITCODE -ne 0) { throw "gradlew $($arguments -join ' ') failed (exit code $LASTEXITCODE)." }
}

# Same rule as Minecraft's UUIDUtil.createOfflinePlayerUUID: name-based UUID (version 3)
# of "OfflinePlayer:<name>".
function Get-OfflineUuid([string]$name) {
	$md5 = [Security.Cryptography.MD5]::Create()
	$b = $md5.ComputeHash([Text.Encoding]::UTF8.GetBytes("OfflinePlayer:$name"))
	$b[6] = ($b[6] -band 0x0f) -bor 0x30
	$b[8] = ($b[8] -band 0x3f) -bor 0x80
	$hex = -join ($b | ForEach-Object { $_.ToString('x2') })
	return '{0}-{1}-{2}-{3}-{4}' -f $hex.Substring(0, 8), $hex.Substring(8, 4), $hex.Substring(12, 4), $hex.Substring(16, 4), $hex.Substring(20)
}

# Files are written as UTF-8 without a BOM, which Minecraft expects.
function Write-TextFile([string]$path, [string[]]$lines) {
	[IO.File]::WriteAllLines($path, $lines)
}

function Initialize-Server {
	New-Item -ItemType Directory -Force $ServerDir | Out-Null

	$eula = Join-Path $ServerDir 'eula.txt'
	if (-not ((Test-Path $eula) -and (Select-String -Path $eula -Pattern '^eula=true' -Quiet))) {
		Write-Host ''
		Write-Host 'The dev server needs you to accept the Minecraft EULA:'
		Write-Host '  https://aka.ms/MinecraftEULA'
		$answer = Read-Host 'Do you agree to it? (y/N)'
		if ($answer -notmatch '^(y|yes)$') { throw 'EULA not accepted, so the server was not started.' }
		Write-TextFile $eula @('eula=true')
	}

	# Keep everything else in server.properties; Minecraft fills in missing keys.
	$props = Join-Path $ServerDir 'server.properties'
	$lines = @()
	if (Test-Path $props) { $lines = @(Get-Content $props | Where-Object { $_ -notmatch '^online-mode=' }) }
	Write-TextFile $props ($lines + 'online-mode=false')

	$ops = Join-Path $ServerDir 'ops.json'
	if (-not (Test-Path $ops)) {
		$entries = foreach ($p in $Players) {
			[ordered]@{ uuid = Get-OfflineUuid $p; name = $p; level = 4; bypassesPlayerLimit = $false }
		}
		Write-TextFile $ops @(ConvertTo-Json -InputObject @($entries))
	}
}

function Test-ServerUp {
	$tcp = New-Object Net.Sockets.TcpClient
	try {
		$tcp.Connect('127.0.0.1', $ServerPort)
		return $true
	} catch {
		return $false
	} finally {
		$tcp.Close()
	}
}

# Runs one dev.ps1 command in a new PowerShell window that stays open afterwards.
function Start-Window([string]$title, [string[]]$arguments) {
	$script = "`$host.UI.RawUI.WindowTitle = '$title'; & '$PSCommandPath' $($arguments -join ' ')"
	Start-Process powershell -ArgumentList '-NoProfile', '-ExecutionPolicy', 'Bypass', '-NoExit', '-Command', "`"$script`""
}

function Start-Multiplayer {
	Initialize-Server
	if (Test-ServerUp) {
		Write-Host "A server is already running on port $ServerPort; using it."
	} else {
		Start-Window 'Mythweave server' @('server')
		Write-Host 'Starting the server (type commands like "stop" in its window)...'
		$deadline = (Get-Date).AddMinutes(5)
		while (-not (Test-ServerUp)) {
			if ((Get-Date) -gt $deadline) { throw 'The server did not open its port within 5 minutes. Check its window.' }
			Start-Sleep -Seconds 2
		}
	}
	Write-Host 'Server is up. Opening Player1 and Player2; they join it automatically.'
	Start-Window 'Mythweave Player1' @('client', '-Join')
	Start-Sleep -Seconds 5   # stagger so the two Gradle runs don't fight over startup
	Start-Window 'Mythweave Player2' @('client2', '-Join')
}

function Show-Menu {
	Write-Host ''
	Write-Host 'Mythweave dev launcher'
	Write-Host '  1) Minecraft (singleplayer, Player1)'
	Write-Host '  2) Multiplayer test (server + Player1 + Player2)'
	Write-Host '  3) Dedicated server only'
	Write-Host '  4) Second client (Player2)'
	Write-Host '  5) Build the JAR'
	$choice = Read-Host 'Pick 1-5'
	switch ($choice) {
		'1' { return 'client' }
		'2' { return 'multiplayer' }
		'3' { return 'server' }
		'4' { return 'client2' }
		'5' { return 'build' }
		default { throw "Unknown choice '$choice'." }
	}
}

$joinArg = @()
if ($Join) { $joinArg = @("-PjoinServer=localhost:$ServerPort") }

if ($Command -eq '') { $Command = Show-Menu }

switch ($Command.ToLower()) {
	'client' { Invoke-Gradle (@('runClient') + $joinArg) }
	'client2' { Invoke-Gradle (@('runClient2') + $joinArg) }
	'server' { Initialize-Server; Invoke-Gradle @('runServer') }
	{ $_ -in 'multiplayer', 'mp' } { Start-Multiplayer }
	'build' { Invoke-Gradle @('build') }
	{ $_ -in 'help', '-h', '--help', '/?' } { Get-Help $PSCommandPath -Detailed }
	default { throw "Unknown command '$Command'. Try: .\dev help" }
}
