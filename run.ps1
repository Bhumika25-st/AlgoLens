param(
    [switch]$TestOnly
)

$ErrorActionPreference = "Stop"

$Root = $PSScriptRoot
$MainSrc = Join-Path $Root "src\main\java"
$TestSrc = Join-Path $Root "src\test\java"
$LibDir = Join-Path $Root "lib"
$OutMain = Join-Path $Root "out\main"
$OutTest = Join-Path $Root "out\test"

$LibJars = @()
if (Test-Path $LibDir) {
    $LibJars = Get-ChildItem -Path $LibDir -Filter *.jar | ForEach-Object { $_.FullName }
}
$LibClasspath = $LibJars -join ";"

$JavaFlags = @()
$VersionText = (& cmd /c "java -version 2>&1") -join " "
if ($VersionText -match '"(\d+)') {
    if ([int]$Matches[1] -ge 24) {
        $JavaFlags += "--enable-native-access=ALL-UNNAMED"
    }
}

New-Item -ItemType Directory -Force -Path $OutMain | Out-Null
$MainFiles = Get-ChildItem -Path $MainSrc -Filter *.java -Recurse | ForEach-Object { $_.FullName }
if ($LibClasspath) {
    & javac -encoding UTF-8 -cp $LibClasspath -d $OutMain $MainFiles
} else {
    & javac -encoding UTF-8 -d $OutMain $MainFiles
}
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

if ($TestOnly) {
    New-Item -ItemType Directory -Force -Path $OutTest | Out-Null
    $TestFiles = Get-ChildItem -Path $TestSrc -Filter *.java -Recurse | ForEach-Object { $_.FullName }
    $CompileTestCp = if ($LibClasspath) { "$OutMain;$LibClasspath" } else { $OutMain }
    $RunTestCp = if ($LibClasspath) { "$OutMain;$OutTest;$LibClasspath" } else { "$OutMain;$OutTest" }
    & javac -encoding UTF-8 -cp $CompileTestCp -d $OutTest $TestFiles
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

    & java @JavaFlags -cp $RunTestCp com.algolens.SortingEngineTest
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

    & java @JavaFlags -cp $RunTestCp com.algolens.SearchEngineTest
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
} else {
    $RunCp = if ($LibClasspath) { "$OutMain;$LibClasspath" } else { $OutMain }
    & java @JavaFlags -cp $RunCp com.algolens.app.AlgoLensApp
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}
