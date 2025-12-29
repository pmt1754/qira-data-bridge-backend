# Copilot Review Protocol

Review code using the following rules:

## Architecture
- Follow Controller → Service → Repository pattern
- No business logic in Controllers
- No DB logic in Services
- DTOs must be used for requests/responses

## Coding Standards
- Follow naming conventions in /copilot/conventions.md
- Follow patterns in /copilot/patterns.md

## Quality Checks
- Validate inputs
- Add null checks
- Add proper logging
- Avoid duplicate code
- Ensure methods are small & clean

## Testing
- New logic must have test coverage
- Ensure mocking is used properly

## Output Format
1. Problems found
2. Suggested improvements
3. Revised code (if needed)
