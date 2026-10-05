#Requires -Version 5.1
<#
	Better GUI - manual build script (Minecraft 1.20.6)

	Compiles the sources with javac, remaps them from Yarn names to intermediary with
	tiny-remapper and packs the jar by hand. This is a fallback for machines where the Gradle
	build is unavailable or does not support this Minecraft version yet.

	Requirements:
	  * JDK 21 or newer (either set JAVA_HOME, or have javac on PATH)
	  * a Gradle / Fabric-Loom cache for Minecraft 1.20.6 - run `gradlew build` once on
	    this machine, or copy the cache over from one that has
	  * network access the first time, unless the Mod Menu jar is already around: it is
	    downloaded into libs/ (which is git-ignored) when it cannot be found locally

	Usage:
	  powershell -ExecutionPolicy Bypass -File build-manual.ps1
	  powershell -ExecutionPolicy Bypass -File build-manual.ps1 -ModsDir "C:\path\to\mods"
#>
param(
	[string]$ModsDir = ""
)

$ErrorActionPreference = "Stop"

$mcVersion = "1.20.6"
$modVersion = "1.0.0"
$javaRelease = "21"
$modMenuVersion = "11.0.0"
$cache = Join-Path $env:USERPROFILE ".gradle\caches"
$root = $PSScriptRoot

# --- toolchain ----------------------------------------------------------------------------------
# Any JDK of the right version will do, so look at JAVA_HOME, PATH and the usual install
# directories and keep the newest one - `javac` on PATH is often an older JDK than the one this
# Minecraft version needs.
function Get-JdkVersion([string]$jdkHome) {
	$releaseFile = Join-Path $jdkHome "release"
	if (Test-Path $releaseFile) {
		$match = [regex]::Match((Get-Content -LiteralPath $releaseFile -Raw), 'JAVA_VERSION="(\d+)')
		if ($match.Success) { return [int]$match.Groups[1].Value }
	}
	return 0
}

$candidates = @()
if ($env:JAVA_HOME) { $candidates += $env:JAVA_HOME }
$javacOnPath = Get-Command javac -ErrorAction SilentlyContinue
if ($javacOnPath) { $candidates += (Split-Path (Split-Path $javacOnPath.Source -Parent) -Parent) }
$candidates += Get-ChildItem "C:\Program Files\Eclipse Adoptium", "C:\Program Files\Java",
	"C:\Program Files\Microsoft", "C:\Program Files\Zulu" -Directory -ErrorAction SilentlyContinue |
	Where-Object { $_.Name -match "jdk|jre" } | Select-Object -ExpandProperty FullName

$javaHome = $candidates |
	Where-Object { $_ -and (Test-Path (Join-Path $_ "bin\javac.exe")) } |
	Sort-Object { Get-JdkVersion $_ } -Descending |
	Select-Object -First 1
if (-not $javaHome -or (Get-JdkVersion $javaHome) -lt [int]$javaRelease) {
	throw "No JDK $javaRelease or newer found. Install one, or set JAVA_HOME to it."
}

$javac = Join-Path $javaHome "bin\javac.exe"
$java = Join-Path $javaHome "bin\java.exe"
$jarTool = Join-Path $javaHome "bin\jar.exe"
Write-Host "using JDK $(Get-JdkVersion $javaHome) at $javaHome"

# --- Fabric Loom cache --------------------------------------------------------------------------
$named = Get-ChildItem (Join-Path $cache "fabric-loom\minecraftMaven\net\minecraft\minecraft-merged") -Recurse -Filter "*$mcVersion*.jar" -ErrorAction SilentlyContinue |
	Where-Object { $_.Name -notmatch "sources" } | Select-Object -First 1 -ExpandProperty FullName
$inter = Get-ChildItem (Join-Path $cache "fabric-loom\minecraftMaven\net\minecraft\minecraft-merged-intermediary") -Recurse -Filter "*$mcVersion*.jar" -ErrorAction SilentlyContinue |
	Where-Object { $_.Name -notmatch "sources" } | Select-Object -First 1 -ExpandProperty FullName
$yarnMappings = Get-ChildItem (Join-Path $cache "fabric-loom\$mcVersion") -Recurse -Filter "mappings.tiny" -ErrorAction SilentlyContinue |
	Select-Object -First 1 -ExpandProperty FullName
if (-not $named -or -not $inter -or -not $yarnMappings) {
	throw "No Fabric Loom cache for Minecraft $mcVersion under '$cache'. Run 'gradlew build' once first."
}
$libs = Get-ChildItem (Join-Path $cache "modules-2\files-2.1") -Recurse -Filter *.jar -ErrorAction SilentlyContinue |
	Where-Object { $_.Name -notmatch "sources|javadoc|asm-debug-all" } | Select-Object -ExpandProperty FullName

# The cache can hold several asm / tiny-remapper versions; the newest one has to come first,
# otherwise an older asm is picked up and the remapper dies on modern class files.
$remapper = @($libs | Where-Object { $_ -match "tiny-remapper-0\.10\.0" }) +
	@($libs | Where-Object { $_ -match "asm-9\.6|asm-tree-9\.6|asm-analysis-9\.6|asm-commons-9\.6|asm-util-9\.6" }) +
	@($libs | Where-Object { $_ -notmatch "tiny-remapper|asm" })

# --- Mod Menu (optional dependency; needed to compile the modmenu entrypoint) -------------------
$libsDir = Join-Path $root "libs"
$modMenu = $null
if (Test-Path $libsDir) {
	$modMenu = Get-ChildItem $libsDir -Filter "*modmenu*.jar" -ErrorAction SilentlyContinue |
		Select-Object -First 1 -ExpandProperty FullName
}
if (-not $modMenu) {
	$modMenu = $libs | Where-Object { $_ -match "modmenu" } | Select-Object -First 1
}
if (-not $modMenu) {
	$modMenu = Join-Path $libsDir "modmenu-$modMenuVersion.jar"
	$url = "https://maven.terraformersmc.com/releases/com/terraformersmc/modmenu/$modMenuVersion/modmenu-$modMenuVersion.jar"
	New-Item -ItemType Directory -Force -Path $libsDir | Out-Null
	Write-Host "downloading $url"
	Invoke-WebRequest -Uri $url -OutFile $modMenu -UseBasicParsing
}

$refmap = Join-Path $root "refmap\bettergui-refmap.json"
$shadowFile = Join-Path $root "refmap\shadow-members.tiny"
if (-not (Test-Path $refmap)) {
	throw "missing $refmap"
}

# --- stage 1: compile everything except the optional Mod Menu entrypoint ------------------------
$work = Join-Path $root "build\manual"
Remove-Item -Recurse -Force $work -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path "$work\classes", "$work\classes2", "$work\final" | Out-Null

$stage1 = Get-ChildItem (Join-Path $root "src\main\java") -Recurse -Filter *.java |
	Where-Object { $_.FullName -notmatch "compat" } | Select-Object -ExpandProperty FullName
& $javac -proc:none -encoding UTF-8 --release $javaRelease -nowarn -cp ((@($named) + $libs) -join ";") -d "$work\classes" $stage1
if ($LASTEXITCODE -ne 0) { throw "javac stage1 failed with exit code $LASTEXITCODE" }
& $jarTool cf "$work\classes.jar" -C "$work\classes" .

# --- remap the compiled classes to intermediary -------------------------------------------------
$combined = "$work\combined-mappings.tiny"
Copy-Item -LiteralPath $yarnMappings -Destination $combined -Force
if (Test-Path $shadowFile) {
	$shadowLines = (Get-Content -LiteralPath $shadowFile | Select-Object -Skip 1) -join "`r`n"
	[System.IO.File]::AppendAllText($combined, $shadowLines + "`r`n", (New-Object System.Text.UTF8Encoding($false)))
}

& $java -cp ($remapper -join ";") net.fabricmc.tinyremapper.Main "$work\classes.jar" "$work\classes-inter.jar" $combined named intermediary $named
if ($LASTEXITCODE -ne 0) { throw "tinyremapper failed with exit code $LASTEXITCODE" }

# --- stage 2: the Mod Menu entrypoint, compiled against the remapped classes --------------------
$stage2 = Join-Path $root "src\main\java\com\bettergui\compat\BetterGuiModMenu.java"
& $javac -proc:none -encoding UTF-8 --release $javaRelease -nowarn -cp ((@($inter, "$work\classes-inter.jar", $modMenu) + $libs) -join ";") -d "$work\classes2" $stage2
if ($LASTEXITCODE -ne 0) { throw "javac stage2 failed with exit code $LASTEXITCODE" }

# --- package the jar ----------------------------------------------------------------------------
Add-Type -AssemblyName System.IO.Compression.FileSystem
[System.IO.Compression.ZipFile]::ExtractToDirectory("$work\classes-inter.jar", "$work\final")
Copy-Item -Recurse -Force "$work\classes2\com" "$work\final"
Copy-Item -Recurse -Force (Join-Path $root "src\main\resources\*") "$work\final"
Copy-Item -Force $refmap "$work\final\bettergui-refmap.json"
$modJson = Join-Path $work "final\fabric.mod.json"
[System.IO.File]::WriteAllText($modJson, ([System.IO.File]::ReadAllText($modJson).Replace('${version}', $modVersion)), (New-Object System.Text.UTF8Encoding($false)))

$out = Join-Path $root "build\libs\bettergui-1.0.0+mc1.20.6.jar"
New-Item -ItemType Directory -Force -Path (Split-Path $out) | Out-Null
& $jarTool cf $out -C "$work\final" .

Write-Host "built: $out"
if ($ModsDir) {
	Copy-Item -LiteralPath $out -Destination $ModsDir -Force
	Write-Host "copied to: $ModsDir"
}
