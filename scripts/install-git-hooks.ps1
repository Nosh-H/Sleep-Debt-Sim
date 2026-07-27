Param()

# Installer for git hooks stored in .githooks/
# Run from the repository root: pwsh ./scripts/install-git-hooks.ps1

$repoRoot = Resolve-Path "."
$githooksDir = Join-Path $repoRoot ".githooks"
$gitHooksTarget = Join-Path $repoRoot ".git\hooks"

if (-not (Test-Path $githooksDir)) {
    Write-Error ".githooks directory not found. Run from repository root."
    exit 1
}
if (-not (Test-Path $gitHooksTarget)) {
    Write-Error ".git/hooks not found. Is this a git repository?"
    exit 1
}

$srcHook = Join-Path $githooksDir "pre-commit"
$dstHook = Join-Path $gitHooksTarget "pre-commit"

if (-not (Test-Path $srcHook)) {
    Write-Error "Source pre-commit hook not found at $srcHook"
    exit 1
}

Copy-Item -Path $srcHook -Destination $dstHook -Force
# Ensure the hook is executable where relevant
try {
    & chmod +x $dstHook
} catch {
    # chmod may not exist on Windows; ignore errors
}

Write-Host "Installed git hooks to .git/hooks (pre-commit)."
Write-Host "Note: Ensure you run this installer after cloning to enable the hooks locally."
