# NexusBrief

A modern JavaFX 17 desktop news digest application featuring a bold black-and-white editorial design. NexusBrief aggregates news into a clean, readable dashboard with user authentication, browsing history tracking, and administrative capabilities.

![Java](https://img.shields.io/badge/Java-17-blue)
![JavaFX](https://img.shields.io/badge/JavaFX-17-purple)
![SQL Server](https://img.shields.io/badge/Database-SQL%20Server-brightgreen)
![License](https://img.shields.io/badge/License-MIT-yellow)

## Features

- 📰 **News Aggregation** - Aggregates news from multiple sources via live API integration
- 📅 **Google Calendar Integration** - Schedule and manage news-related events
- 🔐 **User Authentication** - Secure login and registration system
- 📱 **Browsing History** - Track and manage your reading history
- 👨‍💼 **Admin Panel** - User and content management dashboard
- 👤 **Profile Management** - Customize user profiles and preferences
- 🎨 **Clean Editorial UI** - Old-school newspaper design with modern functionality
- 🗄️ **Database Persistence** - SQL Server backend for reliable data storage

## Technology Stack

- **Frontend**: JavaFX 17 for rich desktop UI
- **Backend**: Java 17 core application logic
- **Database**: Microsoft SQL Server
- **Authentication**: Google OAuth 2.0 integration
- **Build**: Maven or direct compilation

## Prerequisites

- **Java 17** or later ([Download](https://www.oracle.com/java/technologies/downloads/))
- **JavaFX 17** (included in `lib/` directory)
- **Microsoft SQL Server** (local or remote instance)
- **Google OAuth Credentials** (for calendar integration)

## Installation

### 1. Clone the Repository

```bash
git clone https://github.com/Abubakar123421/nexusbrief-app.git
cd nexusbrief-app
```

### 2. Database Setup

1. Open SQL Server Management Studio (SSMS) or your preferred SQL client
2. Execute the database schema:
   ```sql
   -- Open and run database/schema.sql
   ```
3. Update the database connection string in `src/com/nexusbrief/dao/Database.java`:
   ```java
   // Update these values:
   private static final String SERVER = "localhost";
   private static final String DATABASE = "nexusbrief";
   private static final String USER = "your_username";
   private static final String PASSWORD = "your_password";
   ```

### 3. Google OAuth Setup (Optional)

To enable Google Calendar integration:

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project
3. Enable the Google Calendar API
4. Create OAuth 2.0 credentials (Desktop application)
5. Add your credentials to `src/com/nexusbrief/service/GoogleServices.java`:
   ```java
   private static final String CLIENT_ID = "your_client_id";
   private static final String CLIENT_SECRET = "your_client_secret";
   ```

### 4. Configure News API

Update your news API key in the appropriate service configuration:

```java
// src/com/nexusbrief/service/NewsService.java
private static final String API_KEY = "your_news_api_key";
```

### 5. Run the Application

**Windows:**
```bash
run.bat
```

**Linux/macOS:**
```bash
./run.sh
```

Alternatively, compile and run with Java:
```bash
javac -cp "lib/*:src" --module-path lib --add-modules javafx.controls src/com/nexusbrief/ui/Main.java
java -cp "lib/*:src" --module-path lib --add-modules javafx.controls com.nexusbrief.ui.Main
```

## Project Structure

```
nexusbrief-app/
├── src/
│   ├── com/nexusbrief/
│   │   ├── dao/              # Database access layer (DAO pattern)
│   │   │   ├── Database.java
│   │   │   ├── UserDAO.java
│   │   │   ├── NewsDAO.java
│   │   │   └── HistoryDAO.java
│   │   ├── model/            # Data models and entities
│   │   │   ├── User.java
│   │   │   ├── News.java
│   │   │   ├── Article.java
│   │   │   └── BrowsingHistory.java
│   │   ├── service/          # Business logic services
│   │   │   ├── NewsService.java
│   │   │   ├── GoogleServices.java
│   │   │   ├── AuthenticationService.java
│   │   │   └── UserService.java
│   │   ├── ui/               # JavaFX UI components and screens
│   │   │   ├── Main.java
│   │   │   ├── LoginScreen.java
│   │   │   ├── DashboardScreen.java
│   │   │   ├── AdminPanel.java
│   │   │   ├── ProfileScreen.java
│   │   │   └── HistoryScreen.java
│   │   └── util/             # Utility classes
│   │       ├── Config.java
│   │       └── Logger.java
│   ├── icons/                # SVG and image assets
│   └── login_icons/          # Login screen assets
├── database/
│   └── schema.sql            # SQL Server database schema
├── lib/                      # External libraries
│   ├── javafx-*.jar          # JavaFX modules
│   ├── mssql-jdbc-*.jar      # SQL Server JDBC driver
│   └── [other dependencies]
├── run.bat                   # Windows launcher
├── run.sh                    # Linux/macOS launcher
└── README.md                 # This file
```

## Usage

### First-Time Setup

1. **Register an Account**
   - Click "Register" on the login screen
   - Create a new account with email and password
   - Verify your email (if required)

2. **Log In**
   - Enter your credentials
   - Select "Remember Me" to stay logged in

### Using the Dashboard

- **Browse News**: View aggregated news from multiple sources
- **View Details**: Click on any article to read the full story
- **Save to History**: Read articles are automatically saved
- **Search**: Use the search bar to find specific topics

### Admin Features

- **User Management**: View and manage user accounts
- **Content Moderation**: Review and moderate news content
- **System Settings**: Configure application-wide settings

### Calendar Integration

- Click "Schedule Event" on any news article
- Select a date and time
- Article will be added to your Google Calendar

## Building from Source

### Using Maven

```bash
mvn clean package
mvn exec:java
```

### Manual Compilation

```bash
javac -cp "lib/*" -d bin src/com/nexusbrief/**/*.java
java -cp "bin:lib/*" --module-path lib --add-modules javafx.controls com.nexusbrief.ui.Main
```

## Configuration

Key configuration files:

- `src/com/nexusbrief/dao/Database.java` - Database connection settings
- `src/com/nexusbrief/service/GoogleServices.java` - Google OAuth settings
- `src/com/nexusbrief/util/Config.java` - Application configuration

## Troubleshooting

### Database Connection Issues

- Ensure SQL Server is running and accessible
- Verify connection credentials in `Database.java`
- Check firewall rules allow connections to port 1433 (default SQL Server port)
- Review database schema is properly installed

### JavaFX Not Loading

- Ensure JavaFX JARs are in the `lib/` directory
- Verify `--module-path` and `--add-modules` flags are set correctly
- Update JavaFX to version 17 or later

### Google Calendar Integration Not Working

- Verify OAuth credentials are correct
- Check that Google Calendar API is enabled in Google Cloud Console
- Ensure internet connection is active

## Performance Tips

- Index frequently searched fields in the database
- Clear browsing history periodically for optimal performance
- Use the admin panel to manage and clean up old news entries

## License

This project is licensed under the MIT License. See LICENSE file for details.

## Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## Support

For issues, questions, or suggestions, please:

- Open an [Issue](https://github.com/Abubakar123421/nexusbrief-app/issues)
- Start a [Discussion](https://github.com/Abubakar123421/nexusbrief-app/discussions)
- Contact the maintainer directly

## Roadmap

- [ ] Mobile app version (iOS/Android)
- [ ] Dark mode support
- [ ] RSS feed support
- [ ] Advanced filtering and categorization
- [ ] Real-time notifications
- [ ] Multi-language support
- [ ] Cloud synchronization

## Author

**Abubakar123421** - [GitHub Profile](https://github.com/Abubakar123421)

---

**Last Updated**: May 2026

For the latest updates, visit the [GitHub repository](https://github.com/Abubakar123421/nexusbrief-app).
