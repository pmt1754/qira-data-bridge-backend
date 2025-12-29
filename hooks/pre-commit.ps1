# Pre-commit hook for AI-based code review and refactoring (PowerShell version)
# This hook runs before every commit to ensure code quality

Write-Host "Running AI-based pre-commit checks..." -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan

# Get the project root directory
$ProjectRoot = git rev-parse --show-toplevel
$HookOutputDir = Join-Path $ProjectRoot ".git\hooks\reports"
if (-not (Test-Path $HookOutputDir)) {
    New-Item -ItemType Directory -Path $HookOutputDir -Force | Out-Null
}

# Output files
$ReviewOutput = Join-Path $HookOutputDir "last-review.md"
$RefactorOutput = Join-Path $HookOutputDir "last-refactor.md"
$Timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"

# Track if there are any blocking issues
$HasBlockingIssues = $false
$ReviewIssues = @()

# Check if copilot protocols exist
$ReviewProtocol = Join-Path $ProjectRoot "copilot\protocols\review.md"
if (-not (Test-Path $ReviewProtocol)) {
    Write-Host "Warning: copilot/protocols/review.md not found" -ForegroundColor Yellow
    Write-Host "   Skipping AI review check" -ForegroundColor Yellow
} else {
    Write-Host "Step 1: Running code review protocol..." -ForegroundColor Green
    
    # Create review output
    "---" | Out-File -FilePath $ReviewOutput -Encoding UTF8
    "timestamp: $Timestamp" | Add-Content -Path $ReviewOutput
    "protocol: review" | Add-Content -Path $ReviewOutput
    "---" | Add-Content -Path $ReviewOutput
    "" | Add-Content -Path $ReviewOutput
    "# Code Review Report" | Add-Content -Path $ReviewOutput
    "" | Add-Content -Path $ReviewOutput
    "Protocol: copilot/protocols/review.md" | Add-Content -Path $ReviewOutput
    "Generated: $Timestamp" | Add-Content -Path $ReviewOutput
    "" | Add-Content -Path $ReviewOutput
    
    # Get staged files
    $StagedFiles = git diff --cached --name-only --diff-filter=ACM | Where-Object { $_ -match '\.(java|kt|kts|properties|md|yml|yaml)$' }
    
    if ($StagedFiles) {
        "Files to be committed:" | Add-Content -Path $ReviewOutput
        '```' | Add-Content -Path $ReviewOutput
        $StagedFiles | ForEach-Object { Add-Content -Path $ReviewOutput -Value $_ }
        '```' | Add-Content -Path $ReviewOutput
        "" | Add-Content -Path $ReviewOutput
        "## Review Checklist" | Add-Content -Path $ReviewOutput
        "" | Add-Content -Path $ReviewOutput
        "Please review the following before committing:" | Add-Content -Path $ReviewOutput
        "" | Add-Content -Path $ReviewOutput
        
        # Check each staged Java file for potential issues
        $JavaFiles = $StagedFiles | Where-Object { $_ -match '\.(java|kt|kts)$' }
        
        foreach ($file in $JavaFiles) {
            $FilePath = Join-Path $ProjectRoot $file
            if (Test-Path $FilePath) {
                $Content = Get-Content $FilePath -Raw
                
                # Check for common issues (excluding comments)
                $CodeLines = $Content -split "`n" | Where-Object { $_ -notmatch '^\s*//' }
                $CodeContent = $CodeLines -join "`n"
                
                if ($CodeContent -match 'System\.out\.println\s*\(') {
                    $ReviewIssues += "BLOCKING: Found System.out.println in $file - use proper logging (SLF4J)"
                    $HasBlockingIssues = $true
                }
                if ($CodeContent -match '\.printStackTrace\s*\(\s*\)') {
                    $ReviewIssues += "BLOCKING: Found printStackTrace() in $file - use proper logging"
                    $HasBlockingIssues = $true
                }
                if ($Content -match '//\s*TODO' -and $Content -match '//\s*FIXME') {
                    $ReviewIssues += "WARNING: Found TODO/FIXME comments in $file - consider addressing before commit"
                }
                if ($CodeContent -match 'catch\s*\([^)]+\)\s*\{\s*\}') {
                    $ReviewIssues += "BLOCKING: Found empty catch block in $file - handle exceptions properly"
                    $HasBlockingIssues = $true
                }
                if ($CodeContent -match '@Autowired\s+private') {
                    $ReviewIssues += "WARNING: Found field injection (@Autowired private) in $file - prefer constructor injection"
                }
            }
        }
        
        # Extract checklist from review protocol
        $ReviewContent = Get-Content $ReviewProtocol -Raw
        if ($ReviewContent -match '## Review Checklist') {
            $ChecklistSection = $ReviewContent -split '## Review Checklist' | Select-Object -Last 1
            $ChecklistSection = $ChecklistSection -split '##' | Select-Object -First 1
            Add-Content -Path $ReviewOutput -Value $ChecklistSection.Trim()
        }
        
        # Add detected issues to report
        if ($ReviewIssues.Count -gt 0) {
            "" | Add-Content -Path $ReviewOutput
            "## Detected Issues" | Add-Content -Path $ReviewOutput
            "" | Add-Content -Path $ReviewOutput
            foreach ($issue in $ReviewIssues) {
                if ($issue -like "BLOCKING:*") {
                    "- :x: $issue" | Add-Content -Path $ReviewOutput
                } else {
                    "- :warning: $issue" | Add-Content -Path $ReviewOutput
                }
            }
        }
        
        Write-Host "Review report generated: $ReviewOutput" -ForegroundColor Green
        
        if ($HasBlockingIssues) {
            Write-Host ""
            Write-Host "BLOCKING ISSUES FOUND:" -ForegroundColor Red
            foreach ($issue in $ReviewIssues | Where-Object { $_ -like "BLOCKING:*" }) {
                Write-Host "  - $($issue -replace 'BLOCKING: ', '')" -ForegroundColor Red
            }
        }
    } else {
        Write-Host "No relevant files staged for review" -ForegroundColor Gray
    }
}

Write-Host ""

# Check for refactor protocol
$RefactorProtocol = Join-Path $ProjectRoot "copilot\protocols\refactor.md"
if (-not (Test-Path $RefactorProtocol)) {
    Write-Host "Warning: copilot/protocols/refactor.md not found" -ForegroundColor Yellow
    Write-Host "   Skipping refactoring suggestions" -ForegroundColor Yellow
} else {
    Write-Host "Step 2: Checking refactoring opportunities..." -ForegroundColor Green
    
    # Create refactor output
    "---" | Out-File -FilePath $RefactorOutput -Encoding UTF8
    "timestamp: $Timestamp" | Add-Content -Path $RefactorOutput
    "protocol: refactor" | Add-Content -Path $RefactorOutput
    "---" | Add-Content -Path $RefactorOutput
    "" | Add-Content -Path $RefactorOutput
    "# Refactoring Suggestions" | Add-Content -Path $RefactorOutput
    "" | Add-Content -Path $RefactorOutput
    "Protocol: copilot/protocols/refactor.md" | Add-Content -Path $RefactorOutput
    "Generated: $Timestamp" | Add-Content -Path $RefactorOutput
    "" | Add-Content -Path $RefactorOutput
    
    # Get staged code files
    $StagedFiles = git diff --cached --name-only --diff-filter=ACM | Where-Object { $_ -match '\.(java|kt|kts)$' }
    
    if ($StagedFiles) {
        "Files analyzed:" | Add-Content -Path $RefactorOutput
        '```' | Add-Content -Path $RefactorOutput
        $StagedFiles | ForEach-Object { Add-Content -Path $RefactorOutput -Value $_ }
        '```' | Add-Content -Path $RefactorOutput
        "" | Add-Content -Path $RefactorOutput
        "## Refactoring Opportunities" | Add-Content -Path $RefactorOutput
        "" | Add-Content -Path $RefactorOutput
        "Consider the following refactoring patterns:" | Add-Content -Path $RefactorOutput
        "" | Add-Content -Path $RefactorOutput
        
        # Extract refactoring guidelines from protocol
        $RefactorContent = Get-Content $RefactorProtocol -Raw
        if ($RefactorContent -match '## Common Refactoring Patterns') {
            $PatternsSection = $RefactorContent -split '## Common Refactoring Patterns' | Select-Object -Last 1
            $PatternsSection = ($PatternsSection -split '##' | Select-Object -First 1).Trim()
            $Lines = $PatternsSection -split "`n" | Select-Object -First 20
            $Lines | ForEach-Object { Add-Content -Path $RefactorOutput -Value $_ }
        }
        
        Write-Host "Refactoring report generated: $RefactorOutput" -ForegroundColor Green
    } else {
        Write-Host "No code files staged for refactoring analysis" -ForegroundColor Gray
    }
}

Write-Host ""
Write-Host "================================================" -ForegroundColor Cyan

if ($HasBlockingIssues) {
    Write-Host "COMMIT BLOCKED!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Your commit has been blocked due to code quality issues." -ForegroundColor Red
    Write-Host "Please fix the blocking issues listed above and try again." -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Review report: $ReviewOutput" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "To bypass this check (NOT RECOMMENDED), use:" -ForegroundColor Gray
    Write-Host "  git commit --no-verify" -ForegroundColor Gray
    Write-Host ""
    exit 1
} else {
    Write-Host "Pre-commit checks completed successfully!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Reports saved to:" -ForegroundColor Cyan
    Write-Host "   - $ReviewOutput"
    Write-Host "   - $RefactorOutput"
    Write-Host ""
    if ($ReviewIssues.Count -gt 0) {
        Write-Host "Note: Some warnings were found. Review the report for details." -ForegroundColor Yellow
    } else {
        Write-Host "No issues detected. Safe to commit!" -ForegroundColor Green
    }
    Write-Host ""
    exit 0
}
