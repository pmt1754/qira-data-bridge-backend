# Reusable Patterns & Templates

## Controller Pattern
- Validate input
- Call service
- Return standardized ResponseEntity

## Service Pattern
- Validate business rules
- Interact with repository
- Map entity ↔ dto
- Wrap DB calls in try/catch
- Throw domain-specific exceptions

## Repository Pattern
- Extend JpaRepository
- Custom queries use JPQL
- Never expose database structure

## Exception Pattern
- Use custom exceptions:
  - ResourceNotFoundException
  - BadRequestException
  - ValidationException
- Handle via GlobalExceptionHandler

## React Patterns
- useEffect → for async API calls
- useState/useReducer → for UI state
- axios/fetch API logic inside services folder
- keep UI clean and minimal

