# NexusBrief

A JavaFX 17 desktop news digest application featuring a bold black-and-white editorial design. NexusBrief aggregates news into a clean, readable dashboard with user authentication, browsing history, admin controls, and profile management — all built with pure Java and custom CSS styling.

## Features

- News aggregation with live API integration
- Google Calendar integration for scheduling
- User authentication (login / register)
- Browsing history tracking
- Admin panel for user management
- Profile management
- Clean old-school editorial UI

## Requirements

- Java 17+
- JavaFX 17 (included in `lib/`)
- Microsoft SQL Server (connection configured in `src/com/nexusbrief/dao/Database.java`)

## Setup

1. Set up the database using `database/schema.sql`
2. Update the DB connection string in `Database.java`
3. Add your Google OAuth credentials in `GoogleServices.java`
4. Run the app:

**Windows:**
```
run.bat
```

**Linux/macOS:**
```
./run.sh
```

## Project Structure

```
src/
├── com/nexusbrief/
│   ├── dao/          # Database access
│   ├── model/        # Data models
│   ├── service/      # News & Google Calendar services
│   └── ui/           # JavaFX screens
├── icons/            # SVG icons
└── login icons/      # Login screen assets
database/
└── schema.sql        # Database schema
lib/                  # JavaFX & JDBC JARs
```
