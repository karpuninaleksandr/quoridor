$ErrorActionPreference = "Stop"

$RootDir = Resolve-Path "$PSScriptRoot\.."
$AppName = "Quoridor"
$JarName = "quoridor-1.0.0-SNAPSHOT.jar"

Set-Location $RootDir
mvn clean package -Pproduction

if (Test-Path "target\jpackage-input") {
    Remove-Item "target\jpackage-input" -Recurse -Force
}
New-Item -ItemType Directory -Force -Path "target\jpackage-input" | Out-Null
Copy-Item "target\$JarName" "target\jpackage-input\$JarName"

if (Test-Path "dist\windows") {
    Remove-Item "dist\windows" -Recurse -Force
}
New-Item -ItemType Directory -Force -Path "dist\windows" | Out-Null

jpackage `
    --type app-image `
    --name $AppName `
    --app-version 1.0.0 `
    --input "target\jpackage-input" `
    --main-jar $JarName `
    --arguments "--quoridor.desktop=true" `
    --dest "dist\windows" `
    --win-console false

Copy-Item "distribution\README.txt" "dist\windows\$AppName\README.txt"

$ZipPath = "dist\windows\$AppName-windows.zip"
if (Test-Path $ZipPath) {
    Remove-Item $ZipPath -Force
}
Compress-Archive -Path "dist\windows\$AppName" -DestinationPath $ZipPath

Write-Host "Готово: $ZipPath"
