# Installation script for Git hooks (PowerShell)
# Run this script from the project root to install pre-commit hooks

Write-Host "Installing Git hooks..." -ForegroundColor Cyan

$ProjectRoot = git rev-parse --show-toplevel
$HooksSourceDir = Join-Path $ProjectRoot "hooks"
$HooksTargetDir = Join-Path $ProjectRoot ".git\hooks"

# Check if source hooks directory exists
if (-not (Test-Path $HooksSourceDir)) {
    Write-Host "Error: hooks/ directory not found!" -ForegroundColor Red
    exit 1
}

# Create .git/hooks directory if it doesn't exist
if (-not (Test-Path $HooksTargetDir)) {
    New-Item -ItemType Directory -Path $HooksTargetDir -Force | Out-Null
}

# Copy pre-commit hook
$PreCommitSource = Join-Path $HooksSourceDir "pre-commit"
$PreCommitTarget = Join-Path $HooksTargetDir "pre-commit"

if (Test-Path $PreCommitSource) {
    Copy-Item $PreCommitSource $PreCommitTarget -Force
    Write-Host "✓ Installed pre-commit hook" -ForegroundColor Green
} else {
    Write-Host "Warning: pre-commit hook not found in hooks/ directory" -ForegroundColor Yellow
}

# Copy pre-commit.ps1 hook
$PreCommitPs1Source = Join-Path $HooksSourceDir "pre-commit.ps1"
$PreCommitPs1Target = Join-Path $HooksTargetDir "pre-commit.ps1"

if (Test-Path $PreCommitPs1Source) {
    Copy-Item $PreCommitPs1Source $PreCommitPs1Target -Force
    Write-Host "✓ Installed pre-commit.ps1 hook" -ForegroundColor Green
} else {
    Write-Host "Warning: pre-commit.ps1 hook not found in hooks/ directory" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Git hooks installed successfully!" -ForegroundColor Green
Write-Host ""
Write-Host "The pre-commit hook will now run automatically before each commit." -ForegroundColor Cyan
Write-Host "It will check your code for quality issues and block commits if problems are found." -ForegroundColor Cyan
Write-Host ""
Write-Host "OPTIONAL: Enable GitHub Copilot CLI hooks for AI-powered reviews" -ForegroundColor Yellow
Write-Host "  See hooks/COPILOT_CLI_HOOKS.md for setup instructions" -ForegroundColor Gray
Write-Host ""
Write-Host "To bypass the hooks (not recommended), use: git commit --no-verify" -ForegroundColor Gray
Write-Host ""
