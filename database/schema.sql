-- =====================================================================
-- NexusBrief - News Generator System
-- SQL Server Database Schema
-- Run this script in SQL Server Management Studio (SSMS)
-- =====================================================================

-- Create database
IF DB_ID('NexusBrief') IS NULL
    CREATE DATABASE NexusBrief;
GO

USE NexusBrief;
GO

-- Drop tables if they exist (for re-running script)
IF OBJECT_ID('calendar_exports', 'U') IS NOT NULL DROP TABLE calendar_exports;
IF OBJECT_ID('feedbacks', 'U') IS NOT NULL DROP TABLE feedbacks;
IF OBJECT_ID('digest_items', 'U') IS NOT NULL DROP TABLE digest_items;
IF OBJECT_ID('digests', 'U') IS NOT NULL DROP TABLE digests;
IF OBJECT_ID('external_email_accounts', 'U') IS NOT NULL DROP TABLE external_email_accounts;
IF OBJECT_ID('connected_sources', 'U') IS NOT NULL DROP TABLE connected_sources;
IF OBJECT_ID('api_configurations', 'U') IS NOT NULL DROP TABLE api_configurations;
IF OBJECT_ID('user_sources', 'U') IS NOT NULL DROP TABLE user_sources;
IF OBJECT_ID('news_sources', 'U') IS NOT NULL DROP TABLE news_sources;
IF OBJECT_ID('digest_schedules', 'U') IS NOT NULL DROP TABLE digest_schedules;
IF OBJECT_ID('preference_categories', 'U') IS NOT NULL DROP TABLE preference_categories;
IF OBJECT_ID('digest_preferences', 'U') IS NOT NULL DROP TABLE digest_preferences;
IF OBJECT_ID('users', 'U') IS NOT NULL DROP TABLE users;
GO

-- =====================================================================
-- 1. USERS
-- =====================================================================
CREATE TABLE users (
    userID VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    passwordHash VARCHAR(255) NOT NULL,
    isVerified BIT DEFAULT 0,
    createdAt DATETIME DEFAULT GETDATE(),
    isAdmin BIT DEFAULT 0
);
GO

-- =====================================================================
-- 2. DIGEST PREFERENCES (one-to-one with user)
-- =====================================================================
CREATE TABLE digest_preferences (
    preferenceID VARCHAR(36) PRIMARY KEY,
    userID VARCHAR(36) NOT NULL UNIQUE,
    prioritizationMode VARCHAR(20) DEFAULT 'LATEST_FIRST',
    deliveryTime TIME DEFAULT '08:00:00',
    updatedAt DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (userID) REFERENCES users(userID) ON DELETE CASCADE
);
GO

-- =====================================================================
-- 3. PREFERENCE CATEGORIES (many per preference)
-- =====================================================================
CREATE TABLE preference_categories (
    categoryID VARCHAR(36) PRIMARY KEY,
    preferenceID VARCHAR(36) NOT NULL,
    categoryName VARCHAR(50) NOT NULL,
    weight FLOAT DEFAULT 1.0,
    FOREIGN KEY (preferenceID) REFERENCES digest_preferences(preferenceID) ON DELETE CASCADE
);
GO

-- =====================================================================
-- 4. DIGEST SCHEDULES
-- =====================================================================
CREATE TABLE digest_schedules (
    scheduleID VARCHAR(36) PRIMARY KEY,
    userID VARCHAR(36) NOT NULL UNIQUE,
    deliveryTime TIME DEFAULT '08:00:00',
    intervalHours INT DEFAULT 24,
    nextTriggerAt DATETIME,
    isActive BIT DEFAULT 1,
    FOREIGN KEY (userID) REFERENCES users(userID) ON DELETE CASCADE
);
GO

-- =====================================================================
-- 5. NEWS SOURCES
-- =====================================================================
CREATE TABLE news_sources (
    sourceID VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,
    apiEndpoint VARCHAR(255),
    isAvailable BIT DEFAULT 1
);
GO

-- =====================================================================
-- 6. USER SOURCES (junction table - users' selected favourite sources)
-- =====================================================================
CREATE TABLE user_sources (
    userID VARCHAR(36) NOT NULL,
    sourceID VARCHAR(36) NOT NULL,
    PRIMARY KEY (userID, sourceID),
    FOREIGN KEY (userID) REFERENCES users(userID) ON DELETE CASCADE,
    FOREIGN KEY (sourceID) REFERENCES news_sources(sourceID) ON DELETE CASCADE
);
GO

-- =====================================================================
-- 7. API CONFIGURATIONS (managed by admin)
-- =====================================================================
CREATE TABLE api_configurations (
    configID VARCHAR(36) PRIMARY KEY,
    sourceID VARCHAR(36) NOT NULL UNIQUE,
    providerName VARCHAR(100) NOT NULL,
    apiKey VARCHAR(255) NOT NULL,
    isValid BIT DEFAULT 1,
    lastValidatedAt DATETIME,
    FOREIGN KEY (sourceID) REFERENCES news_sources(sourceID) ON DELETE CASCADE
);
GO

-- =====================================================================
-- 8. CONNECTED SOURCES (user's active integrations)
-- =====================================================================
CREATE TABLE connected_sources (
    connectionID VARCHAR(36) PRIMARY KEY,
    userID VARCHAR(36) NOT NULL,
    sourceID VARCHAR(36) NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    connectedAt DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (userID) REFERENCES users(userID) ON DELETE CASCADE,
    FOREIGN KEY (sourceID) REFERENCES news_sources(sourceID) ON DELETE CASCADE
);
GO

-- =====================================================================
-- 9. EXTERNAL EMAIL ACCOUNTS
-- =====================================================================
CREATE TABLE external_email_accounts (
    emailID VARCHAR(36) PRIMARY KEY,
    userID VARCHAR(36) NOT NULL UNIQUE,
    emailAddress VARCHAR(150) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    accessToken VARCHAR(MAX),
    refreshToken VARCHAR(MAX),
    isLinked BIT DEFAULT 0,
    linkedAt DATETIME,
    FOREIGN KEY (userID) REFERENCES users(userID) ON DELETE CASCADE
);
GO

-- =====================================================================
-- 10. DIGESTS
-- =====================================================================
CREATE TABLE digests (
    digestID VARCHAR(36) PRIMARY KEY,
    userID VARCHAR(36) NOT NULL,
    generatedAt DATETIME DEFAULT GETDATE(),
    status VARCHAR(20) DEFAULT 'GENERATED',
    isViewed BIT DEFAULT 0,
    FOREIGN KEY (userID) REFERENCES users(userID) ON DELETE CASCADE
);
GO

-- =====================================================================
-- 11. DIGEST ITEMS (1-5 per digest)
-- =====================================================================
CREATE TABLE digest_items (
    itemID VARCHAR(36) PRIMARY KEY,
    digestID VARCHAR(36) NOT NULL,
    sourceID VARCHAR(36),
    headline VARCHAR(500) NOT NULL,
    summary VARCHAR(MAX),
    sourceURL VARCHAR(500),
    thumbnailURL VARCHAR(500),
    publishedAt DATETIME,
    itemRank INT NOT NULL,
    isExported BIT DEFAULT 0,
    FOREIGN KEY (digestID) REFERENCES digests(digestID) ON DELETE CASCADE,
    FOREIGN KEY (sourceID) REFERENCES news_sources(sourceID)
);
GO

-- =====================================================================
-- 12. FEEDBACKS
-- =====================================================================
CREATE TABLE feedbacks (
    feedbackID VARCHAR(36) PRIMARY KEY,
    itemID VARCHAR(36) NOT NULL,
    userID VARCHAR(36) NOT NULL,
    rating VARCHAR(20) NOT NULL,
    reason VARCHAR(255),
    submittedAt DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (itemID) REFERENCES digest_items(itemID) ON DELETE CASCADE,
    FOREIGN KEY (userID) REFERENCES users(userID)
);
GO

-- =====================================================================
-- 13. CALENDAR EXPORTS
-- =====================================================================
CREATE TABLE calendar_exports (
    exportID VARCHAR(36) PRIMARY KEY,
    itemID VARCHAR(36) NOT NULL UNIQUE,
    eventTitle VARCHAR(255) NOT NULL,
    eventDate DATETIME NOT NULL,
    calendarProvider VARCHAR(50),
    exportedAt DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (itemID) REFERENCES digest_items(itemID) ON DELETE CASCADE
);
GO

-- =====================================================================
-- SEED DATA - News sources (all 8 categories, real RSS endpoints)
-- =====================================================================
INSERT INTO news_sources (sourceID, name, category, apiEndpoint, isAvailable) VALUES
-- Stocks / Finance  (all 7 confirmed working)
('src-001', 'Bloomberg',            'Stocks',     'https://feeds.bloomberg.com/markets/news.rss',                  1),
('src-006', 'CNBC',                 'Stocks',     'https://www.cnbc.com/id/100003114/device/rss/rss.html',         1),
('src-037', 'MarketWatch',          'Stocks',     'https://feeds.marketwatch.com/marketwatch/topstories/',          1),
('src-038', 'Yahoo Finance',        'Stocks',     'https://finance.yahoo.com/rss/',                                 1),
('src-039', 'Fortune',              'Stocks',     'https://fortune.com/feed/',                                      1),
('src-040', 'Seeking Alpha',        'Stocks',     'https://seekingalpha.com/feed.xml',                             1),
('src-041', 'Investing.com',        'Stocks',     'https://www.investing.com/rss/news.rss',                        1),

-- Sports  (5 confirmed working; Goal/Athletic/Bleacher removed - feeds dead)
('src-002', 'ESPN',                 'Sports',     'https://www.espn.com/espn/rss/news',                            1),
('src-008', 'BBC Sport',            'Sports',     'https://feeds.bbci.co.uk/sport/rss.xml',                        1),
('src-020', 'Sky Sports',           'Sports',     'https://www.skysports.com/rss/12040',                           1),
('src-059', 'CBS Sports',           'Sports',     'https://www.cbssports.com/rss/headlines/',                      1),
('src-060', 'NYT Sports',           'Sports',     'https://rss.nytimes.com/services/xml/rss/nyt/Sports.xml',       1),

-- Technology  (9 confirmed working + MIT Tech Review)
('src-004', 'TechCrunch',           'Technology', 'https://techcrunch.com/feed/',                                  1),
('src-007', 'The Verge',            'Technology', 'https://www.theverge.com/rss/index.xml',                        1),
('src-024', 'Ars Technica',         'Technology', 'https://feeds.arstechnica.com/arstechnica/index',               1),
('src-025', 'Wired',                'Technology', 'https://www.wired.com/feed/rss',                                1),
('src-026', 'Engadget',             'Technology', 'https://www.engadget.com/rss.xml',                              1),
('src-027', 'ZDNet',                'Technology', 'https://www.zdnet.com/news/rss.xml',                            1),
('src-028', 'CNET',                 'Technology', 'https://www.cnet.com/rss/news/',                                1),
('src-029', 'MIT Technology Review','Technology', 'https://www.technologyreview.com/feed/',                        1),
('src-030', '9to5Mac',              'Technology', 'https://9to5mac.com/feed/',                                     1),
('src-031', 'Android Authority',    'Technology', 'https://www.androidauthority.com/feed/',                        1),

-- Crypto  (5 confirmed working; Bitcoin Magazine removed - blocked 403)
('src-003', 'CoinDesk',             'Crypto',     'https://www.coindesk.com/arc/outboundfeeds/rss/',               1),
('src-032', 'CoinTelegraph',        'Crypto',     'https://cointelegraph.com/rss',                                  1),
('src-033', 'Decrypt',              'Crypto',     'https://decrypt.co/feed',                                        1),
('src-035', 'The Block',            'Crypto',     'https://www.theblock.co/rss.xml',                               1),
('src-036', 'CryptoNews',           'Crypto',     'https://cryptonews.com/news/feed/',                             1),

-- General / World  (8 confirmed working; Reuters/AP News removed - no public RSS)
('src-009', 'BBC News',             'General',    'https://feeds.bbci.co.uk/news/world/rss.xml',                   1),
('src-011', 'CNN',                  'General',    'http://rss.cnn.com/rss/edition.rss',                            1),
('src-012', 'The Guardian',         'General',    'https://www.theguardian.com/world/rss',                         1),
('src-013', 'Al Jazeera',           'General',    'https://www.aljazeera.com/xml/rss/all.xml',                     1),
('src-014', 'NPR',                  'General',    'https://feeds.npr.org/1001/rss.xml',                            1),
('src-015', 'ABC News',             'General',    'https://abcnews.go.com/abcnews/topstories',                     1),
('src-016', 'Fox News',             'General',    'https://moxie.foxnews.com/google-publisher/latest.xml',         1),
('src-018', 'New York Times',       'General',    'https://rss.nytimes.com/services/xml/rss/nyt/World.xml',        1),
('src-019', 'Sky News',             'General',    'https://feeds.skynews.com/feeds/rss/world.xml',                 1),

-- Politics  (4 confirmed working; Politico URL fixed; Foreign Policy/Slate removed)
('src-053', 'Politico',             'Politics',   'https://www.politico.com/rss/politicopicks.xml',                1),
('src-054', 'The Hill',             'Politics',   'https://thehill.com/feed/',                                     1),
('src-055', 'Axios',                'Politics',   'https://api.axios.com/feed/',                                   1),
('src-057', 'Vox',                  'Politics',   'https://www.vox.com/rss/index.xml',                            1),
('src-061', 'NPR Politics',         'Politics',   'https://feeds.npr.org/1014/rss.xml',                           1),

-- Business  (3 sources; FT URL fixed; Business Insider URL fixed)
('src-042', 'Financial Times',      'Business',   'https://www.ft.com/rss/home',                                   1),
('src-043', 'The Economist',        'Business',   'https://www.economist.com/the-world-this-week/rss.xml',         1),
('src-044', 'Business Insider',     'Business',   'https://www.businessinsider.com/rss',                           1),

-- Science  (7 confirmed working)
('src-045', 'Science Daily',        'Science',    'https://www.sciencedaily.com/rss/all.xml',                      1),
('src-046', 'NASA',                 'Science',    'https://www.nasa.gov/news-release/feed/',                        1),
('src-047', 'New Scientist',        'Science',    'https://www.newscientist.com/feed/home/',                        1),
('src-048', 'Scientific American',  'Science',    'https://rss.sciam.com/ScientificAmerican-Global',               1),
('src-049', 'Phys.org',             'Science',    'https://phys.org/rss-feed/',                                    1),
('src-050', 'Nature',               'Science',    'https://www.nature.com/nature.rss',                             1),
('src-051', 'Space.com',            'Science',    'https://www.space.com/feeds/all',                               1),
('src-052', 'Live Science',         'Science',    'https://www.livescience.com/feeds/all',                         1);
GO

-- =====================================================================
-- SEED DATA - API configurations (one entry per source for demo)
-- =====================================================================
INSERT INTO api_configurations (configID, sourceID, providerName, apiKey, isValid, lastValidatedAt) VALUES
('cfg-001', 'src-001', 'Bloomberg RSS',           'RSS-PUBLIC', 1, GETDATE()),
('cfg-002', 'src-002', 'ESPN RSS',                'RSS-PUBLIC', 1, GETDATE()),
('cfg-003', 'src-003', 'CoinDesk RSS',            'RSS-PUBLIC', 1, GETDATE()),
('cfg-004', 'src-004', 'TechCrunch RSS',          'RSS-PUBLIC', 1, GETDATE()),
('cfg-005', 'src-006', 'CNBC RSS',                'RSS-PUBLIC', 1, GETDATE()),
('cfg-006', 'src-007', 'The Verge RSS',           'RSS-PUBLIC', 1, GETDATE()),
('cfg-007', 'src-008', 'BBC Sport RSS',           'RSS-PUBLIC', 1, GETDATE()),
('cfg-008', 'src-009', 'BBC News RSS',            'RSS-PUBLIC', 1, GETDATE()),
('cfg-009', 'src-011', 'CNN RSS',                 'RSS-PUBLIC', 1, GETDATE()),
('cfg-010', 'src-012', 'The Guardian RSS',        'RSS-PUBLIC', 1, GETDATE());
GO

-- =====================================================================
-- SEED DATA - Default Admin Account
-- =====================================================================
INSERT INTO users (userID, name, email, passwordHash, isVerified, createdAt, isAdmin)
VALUES (NEWID(), 'Admin', 'admin@nexusbrief.com', 'Admin123', 1, GETDATE(), 1);
GO

PRINT 'NexusBrief database schema created successfully.';
PRINT 'Admin login → Email: admin@nexusbrief.com | Password: Admin123';
GO