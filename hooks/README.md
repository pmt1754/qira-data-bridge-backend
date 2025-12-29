# Git Hooks for QIRA Data Bridge Backend

This directory contains Git hooks that enforce code quality standards through automated checks before commits.

## Available Hook Systems

### 1. Pattern-Based Pre-Commit Hooks (Default)

**Files**: `pre-commit`, `pre-commit.ps1`

The default pre-commit hooks run fast, pattern-based code quality checks before allowing a commit to proceed. It performs:

1. **Code Review Protocol** - Analyzes staged files for common issues:
   - ❌ **BLOCKING**: `System.out.println` usage (use SLF4J logging instead)
   - ❌ **BLOCKING**: `printStackTrace()` calls (use proper logging)
   - ❌ **BLOCKING**: Empty catch blocks (handle exceptions properly)
   - ⚠️ **WARNING**: Field injection with `@Autowired private` (prefer constructor injection)
   - ⚠️ **WARNING**: TODO/FIXME comments (consider addressing before commit)

2. **Refactoring Analysis** - Identifies opportunities for code improvement based on the project's refactoring protocols

### Features

- **Blocking Issues**: The hook will prevent commits when critical code quality issues are detected
- **Cross-Platform**: Includes both PowerShell (Windows) and Bash (Unix/Linux/Mac) versions
- **Reports**: Generates detailed reports in `.git/hooks/reports/` directory:
- **Protocol-Driven**: Uses project-specific protocols from `/copilot/protocols/` directory

### 2. GitHub Copilot CLI Pre-Commit Hooks (Advanced)

**Files**: `pre-commit-copilot`, `pre-commit-copilot.ps1`

Enhanced hooks that use **GitHub Copilot CLI** for AI-powered code reviews:

- **AI-Powered Reviews**: Uses GitHub Copilot to analyze code changes
- **Protocol-Based**: Reviews against architecture, security, performance, testing, and refactoring protocols
- **Comprehensive**: Detects complex issues beyond pattern matching
- **Detailed Reports**: Generates in-depth analysis reports
- **Requires**: GitHub CLI with Copilot extension installed

**See `COPILOT_CLI_HOOKS.md` for detailed setup and usage instructions.**

## Installationiven**: Uses project-specific protocols from `/copilot/protocols/` directory

## Installation

### Automatic Installation (Recommended)

Run the installation script from the project root:

**Windows (PowerShell):**
```powershell
.\hooks\install.ps1
```

**Unix/Linux/Mac (Bash):**
```bash
./hooks/install.sh
chmod +x .git/hooks/pre-commit
```

### Manual Installation

1. Copy the hook files to your `.git/hooks` directory:

**Windows:**
```powershell
Copy-Item hooks\pre-commit .git\hooks\pre-commit -Force
Copy-Item hooks\pre-commit.ps1 .git\hooks\pre-commit.ps1 -Force
```

**Unix/Linux/Mac:**
```bash
cp hooks/pre-commit .git/hooks/pre-commit
cp hooks/pre-commit.ps1 .git/hooks/pre-commit.ps1
chmod +x .git/hooks/pre-commit
```

2. Ensure the hooks are executable (Unix/Linux/Mac only):
```bash
chmod +x .git/hooks/pre-commit
```

## Usage

Once installed, the hooks run automatically before each commit. No additional action is required.

### Example Output

**When no issues are found:**
```
Running AI-based pre-commit checks...
================================================
Step 1: Running code review protocol...
Review report generated
Step 2: Checking refactoring opportunities...
Refactoring report generated
================================================
✅ Pre-commit checks completed successfully!
No issues detected. Safe to commit!
```

**When blocking issues are found:**
```
Running AI-based pre-commit checks...
================================================
Step 1: Running code review protocol...
Review report generated

BLOCKING ISSUES FOUND:
  - Found System.out.println in TestClass.java - use proper logging (SLF4J)
  - Found printStackTrace() in TestClass.java - use proper logging

================================================
❌ COMMIT BLOCKED!

Your commit has been blocked due to code quality issues.
Please fix the blocking issues listed above and try again.
```

### Bypassing Hooks (Not Recommended)

In exceptional cases, you can bypass the hooks:

```bash
git commit --no-verify -m "Your commit message"
```

⚠️ **Warning**: Bypassing hooks should be avoided as it may introduce code quality issues into the repository.

## Customization

### Adding New Quality Checks

Edit the hook scripts in this directory and reinstall:

1. **PowerShell version**: `hooks/pre-commit.ps1`
2. **Bash version**: `hooks/pre-commit`

Look for the pattern matching sections in each file to add new checks.

### Modifying Protocols

The hooks reference protocol files in `/copilot/protocols/`:
- `review.md` - Code review guidelines
- `refactor.md` - Refactoring patterns

Update these files to customize the review and refactoring guidance.

## Troubleshooting

### Hook not running
- Verify the hook is installed: `ls -la .git/hooks/pre-commit`
- Ensure execute permissions (Unix/Linux/Mac): `chmod +x .git/hooks/pre-commit`
- Check that `.git/hooks/pre-commit` exists and is not `.sample`

### PowerShell execution policy errors (Windows)
```powershell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

### Protocol files not found
Ensure the `/copilot/protocols/` directory exists with:
- `review.md`
- `refactor.md`

## Reports

After each commit attempt, detailed reports are generated:

- **Location**: `.git/hooks/reports/`
- **Files**:
  - `last-review.md` - Latest code review results
  - `last-refactor.md` - Latest refactoring suggestions

These reports are not tracked by Git and remain local to your repository.

## Requirements

- Git 2.0 or higher
- **Windows**: PowerShell 5.1 or higher
- **Unix/Linux/Mac**: Bash 4.0 or higher

## Contributing

To improve these hooks:

1. Modify the scripts in the `hooks/` directory
2. Test thoroughly on your platform
3. Update this README with any new features or changes
4. Commit and push your changes

---

**Note**: Git hooks are not automatically distributed when cloning a repository for security reasons. Each team member must install the hooks locally using the installation instructions above.
