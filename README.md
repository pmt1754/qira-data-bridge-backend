# QIRA Data Bridge - Backend

Spring Boot application for ingesting QIRA ticket data, storing in PostgreSQL, and generating monthly reports.

## Features

- **Automated Monthly Ingestion**: Scheduled job runs monthly to fetch tickets from QIRA REST API
- **Bulk Upsert**: Efficiently processes and upserts tickets with idempotent operations
- **Excel Reporting**: Generates comprehensive Excel reports with summary and detailed data sheets
- **Email Notifications**: Sends monthly reports and failure notifications to configured recipients
- **Admin Endpoints**: Manual trigger and status monitoring capabilities
- **Metrics & Observability**: Prometheus metrics and detailed logging
- **Retry & Rate Limiting**: Exponential backoff retry logic with rate limit handling

## Architecture

### Core Components

1. **QiraClient** - REST client for QIRA API with pagination and retry logic
2. **TicketMapper** - Maps JSON ticket data to IssueRecord entities
3. **IssueService** - Bulk upsert operations with efficient batch processing
4. **ReportService** - Excel generation using Apache POI
5. **EmailService** - Email delivery with attachments
6. **IngestionOrchestrator** - Main orchestration flow for monthly jobs
7. **Admin/Issue Controllers** - REST endpoints for manual operations

## Configuration

### Required Environment Variables

```properties
# QIRA API
QIRA_API_KEY=your-api-key-here

# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/qira_db
spring.datasource.username=admin
spring.datasource.password=password

# Email
MAIL_USERNAME=your-email@example.com
MAIL_PASSWORD=your-password
MAIL_RECIPIENTS_MANAGEMENT=manager@example.com,director@example.com
MAIL_RECIPIENTS_SUPPORT=support@example.com
```

### Application Properties

Key configuration in `application.properties`:

- `scheduler.monthly.cron` - Cron expression for monthly job (default: `0 0 2 1 * ?` - 2 AM on 1st)
- `ingestion.page-size` - Page size for QIRA API pagination (default: 200)
- `report.output-dir` - Directory for generated reports (default: `./reports`)
- `qira.base-url` - QIRA API base URL

## API Endpoints

### Admin Endpoints

- `POST /admin/fetch-now` - Trigger manual ingestion immediately
- `GET /admin/last-run` - Get last job execution status
- `GET /admin/status` - Get current job status

### Issue Export

- `GET /issues/export?from=2024-01-01&to=2024-12-31&team=Engineering` - Export filtered data to Excel

## Database Schema

The `issue_records` table stores all ticket data with:
- Unique constraint on `qira_id` for idempotent upserts
- All QIRA fields mapped to appropriate columns
- `raw_json` column for troubleshooting
- `ingested_at` timestamp for audit

## Ingestion Flow

1. **Fetch**: QiraClient fetches all pages from QIRA API with automatic pagination
2. **Map**: TicketMapper converts JSON to IssueRecord entities
3. **Upsert**: IssueService performs bulk upsert (finds existing IDs, then batch insert/update)
4. **Report**: ReportService generates Excel with summary and detailed sheets
5. **Email**: EmailService sends report to management recipients

## Error Handling

- **Transient failures**: Retry with exponential backoff (max 5 attempts)
- **Rate limiting**: Automatic sleep between requests + retry on 429
- **Partial failures**: Job continues and reports partial stats
- **Fatal errors**: Sends failure notification email to support team

## Metrics

Exposed via Spring Actuator and Prometheus:

- `qira.ingestion.fetched` - Total tickets fetched
- `qira.ingestion.inserted` - New records inserted
- `qira.ingestion.updated` - Records updated
- `qira.ingestion.failed` - Failed operations
- `qira.ingestion.duration` - Job duration in seconds
- `qira.ingestion.job.failed` - Failed job count

## Building and Running

### With Gradle

```bash
# Build
./gradlew clean build

# Run
./gradlew bootRun
```

### With Docker

```bash
# Build image
docker build -t qira-data-bridge-backend .

# Run with docker-compose (includes PostgreSQL)
docker-compose up
```

## Testing

```bash
# Run all tests
./gradlew test

# Run with coverage
./gradlew test jacocoTestReport
```

Unit tests cover:
- TicketMapper field mapping logic
- IssueService upsert operations
- QiraClient pagination handling

## Deployment Checklist

1. Configure environment variables for QIRA API key and credentials
2. Set up PostgreSQL database and run migrations
3. Configure SMTP settings for email delivery
4. Set email recipient lists for management and support
5. Verify cron schedule matches business requirements
6. Set up Prometheus scraping for metrics endpoint
7. Configure report output directory with appropriate permissions
8. Test manual fetch endpoint before enabling scheduler

## TODO Items

See inline `// TODO:` comments for environment-specific configuration:
- Email SMTP configuration in `AppConfig.java`
- QIRA API base URL verification
- Production database connection settings
- Proper async execution for manual triggers (use @Async)
- Temp file cleanup mechanism for export endpoint
- Additional error handling for edge cases

---
Generated by SN ProtoMate

