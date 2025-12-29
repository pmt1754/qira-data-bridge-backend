#!/bin/bash
# Installation script for Git hooks (Bash)
# Run this script from the project root to install pre-commit hooks

echo "Installing Git hooks..."

PROJECT_ROOT=$(git rev-parse --show-toplevel)
HOOKS_SOURCE_DIR="$PROJECT_ROOT/hooks"
HOOKS_TARGET_DIR="$PROJECT_ROOT/.git/hooks"

# Check if source hooks directory exists
if [ ! -d "$HOOKS_SOURCE_DIR" ]; then
    echo "❌ Error: hooks/ directory not found!"
    exit 1
fi

# Create .git/hooks directory if it doesn't exist
mkdir -p "$HOOKS_TARGET_DIR"

# Copy pre-commit hook
if [ -f "$HOOKS_SOURCE_DIR/pre-commit" ]; then
    cp "$HOOKS_SOURCE_DIR/pre-commit" "$HOOKS_TARGET_DIR/pre-commit"
    chmod +x "$HOOKS_TARGET_DIR/pre-commit"
    echo "✓ Installed pre-commit hook"
else
    echo "⚠️  Warning: pre-commit hook not found in hooks/ directory"
fi

# Copy pre-commit.ps1 hook (for Windows compatibility)
if [ -f "$HOOKS_SOURCE_DIR/pre-commit.ps1" ]; then
    cp "$HOOKS_SOURCE_DIR/pre-commit.ps1" "$HOOKS_TARGET_DIR/pre-commit.ps1"
    echo "✓ Installed pre-commit.ps1 hook"
else
    echo "⚠️  Warning: pre-commit.ps1 hook not found in hooks/ directory"
fi

echo ""
echo "✅ Git hooks installed successfully!"
echo ""
echo "The pre-commit hook will now run automatically before each commit."
echo "It will check your code for quality issues and block commits if problems are found."
echo ""
echo "OPTIONAL: Enable GitHub Copilot CLI hooks for AI-powered reviews"
echo "  See hooks/COPILOT_CLI_HOOKS.md for setup instructions"
echo ""
echo "To bypass the hooks (not recommended), use: git commit --no-verify"
echo ""
