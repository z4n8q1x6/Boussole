$src = "C:\Users\siwar\IdeaProjects\Boussole\src\main\resources"
$dst = "C:\Users\siwar\IdeaProjects\Boussole\target\classes"

if (-not (Test-Path $dst)) { New-Item -ItemType Directory -Path $dst -Force }

Get-ChildItem -Path $src -Recurse -Include "*.fxml","*.css" | ForEach-Object {
    $relative = $_.FullName.Substring($src.Length + 1)
    $destFile = Join-Path $dst $relative
    $destDir = Split-Path $destFile -Parent
    if (-not (Test-Path $destDir)) { New-Item -ItemType Directory -Path $destDir -Force }
    Copy-Item $_.FullName $destFile -Force
    [Console]::WriteLine("Copied: " + $relative)
}
[Console]::WriteLine("=== SYNC DONE ===")
