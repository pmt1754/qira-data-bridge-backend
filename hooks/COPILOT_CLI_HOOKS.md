# GitHub Copilot CLI Pre-Commit Hooks

## Overview

This directory contains enhanced pre-commit hooks that leverage **GitHub Copilot CLI** to perform comprehensive automated code reviews before each commit.

## Two Hook Systems Available

### 1. Pattern-Based Hooks (Default)
- **Files**: `pre-commit`, `pre-commit.ps1`
- **Fast**, static pattern matching (System.out.println, printStackTrace, etc.)
- **No external dependencies** beyond Git
- **Always active** by default

### 2. Copilot CLI Hooks (Advanced)
- **Files**: `pre-commit-copilot`, `pre-commit-copilot.ps1`
- **AI-powered** review using GitHub Copilot
- **Protocol-driven** checks (architecture, security, performance, testing, refactoring)
- **Requires** GitHub CLI with Copilot extension
- **Opt-in** activation required

---

## Prerequisites for Copilot CLI Hooks

### 1. Install GitHub CLI

**macOS:**
```bash
brew install gh
```

**Windows:**
```powershell
winget install --id GitHub.cli
```

**Linux:**
```bash
# Debian/Ubuntu
curl -fsSL https://cli.github.com/packages/githubcli-archive-keyring.gpg | sudo dd of=/usr/share/keyrings/githubcli-archive-keyring.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/githubcli-archive-keyring.gpg] https://cli.github.com/packages stable main" | sudo tee /etc/apt/sources.list.d/github-cli.list > /dev/null
sudo apt update
sudo apt install gh

# Fedora/RHEL/CentOS
sudo dnf install 'dnf-command(config-manager)'
sudo dnf config-manager --add-repo https://cli.github.com/packages/rpm/gh-cli.repo
sudo dnf install gh
```

### 2. Authenticate GitHub CLI
```bash
gh auth login
```

### 3. Install GitHub Copilot Extension
```bash
gh extension install github/gh-copilot
```

### 4. Verify Installation
```bash
gh copilot --version
```

---

## Enabling Copilot CLI Hooks

### Option 1: Enable for Your Repository (Recommended)

From the project root:

```bash
# Create flag file to enable Copilot CLI hooks
touch .git/hooks/USE_COPILOT_CLI
```

The existing `pre-commit` hook will automatically detect this flag and switch to Copilot CLI mode.

### Option 2: Install Copilot Hooks Directly

**Windows (PowerShell):**
```powershell
Copy-Item hooks\pre-commit-copilot .git\hooks\pre-commit -Force
Copy-Item hooks\pre-commit-copilot.ps1 .git\hooks\pre-commit-copilot.ps1 -Force
```

**Unix/Linux/Mac:**
```bash
cp hooks/pre-commit-copilot .git/hooks/pre-commit
chmod +x .git/hooks/pre-commit
cp hooks/pre-commit-copilot.ps1 .git/hooks/pre-commit-copilot.ps1
```

---

## What Gets Checked

The Copilot CLI hooks run 5 protocol-based reviews in sequence:

### 1. ✅ Architecture & Code Review (`review.md`)
**Blocking**: Yes  
**Checks**:
- Controller → Service → Repository pattern compliance
- No business logic in controllers
- DTO usage for requests/responses
- Input validation and null checks
- Proper logging
- Code duplication
- Method complexity

### 2. 🔒 Security Review (`security.md`)
**Blocking**: Yes  
**Checks**:
- SQL injection vulnerabilities
- Authentication/authorization issues
- Sensitive data exposure
- Insecure cryptography
- Input sanitization
- API security best practices

### 3. ⚡ Performance Review (`performance.md`)
**Blocking**: Yes  
**Checks**:
- N+1 query problems
- Missing database indexes
- Inefficient loops
- Memory leaks
- Unnecessary object creation
- Caching opportunities

### 4. 🧪 Testing Review (`tests.md`)
**Blocking**: Yes  
**Checks**:
- Test coverage for new logic
- Proper mocking
- Test isolation
- Edge case coverage
- Integration test requirements

### 5. 🔧 Refactoring Suggestions (`refactor.md`)
**Blocking**: No (warnings only)  
**Suggests**:
- Code simplification opportunities
- Design pattern improvements
- Maintainability enhancements
- Technical debt reduction

---

## How It Works

### Execution Flow

1. **Detect Staged Files**: Only analyzes source code files (`.java`, `.kt`, `.js`, `.ts`, `.py`, etc.)
2. **Run Protocol Checks**: Each protocol is evaluated by GitHub Copilot CLI
3. **Generate Report**: Detailed findings saved to `.git/hooks/reports/copilot-precommit-report.txt`
4. **Block or Allow**:
   - **BLOCK**: If critical issues found in Architecture, Security, Performance, or Testing
   - **ALLOW**: If only warnings or refactoring suggestions present

### Example Output

**All Checks Pass:**
```
╔════════════════════════════════════════════════════════════════╗
║          GitHub Copilot Pre-Commit Quality Gate              ║
╚════════════════════════════════════════════════════════════════╝

Files to review:
  • src/main/java/com/app/service/UserService.java

▶ Running Architecture & Code Review check...
  ✓ Check passed
▶ Running Security check...
  ✓ Check passed
▶ Running Performance check...
  ✓ Check passed
▶ Running Testing check...
  ✓ Check passed
▶ Running Refactoring check...
  ✓ Check passed

╔════════════════════════════════════════════════════════════════╗
║                    ✓ ALL CHECKS PASSED                        ║
╚════════════════════════════════════════════════════════════════╝

No critical issues detected. Safe to commit!

Detailed report saved to:
  .git/hooks/reports/copilot-precommit-report.txt
```

**Critical Issues Found:**
```
╔════════════════════════════════════════════════════════════════╗
║          GitHub Copilot Pre-Commit Quality Gate              ║
╚════════════════════════════════════════════════════════════════╝

Files to review:
  • src/main/java/com/app/service/UserService.java

▶ Running Architecture & Code Review check...
  ✓ Check passed
▶ Running Security check...
  ✗ Critical issues found
▶ Running Performance check...
  ✓ Check passed
▶ Running Testing check...
  ✗ Critical issues found
▶ Running Refactoring check...
  ✓ Check passed

╔════════════════════════════════════════════════════════════════╗
║                    ✗ COMMIT BLOCKED                           ║
╚════════════════════════════════════════════════════════════════╝

Critical issues detected:
  • Security: Critical issues detected
  • Testing: Critical issues detected

Review the detailed report:
  .git/hooks/reports/copilot-precommit-report.txt

To bypass this check (NOT RECOMMENDED):
  git commit --no-verify
```

---

## Report Format

Reports are saved to `.git/hooks/reports/copilot-precommit-report.txt`:

```
═══════════════════════════════════════════════════════════════
GitHub Copilot Pre-Commit Quality Report
═══════════════════════════════════════════════════════════════
Generated: 2025-12-29 14:30:45
Repository: qira-data-bridge-backend
Branch: feature/user-auth
═══════════════════════════════════════════════════════════════

FILES UNDER REVIEW:
-------------------------------------------------------------------
src/main/java/com/app/service/UserService.java

═══════════════════════════════════════════════════════════════
  Architecture & Code Review CHECK
═══════════════════════════════════════════════════════════════

[Copilot analysis output...]

═══════════════════════════════════════════════════════════════
  Security CHECK
═══════════════════════════════════════════════════════════════

[Copilot analysis output...]

...
```

---

## Bypassing Hooks (Emergency Use Only)

### Temporary Bypass (Single Commit)
```bash
git commit --no-verify -m "Emergency hotfix"
```

### Disable Copilot CLI Hooks (Revert to Pattern-Based)
```bash
rm .git/hooks/USE_COPILOT_CLI
```

### Disable All Hooks (Not Recommended)
```bash
# Backup and remove hooks
mv .git/hooks/pre-commit .git/hooks/pre-commit.disabled
```

⚠️ **Warning**: Bypassing hooks should only be done in emergencies. Always run checks manually afterward.

---

## Troubleshooting

### Hook Not Running
```bash
# Verify hook is executable (Unix/Linux/Mac)
chmod +x .git/hooks/pre-commit

# Verify hook exists
ls -la .git/hooks/pre-commit
```

### "gh command not found"
```bash
# Install GitHub CLI
# macOS: brew install gh
# Windows: winget install --id GitHub.cli
# Linux: See installation instructions above

# Verify installation
which gh
gh --version
```

### "Copilot extension not found"
```bash
# Install Copilot extension
gh extension install github/gh-copilot

# Verify installation
gh copilot --version
```

### Slow Performance
- Copilot CLI hooks are slower than pattern-based hooks (~5-15 seconds)
- Use pattern-based hooks for rapid development
- Enable Copilot CLI hooks for feature branches and critical commits

### False Positives
If Copilot incorrectly blocks a commit:
1. Review the report: `.git/hooks/reports/copilot-precommit-report.txt`
2. Address legitimate issues
3. If false positive, use `git commit --no-verify` and report the issue to the team

---

## Performance Comparison

| Hook Type | Execution Time | Accuracy | Dependencies |
|-----------|----------------|----------|--------------|
| Pattern-Based | ~1-2 seconds | Good | Git only |
| Copilot CLI | ~5-15 seconds | Excellent | Git + GH CLI + Copilot |

**Recommendation**: 
- Use **pattern-based hooks** for day-to-day development
- Enable **Copilot CLI hooks** for:
  - Feature branch final commits
  - Pull request preparation
  - Critical production code
  - Security-sensitive changes

---

## Team Rollout Strategy

### Phase 1: Optional Adoption (Week 1-2)
- Install hooks as optional for volunteers
- Collect feedback on false positives
- Tune protocols based on findings

### Phase 2: Feature Branches (Week 3-4)
- Require Copilot CLI hooks on feature branches
- Allow pattern-based hooks on development branches

### Phase 3: Full Rollout (Week 5+)
- Enable Copilot CLI hooks by default
- Document exception process
- Monitor metrics (blocked commits, bypass rate)

---

## Customizing Protocols

All protocols are stored in `/copilot/protocols/`:

- `review.md` - Architecture and code review rules
- `security.md` - Security vulnerability checks
- `performance.md` - Performance anti-patterns
- `tests.md` - Testing requirements
- `refactor.md` - Refactoring suggestions

**To customize**:
1. Edit the protocol files to match your team's standards
2. Commit the protocol changes
3. Hooks will automatically use updated protocols

---

## Support

For issues or questions:
1. Check troubleshooting section above
2. Review hook logs: `.git/hooks/reports/copilot-precommit-report.txt`
3. Contact platform engineering team
4. File an issue in the repository

---

**Note**: These hooks require an active GitHub Copilot subscription and are designed for teams already using GitHub Copilot in their development workflow.
