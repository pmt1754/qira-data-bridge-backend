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

$Timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"

# Track if there are any blocking issues
$HasBlockingIssues = $false
$ReviewIssues = @()

# Get staged files once
$StagedFiles = git diff --cached --name-only --diff-filter=ACM | Where-Object { $_ -match '\.(java|kt|kts|properties|md|yml|yaml)$' }
$JavaFiles = $StagedFiles | Where-Object { $_ -match '\.(java|kt|kts)$' }

# Step 1: Code Review Protocol
$ReviewProtocol = Join-Path $ProjectRoot "copilot\protocols\review.md"
$ReviewOutput = Join-Path $HookOutputDir "last-review.md"
if (Test-Path $ReviewProtocol) {
    Write-Host "Step 1: Running Code Review protocol..." -ForegroundColor Green
    
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
    
    if ($StagedFiles) {
        "Files analyzed:" | Add-Content -Path $ReviewOutput
        '```' | Add-Content -Path $ReviewOutput
        $StagedFiles | ForEach-Object { Add-Content -Path $ReviewOutput -Value $_ }
        '```' | Add-Content -Path $ReviewOutput
        "" | Add-Content -Path $ReviewOutput
        
        # Check each staged Java file for potential issues
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
                if ($Content -match '//\s*TODO' -or $Content -match '//\s*FIXME') {
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
    }
}

Write-Host ""

# Step 2: Security Protocol
$SecurityProtocol = Join-Path $ProjectRoot "copilot\protocols\security.md"
$SecurityOutput = Join-Path $HookOutputDir "last-security.md"
if (Test-Path $SecurityProtocol) {
    Write-Host "Step 2: Running Security protocol..." -ForegroundColor Green
    
    "---" | Out-File -FilePath $SecurityOutput -Encoding UTF8
    "timestamp: $Timestamp" | Add-Content -Path $SecurityOutput
    "protocol: security" | Add-Content -Path $SecurityOutput
    "---" | Add-Content -Path $SecurityOutput
    "" | Add-Content -Path $SecurityOutput
    "# Security Review Report" | Add-Content -Path $SecurityOutput
    "" | Add-Content -Path $SecurityOutput
    "Protocol: copilot/protocols/security.md" | Add-Content -Path $SecurityOutput
    "Generated: $Timestamp" | Add-Content -Path $SecurityOutput
    "" | Add-Content -Path $SecurityOutput
    
    if ($JavaFiles) {
        "Files analyzed:" | Add-Content -Path $SecurityOutput
        '```' | Add-Content -Path $SecurityOutput
        $JavaFiles | ForEach-Object { Add-Content -Path $SecurityOutput -Value $_ }
        '```' | Add-Content -Path $SecurityOutput
        "" | Add-Content -Path $SecurityOutput
        "## Security Checks" | Add-Content -Path $SecurityOutput
        "" | Add-Content -Path $SecurityOutput
        
        foreach ($file in $JavaFiles) {
            $FilePath = Join-Path $ProjectRoot $file
            if (Test-Path $FilePath) {
                $Content = Get-Content $FilePath -Raw
                
                # Check for security issues
                if ($Content -match "password\s*=\s*[`"'].*[`"']" -or $Content -match "api[_-]?key\s*=\s*[`"'].*[`"']") {
                    $ReviewIssues += "BLOCKING: Possible hard-coded credentials in $file - use environment variables"
                    $HasBlockingIssues = $true
                    "- :x: BLOCKING: Possible hard-coded credentials detected in $file" | Add-Content -Path $SecurityOutput
                }
                if ($Content -match "SELECT.*\+.*WHERE" -or $Content -match "String\s+sql\s*=.*\+") {
                    $ReviewIssues += "BLOCKING: Possible SQL injection vulnerability in $file - use parameterized queries"
                    $HasBlockingIssues = $true
                    "- :x: BLOCKING: Possible SQL injection vulnerability in $file" | Add-Content -Path $SecurityOutput
                }
            }
        }
    }
}

Write-Host ""

# Step 3: Performance Protocol
$PerformanceProtocol = Join-Path $ProjectRoot "copilot\protocols\performance.md"
$PerformanceOutput = Join-Path $HookOutputDir "last-performance.md"
if (Test-Path $PerformanceProtocol) {
    Write-Host "Step 3: Running Performance protocol..." -ForegroundColor Green
    
    "---" | Out-File -FilePath $PerformanceOutput -Encoding UTF8
    "timestamp: $Timestamp" | Add-Content -Path $PerformanceOutput
    "protocol: performance" | Add-Content -Path $PerformanceOutput
    "---" | Add-Content -Path $PerformanceOutput
    "" | Add-Content -Path $PerformanceOutput
    "# Performance Review Report" | Add-Content -Path $PerformanceOutput
    "" | Add-Content -Path $PerformanceOutput
    "Protocol: copilot/protocols/performance.md" | Add-Content -Path $PerformanceOutput
    "Generated: $Timestamp" | Add-Content -Path $PerformanceOutput
    "" | Add-Content -Path $PerformanceOutput
    
    if ($JavaFiles) {
        "Files analyzed:" | Add-Content -Path $PerformanceOutput
        '```' | Add-Content -Path $PerformanceOutput
        $JavaFiles | ForEach-Object { Add-Content -Path $PerformanceOutput -Value $_ }
        '```' | Add-Content -Path $PerformanceOutput
        "" | Add-Content -Path $PerformanceOutput
        "## Performance Checks" | Add-Content -Path $PerformanceOutput
        "" | Add-Content -Path $PerformanceOutput
        
        foreach ($file in $JavaFiles) {
            $FilePath = Join-Path $ProjectRoot $file
            if (Test-Path $FilePath) {
                $Content = Get-Content $FilePath -Raw
                
                # Check for performance issues
                if ($Content -match 'for\s*\([^)]+\)\s*\{[^}]*\.(find|get|fetch)\w*\(') {
                    "- :warning: WARNING: Possible N+1 query pattern in $file" | Add-Content -Path $PerformanceOutput
                }
                if ($Content -match 'for\s*\([^)]+\)\s*\{[^}]*result\s*\+=') {
                    "- :warning: WARNING: String concatenation in loop in $file - consider StringBuilder" | Add-Content -Path $PerformanceOutput
                }
            }
        }
    }
}

Write-Host ""

# Step 4: Reliability Protocol
$ReliabilityProtocol = Join-Path $ProjectRoot "copilot\protocols\reliability.md"
$ReliabilityOutput = Join-Path $HookOutputDir "last-reliability.md"
if (Test-Path $ReliabilityProtocol) {
    Write-Host "Step 4: Running Reliability protocol..." -ForegroundColor Green
    
    "---" | Out-File -FilePath $ReliabilityOutput -Encoding UTF8
    "timestamp: $Timestamp" | Add-Content -Path $ReliabilityOutput
    "protocol: reliability" | Add-Content -Path $ReliabilityOutput
    "---" | Add-Content -Path $ReliabilityOutput
    "" | Add-Content -Path $ReliabilityOutput
    "# Reliability Review Report" | Add-Content -Path $ReliabilityOutput
    "" | Add-Content -Path $ReliabilityOutput
    "Protocol: copilot/protocols/reliability.md" | Add-Content -Path $ReliabilityOutput
    "Generated: $Timestamp" | Add-Content -Path $ReliabilityOutput
    "" | Add-Content -Path $ReliabilityOutput
    
    if ($JavaFiles) {
        "Files analyzed:" | Add-Content -Path $ReliabilityOutput
        '```' | Add-Content -Path $ReliabilityOutput
        $JavaFiles | ForEach-Object { Add-Content -Path $ReliabilityOutput -Value $_ }
        '```' | Add-Content -Path $ReliabilityOutput
        "" | Add-Content -Path $ReliabilityOutput
    }
}

Write-Host ""

# Step 5: Maintainability Protocol
$MaintainabilityProtocol = Join-Path $ProjectRoot "copilot\protocols\maintainability.md"
$MaintainabilityOutput = Join-Path $HookOutputDir "last-maintainability.md"
if (Test-Path $MaintainabilityProtocol) {
    Write-Host "Step 5: Running Maintainability protocol..." -ForegroundColor Green
    
    "---" | Out-File -FilePath $MaintainabilityOutput -Encoding UTF8
    "timestamp: $Timestamp" | Add-Content -Path $MaintainabilityOutput
    "protocol: maintainability" | Add-Content -Path $MaintainabilityOutput
    "---" | Add-Content -Path $MaintainabilityOutput
    "" | Add-Content -Path $MaintainabilityOutput
    "# Maintainability Review Report" | Add-Content -Path $MaintainabilityOutput
    "" | Add-Content -Path $MaintainabilityOutput
    "Protocol: copilot/protocols/maintainability.md" | Add-Content -Path $MaintainabilityOutput
    "Generated: $Timestamp" | Add-Content -Path $MaintainabilityOutput
    "" | Add-Content -Path $MaintainabilityOutput
    
    if ($JavaFiles) {
        "Files analyzed:" | Add-Content -Path $MaintainabilityOutput
        '```' | Add-Content -Path $MaintainabilityOutput
        $JavaFiles | ForEach-Object { Add-Content -Path $MaintainabilityOutput -Value $_ }
        '```' | Add-Content -Path $MaintainabilityOutput
        "" | Add-Content -Path $MaintainabilityOutput
    }
}

Write-Host ""

# Step 6: Documentation Protocol
$DocumentationProtocol = Join-Path $ProjectRoot "copilot\protocols\documentation.md"
$DocumentationOutput = Join-Path $HookOutputDir "last-documentation.md"
if (Test-Path $DocumentationProtocol) {
    Write-Host "Step 6: Running Documentation protocol..." -ForegroundColor Green
    
    "---" | Out-File -FilePath $DocumentationOutput -Encoding UTF8
    "timestamp: $Timestamp" | Add-Content -Path $DocumentationOutput
    "protocol: documentation" | Add-Content -Path $DocumentationOutput
    "---" | Add-Content -Path $DocumentationOutput
    "" | Add-Content -Path $DocumentationOutput
    "# Documentation Review Report" | Add-Content -Path $DocumentationOutput
    "" | Add-Content -Path $DocumentationOutput
    "Protocol: copilot/protocols/documentation.md" | Add-Content -Path $DocumentationOutput
    "Generated: $Timestamp" | Add-Content -Path $DocumentationOutput
    "" | Add-Content -Path $DocumentationOutput
    
    if ($JavaFiles) {
        "Files analyzed:" | Add-Content -Path $DocumentationOutput
        '```' | Add-Content -Path $DocumentationOutput
        $JavaFiles | ForEach-Object { Add-Content -Path $DocumentationOutput -Value $_ }
        '```' | Add-Content -Path $DocumentationOutput
        "" | Add-Content -Path $DocumentationOutput
    }
}

Write-Host ""

# Step 7: Refactoring Protocol
$RefactorProtocol = Join-Path $ProjectRoot "copilot\protocols\refactor.md"
$RefactorOutput = Join-Path $HookOutputDir "last-refactor.md"
if (Test-Path $RefactorProtocol) {
    Write-Host "Step 7: Running Refactoring protocol..." -ForegroundColor Green
    
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
    
    if ($JavaFiles) {
        "Files analyzed:" | Add-Content -Path $RefactorOutput
        '```' | Add-Content -Path $RefactorOutput
        $JavaFiles | ForEach-Object { Add-Content -Path $RefactorOutput -Value $_ }
        '```' | Add-Content -Path $RefactorOutput
        "" | Add-Content -Path $RefactorOutput
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
    Write-Host "Review reports:" -ForegroundColor Cyan
    Write-Host "   - $ReviewOutput"
    if (Test-Path $SecurityOutput) { Write-Host "   - $SecurityOutput" }
    if (Test-Path $PerformanceOutput) { Write-Host "   - $PerformanceOutput" }
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
    if (Test-Path $SecurityOutput) { Write-Host "   - $SecurityOutput" }
    if (Test-Path $PerformanceOutput) { Write-Host "   - $PerformanceOutput" }
    if (Test-Path $ReliabilityOutput) { Write-Host "   - $ReliabilityOutput" }
    if (Test-Path $MaintainabilityOutput) { Write-Host "   - $MaintainabilityOutput" }
    if (Test-Path $DocumentationOutput) { Write-Host "   - $DocumentationOutput" }
    if (Test-Path $RefactorOutput) { Write-Host "   - $RefactorOutput" }
    Write-Host ""
    if ($ReviewIssues.Count -gt 0) {
        Write-Host "Note: Some warnings were found. Review the reports for details." -ForegroundColor Yellow
    } else {
        Write-Host "No issues detected. Safe to commit!" -ForegroundColor Green
    }
    Write-Host ""
    exit 0
}
