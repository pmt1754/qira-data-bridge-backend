# Pre-commit hook with GitHub Copilot CLI protocol enforcement (PowerShell)
# This hook runs automated protocol checks using GitHub Copilot before allowing commits
# Blocks commits with critical issues; allows commits with minor suggestions

$ErrorActionPreference = "Stop"

Write-Host "╔════════════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║          GitHub Copilot Pre-Commit Quality Gate              ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

# Get the project root directory
$ProjectRoot = git rev-parse --show-toplevel
$ReportDir = Join-Path $ProjectRoot ".git\hooks\reports"
$ReportFile = Join-Path $ReportDir "copilot-precommit-report.txt"

# Create report directory if it doesn't exist
if (-not (Test-Path $ReportDir)) {
    New-Item -ItemType Directory -Path $ReportDir -Force | Out-Null
}

# Initialize report file with header
$Timestamp = Get-Date -Format "yyyy-MM-dd HH:MM:ss"
$Branch = git rev-parse --abbrev-ref HEAD
$RepoName = Split-Path -Leaf $ProjectRoot

@"
═══════════════════════════════════════════════════════════════
GitHub Copilot Pre-Commit Quality Report
═══════════════════════════════════════════════════════════════
Generated: $Timestamp
Repository: $RepoName
Branch: $Branch
═══════════════════════════════════════════════════════════════

"@ | Out-File -FilePath $ReportFile -Encoding UTF8

# Check if GitHub CLI is available
if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
    Write-Host "✗ GitHub CLI (gh) not found" -ForegroundColor Red
    Write-Host "  Please install: https://cli.github.com/" -ForegroundColor Yellow
    exit 1
}

# Check if Copilot extension is available
try {
    $null = gh copilot --version 2>&1
} catch {
    Write-Host "⚠ GitHub Copilot CLI not available" -ForegroundColor Yellow
    Write-Host "  Install with: gh extension install github/gh-copilot" -ForegroundColor Yellow
    Write-Host "  Skipping Copilot checks..." -ForegroundColor Yellow
    "" | Add-Content -Path $ReportFile
    "⚠ WARNING: GitHub Copilot CLI not available - checks skipped" | Add-Content -Path $ReportFile
    exit 0
}

# Get list of staged files (only source code files)
$StagedFiles = git diff --cached --name-only --diff-filter=ACM | Where-Object { $_ -match '\.(java|kt|kts|js|ts|tsx|jsx|py|go|rb|php|cs)$' }

if (-not $StagedFiles) {
    Write-Host "✓ No source code files staged" -ForegroundColor Green
    Write-Host ""
    "No source code files to review" | Add-Content -Path $ReportFile
    exit 0
}

Write-Host "Files to review:" -ForegroundColor Cyan
$StagedFiles | ForEach-Object { Write-Host "  • $_" }
Write-Host ""

# Save staged files to report
"FILES UNDER REVIEW:" | Add-Content -Path $ReportFile
"-------------------------------------------------------------------" | Add-Content -Path $ReportFile
$StagedFiles | ForEach-Object { Add-Content -Path $ReportFile -Value $_ }
"" | Add-Content -Path $ReportFile

# Track blocking issues
$HasBlockingIssues = $false
$BlockingReasons = @()

# Protocol paths
$ReviewProtocol = Join-Path $ProjectRoot "copilot\protocols\review.md"
$SecurityProtocol = Join-Path $ProjectRoot "copilot\protocols\security.md"
$PerformanceProtocol = Join-Path $ProjectRoot "copilot\protocols\performance.md"
$TestsProtocol = Join-Path $ProjectRoot "copilot\protocols\tests.md"
$RefactorProtocol = Join-Path $ProjectRoot "copilot\protocols\refactor.md"

# Function to run Copilot review for a protocol
function Run-CopilotCheck {
    param(
        [string]$ProtocolName,
        [string]$ProtocolFile,
        [bool]$IsBlocking
    )
    
    Write-Host "▶ Running $ProtocolName check..." -ForegroundColor Cyan
    
    # Add section header to report
    "" | Add-Content -Path $ReportFile
    "═══════════════════════════════════════════════════════════════" | Add-Content -Path $ReportFile
    "  $ProtocolName CHECK" | Add-Content -Path $ReportFile
    "═══════════════════════════════════════════════════════════════" | Add-Content -Path $ReportFile
    "" | Add-Content -Path $ReportFile
    
    if (-not (Test-Path $ProtocolFile)) {
        Write-Host "  ⚠ Protocol file not found: $ProtocolFile" -ForegroundColor Yellow
        "⚠ Protocol file not found: $ProtocolFile" | Add-Content -Path $ReportFile
        "" | Add-Content -Path $ReportFile
        return
    }
    
    # Read protocol content
    $ProtocolContent = Get-Content $ProtocolFile -Raw
    
    # Get staged changes
    $StagedChanges = git diff --cached
    
    # Build prompt for Copilot
    $Prompt = @"
Review the following code changes according to this protocol:

PROTOCOL:
$ProtocolContent

STAGED CHANGES:
$StagedChanges

Analyze the changes and report:
1. Critical issues (architecture violations, security risks, missing tests, unsafe patterns)
2. Warnings (minor improvements, style suggestions)
3. Pass (if no issues)

Format your response as:
CRITICAL: [list critical issues or 'none']
WARNINGS: [list warnings or 'none']
STATUS: [BLOCK or PASS]
"@
    
    # Run Copilot review using explain command
    try {
        $CopilotOutput = $Prompt | gh copilot explain 2>&1
    } catch {
        $CopilotOutput = "ERROR: Copilot check failed - $_"
    }
    
    # Write output to report
    $CopilotOutput | Add-Content -Path $ReportFile
    "" | Add-Content -Path $ReportFile
    
    # Check for critical issues in output
    $OutputText = $CopilotOutput -join "`n"
    if ($OutputText -match "CRITICAL:" -and $OutputText -notmatch "CRITICAL:\s*(none|None|NONE)") {
        if ($IsBlocking) {
            $script:HasBlockingIssues = $true
            $script:BlockingReasons += "$ProtocolName: Critical issues detected"
            Write-Host "  ✗ Critical issues found" -ForegroundColor Red
        } else {
            Write-Host "  ⚠ Issues found (non-blocking)" -ForegroundColor Yellow
        }
    } elseif ($OutputText -match "STATUS:\s*BLOCK") {
        if ($IsBlocking) {
            $script:HasBlockingIssues = $true
            $script:BlockingReasons += "$ProtocolName: Review blocked"
            Write-Host "  ✗ Review blocked" -ForegroundColor Red
        } else {
            Write-Host "  ⚠ Issues found (non-blocking)" -ForegroundColor Yellow
        }
    } else {
        Write-Host "  ✓ Check passed" -ForegroundColor Green
    }
}

# Step 1: Architecture & Code Review
Run-CopilotCheck -ProtocolName "Architecture & Code Review" -ProtocolFile $ReviewProtocol -IsBlocking $true

# Step 2: Security Review
Run-CopilotCheck -ProtocolName "Security" -ProtocolFile $SecurityProtocol -IsBlocking $true

# Step 3: Performance Review
Run-CopilotCheck -ProtocolName "Performance" -ProtocolFile $PerformanceProtocol -IsBlocking $true

# Step 4: Testing Review
Run-CopilotCheck -ProtocolName "Testing" -ProtocolFile $TestsProtocol -IsBlocking $true

# Step 5: Refactoring Suggestions (non-blocking)
Run-CopilotCheck -ProtocolName "Refactoring" -ProtocolFile $RefactorProtocol -IsBlocking $false

# Add footer to report
"" | Add-Content -Path $ReportFile
"═══════════════════════════════════════════════════════════════" | Add-Content -Path $ReportFile
"END OF REPORT" | Add-Content -Path $ReportFile
"═══════════════════════════════════════════════════════════════" | Add-Content -Path $ReportFile

Write-Host ""
Write-Host "╔════════════════════════════════════════════════════════════════╗" -ForegroundColor Cyan

# Final decision
if ($HasBlockingIssues) {
    Write-Host "║                    ✗ COMMIT BLOCKED                           ║" -ForegroundColor Red
    Write-Host "╚════════════════════════════════════════════════════════════════╝" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Critical issues detected:" -ForegroundColor Red
    $BlockingReasons | ForEach-Object { Write-Host "  • $_" -ForegroundColor Red }
    Write-Host ""
    Write-Host "Review the detailed report:" -ForegroundColor Yellow
    Write-Host "  $ReportFile" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "To bypass this check (NOT RECOMMENDED):" -ForegroundColor Yellow
    Write-Host "  git commit --no-verify" -ForegroundColor Cyan
    Write-Host ""
    exit 1
} else {
    Write-Host "║                    ✓ ALL CHECKS PASSED                        ║" -ForegroundColor Green
    Write-Host "╚════════════════════════════════════════════════════════════════╝" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "No critical issues detected. Safe to commit!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Detailed report saved to:" -ForegroundColor Cyan
    Write-Host "  $ReportFile" -ForegroundColor Cyan
    Write-Host ""
    exit 0
}
