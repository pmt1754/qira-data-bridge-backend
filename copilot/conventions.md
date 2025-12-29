# Coding & Naming Conventions

## Java Naming
- Classes: PascalCase (e.g., UserService)
- Methods: camelCase
- Constants: UPPER_SNAKE_CASE
- DTOs end with *Dto
- Entities use singular names
- Services end with *Service
- Repositories end with *Repository
- Controllers end with *Controller

## React Naming
- Components: PascalCase
- Hooks: useXxxx
- Context providers: XxxProvider
- State variables: camelCase

## File & Folder Structure
### Java (Spring Boot)
- controllers/
- services/
- repositories/
- entities/
- dto/
- mappers/
- config/
- exceptions/
- utils/

### React
- src/components/
- src/hooks/
- src/context/
- src/services/
- src/pages/
- src/utils/

## Error Handling (Java)
- Use GlobalExceptionHandler
- Throw custom exceptions only
- Return meaningful messages
- Map validation failures to 400
- Log errors with stack trace

## API Rules
- Never expose Entity directly
- Always convert Entity ↔ DTO
- Controller only handles routing/validation
- Service handles all business logic
- Repository handles all DB calls

## Logging
- Use SLF4J
- INFO: success flow
- DEBUG: internal steps
- ERROR: failures

## Testing
- Use JUnit + Mockito
- Test service logic separately
- Each PR must include tests for new code
