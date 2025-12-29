# System Architecture Standards

## Layered Architecture (Backend)
- Controller Layer:
  - Input validation
  - Routing only
  - No business logic

- Service Layer:
  - Business logic
  - Data transformations
  - Transaction boundaries

- Repository Layer:
  - Database interactions
  - No business logic

## React (Frontend)
- Components: UI-only, no business logic
- Hooks: data fetching / state logic
- Services: API calls
- Context: shared/global state

## Data Flow
Controller → Service → Repository → DB

## DTO Rules
- Use DTOs for all requests & responses
- Avoid exposing entities

## Security Rules
- Input validation at Controller
- Sensitive fields masked in logs
- No direct SQL (use JPA)

