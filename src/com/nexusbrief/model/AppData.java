package com.nexusbrief.model;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public final class AppData {

    // ── Enums

    public enum PrioritizationMode {
        LATEST_FIRST("Latest First"), MOST_RELEVANT("Most Relevant"), MOST_POPULAR("Most Popular");

        private final String display;

        PrioritizationMode(String d) {
            this.display = d;
        }

        public String getDisplay() {
            return display;
        }

        public String toString() {
            return display;
        }
    }

    public enum DigestStatus {
        GENERATED, SENT, FAILED
    }

    public enum FeedbackType {
        THUMBS_UP, THUMBS_DOWN
    }

    // ── Model classes

    public static class User {
        private String userID, name, email, passwordHash;
        private boolean isVerified, isAdmin;
        private LocalDateTime createdAt;

        public User() {
        }

        // Creates a new user obj
        public User(String userID, String name, String email, String passwordHash,
                boolean isVerified, LocalDateTime createdAt, boolean isAdmin) {
            this.userID = userID;
            this.name = name;
            this.email = email;
            this.passwordHash = passwordHash;
            this.isVerified = isVerified;
            this.createdAt = createdAt;
            this.isAdmin = isAdmin;
        }

        public String getUserID() {
            return userID;
        }

        public String getName() {
            return name;
        }

        public String getEmail() {
            return email;
        }

        public String getPasswordHash() {
            return passwordHash;
        }

        public boolean isVerified() {
            return isVerified;
        }

        public boolean isAdmin() {
            return isAdmin;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setUserID(String v) {
            userID = v;
        }

        public void setName(String v) {
            name = v;
        }

        public void setEmail(String v) {
            email = v;
        }

        public void setPasswordHash(String v) {
            passwordHash = v;
        }

        public void setVerified(boolean v) {
            isVerified = v;
        }

        public void setAdmin(boolean v) {
            isAdmin = v;
        }

        public void setCreatedAt(LocalDateTime v) {
            createdAt = v;
        }

        public String toString() {
            return name + " <" + email + ">";
        }
    }

    public static class Digest {
        private String digestID, userID, userName;
        private LocalDateTime generatedAt;
        private DigestStatus status;
        private boolean isViewed;
        private List<DigestItem> items = new ArrayList<>();

        public Digest() {
        }

        public Digest(String digestID, String userID, LocalDateTime generatedAt,
                DigestStatus status, boolean isViewed) {
            this.digestID = digestID;
            this.userID = userID;
            this.generatedAt = generatedAt;
            this.status = status;
            this.isViewed = isViewed;
        }

        public String getDigestID() {
            return digestID;
        }

        public String getUserID() {
            return userID;
        }

        public String getUserName() {
            return userName;
        }

        public LocalDateTime getGeneratedAt() {
            return generatedAt;
        }

        public DigestStatus getStatus() {
            return status;
        }

        public boolean isViewed() {
            return isViewed;
        }

        public List<DigestItem> getItems() {
            return items;
        }

        public void setDigestID(String v) {
            digestID = v;
        }

        public void setUserID(String v) {
            userID = v;
        }

        public void setUserName(String v) {
            userName = v;
        }

        public void setGeneratedAt(LocalDateTime v) {
            generatedAt = v;
        }

        public void setStatus(DigestStatus v) {
            status = v;
        }

        public void setViewed(boolean v) {
            isViewed = v;
        }

        public void setItems(List<DigestItem> v) {
            items = v;
        }

        public void addItem(DigestItem i) {
            items.add(i);
        }
    }

    public static class DigestItem {
        private String itemID, digestID, sourceID, headline, summary, sourceURL, thumbnailURL;
        private LocalDateTime publishedAt;
        private int itemRank;
        private boolean isExported;

        public DigestItem() {
        }

        public DigestItem(String itemID, String digestID, String sourceID, String headline,
                String summary, String sourceURL, LocalDateTime publishedAt,
                int itemRank, boolean isExported) {
            this.itemID = itemID;
            this.digestID = digestID;
            this.sourceID = sourceID;
            this.headline = headline;
            this.summary = summary;
            this.sourceURL = sourceURL;
            this.publishedAt = publishedAt;
            this.itemRank = itemRank;
            this.isExported = isExported;
        }

        public String getItemID() {
            return itemID;
        }

        public String getDigestID() {
            return digestID;
        }

        public String getSourceID() {
            return sourceID;
        }

        public String getHeadline() {
            return headline;
        }

        public String getSummary() {
            return summary;
        }

        public String getSourceURL() {
            return sourceURL;
        }

        public String getThumbnailURL() {
            return thumbnailURL;
        }

        public LocalDateTime getPublishedAt() {
            return publishedAt;
        }

        public int getItemRank() {
            return itemRank;
        }

        public boolean isExported() {
            return isExported;
        }

        public void setItemID(String v) {
            itemID = v;
        }

        public void setDigestID(String v) {
            digestID = v;
        }

        public void setSourceID(String v) {
            sourceID = v;
        }

        public void setHeadline(String v) {
            headline = v;
        }

        public void setSummary(String v) {
            summary = v;
        }

        public void setSourceURL(String v) {
            sourceURL = v;
        }

        public void setThumbnailURL(String v) {
            thumbnailURL = v;
        }

        public void setPublishedAt(LocalDateTime v) {
            publishedAt = v;
        }

        public void setItemRank(int v) {
            itemRank = v;
        }

        public void setExported(boolean v) {
            isExported = v;
        }
    }

    public static class DigestPreference {
        private String preferenceID, userID;
        private PrioritizationMode prioritizationMode;
        private LocalTime deliveryTime;
        private LocalDateTime updatedAt;
        private List<PreferenceCategory> categories = new ArrayList<>();

        public DigestPreference() {
        }

        public DigestPreference(String preferenceID, String userID, PrioritizationMode mode,
                LocalTime deliveryTime, LocalDateTime updatedAt) {
            this.preferenceID = preferenceID;
            this.userID = userID;
            this.prioritizationMode = mode;
            this.deliveryTime = deliveryTime;
            this.updatedAt = updatedAt;
        }

        public String getPreferenceID() {
            return preferenceID;
        }

        public String getUserID() {
            return userID;
        }

        public PrioritizationMode getPrioritizationMode() {
            return prioritizationMode;
        }

        public LocalTime getDeliveryTime() {
            return deliveryTime;
        }

        public LocalDateTime getUpdatedAt() {
            return updatedAt;
        }

        public List<PreferenceCategory> getCategories() {
            return categories;
        }

        public void setPreferenceID(String v) {
            preferenceID = v;
        }

        public void setUserID(String v) {
            userID = v;
        }

        public void setPrioritizationMode(PrioritizationMode v) {
            prioritizationMode = v;
        }

        public void setDeliveryTime(LocalTime v) {
            deliveryTime = v;
        }

        public void setUpdatedAt(LocalDateTime v) {
            updatedAt = v;
        }

        public void setCategories(List<PreferenceCategory> v) {
            categories = v;
        }
    }

    public static class PreferenceCategory {
        private String categoryID, preferenceID, categoryName;
        private float weight;

        public PreferenceCategory() {
        }

        public PreferenceCategory(String categoryID, String preferenceID, String categoryName, float weight) {
            this.categoryID = categoryID;
            this.preferenceID = preferenceID;
            this.categoryName = categoryName;
            this.weight = weight;
        }

        public String getCategoryID() {
            return categoryID;
        }

        public String getPreferenceID() {
            return preferenceID;
        }

        public String getCategoryName() {
            return categoryName;
        }

        public float getWeight() {
            return weight;
        }

        public void setCategoryID(String v) {
            categoryID = v;
        }

        public void setPreferenceID(String v) {
            preferenceID = v;
        }

        public void setCategoryName(String v) {
            categoryName = v;
        }

        public void setWeight(float v) {
            weight = v;
        }

        public String toString() {
            return categoryName;
        }
    }

    public static class NewsSource {
        private String sourceID, name, category, apiEndpoint;
        private boolean isAvailable;

        public NewsSource() {
        }

        public NewsSource(String sourceID, String name, String category, String apiEndpoint, boolean isAvailable) {
            this.sourceID = sourceID;
            this.name = name;
            this.category = category;
            this.apiEndpoint = apiEndpoint;
            this.isAvailable = isAvailable;
        }

        public String getSourceID() {
            return sourceID;
        }

        public String getName() {
            return name;
        }

        public String getCategory() {
            return category;
        }

        public String getApiEndpoint() {
            return apiEndpoint;
        }

        public boolean isAvailable() {
            return isAvailable;
        }

        public void setSourceID(String v) {
            sourceID = v;
        }

        public void setName(String v) {
            name = v;
        }

        public void setCategory(String v) {
            category = v;
        }

        public void setApiEndpoint(String v) {
            apiEndpoint = v;
        }

        public void setAvailable(boolean v) {
            isAvailable = v;
        }

        public String toString() {
            return name + " [" + category + "]";
        }
    }

    public static class DigestSchedule {
        private String scheduleID, userID;
        private LocalTime deliveryTime;
        private int intervalHours;
        private LocalDateTime nextTriggerAt;
        private boolean isActive;

        public DigestSchedule() {
        }

        public DigestSchedule(String scheduleID, String userID, LocalTime deliveryTime,
                int intervalHours, LocalDateTime nextTriggerAt, boolean isActive) {
            this.scheduleID = scheduleID;
            this.userID = userID;
            this.deliveryTime = deliveryTime;
            this.intervalHours = intervalHours;
            this.nextTriggerAt = nextTriggerAt;
            this.isActive = isActive;
        }

        public String getScheduleID() {
            return scheduleID;
        }

        public String getUserID() {
            return userID;
        }

        public LocalTime getDeliveryTime() {
            return deliveryTime;
        }

        public int getIntervalHours() {
            return intervalHours;
        }

        public LocalDateTime getNextTriggerAt() {
            return nextTriggerAt;
        }

        public boolean isActive() {
            return isActive;
        }

        public void setScheduleID(String v) {
            scheduleID = v;
        }

        public void setUserID(String v) {
            userID = v;
        }

        public void setDeliveryTime(LocalTime v) {
            deliveryTime = v;
        }

        public void setIntervalHours(int v) {
            intervalHours = v;
        }

        public void setNextTriggerAt(LocalDateTime v) {
            nextTriggerAt = v;
        }

        public void setActive(boolean v) {
            isActive = v;
        }
    }

    public static class ExternalEmailAccount {
        private String emailID, userID, emailAddress, provider, accessToken, refreshToken;
        private boolean isLinked;
        private LocalDateTime linkedAt;

        public ExternalEmailAccount() {
        }

        public ExternalEmailAccount(String emailID, String userID, String emailAddress, String provider,
                String accessToken, String refreshToken, boolean isLinked, LocalDateTime linkedAt) {
            this.emailID = emailID;
            this.userID = userID;
            this.emailAddress = emailAddress;
            this.provider = provider;
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.isLinked = isLinked;
            this.linkedAt = linkedAt;
        }

        public String getEmailID() {
            return emailID;
        }

        public String getUserID() {
            return userID;
        }

        public String getEmailAddress() {
            return emailAddress;
        }

        public String getProvider() {
            return provider;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }

        public boolean isLinked() {
            return isLinked;
        }

        public LocalDateTime getLinkedAt() {
            return linkedAt;
        }

        public void setEmailID(String v) {
            emailID = v;
        }

        public void setUserID(String v) {
            userID = v;
        }

        public void setEmailAddress(String v) {
            emailAddress = v;
        }

        public void setProvider(String v) {
            provider = v;
        }

        public void setAccessToken(String v) {
            accessToken = v;
        }

        public void setRefreshToken(String v) {
            refreshToken = v;
        }

        public void setLinked(boolean v) {
            isLinked = v;
        }

        public void setLinkedAt(LocalDateTime v) {
            linkedAt = v;
        }
    }

    public static class ConnectedSource {
        private String connectionID, userID, sourceID, status, sourceName;
        private LocalDateTime connectedAt;

        public ConnectedSource() {
        }

        public ConnectedSource(String connectionID, String userID, String sourceID,
                String status, LocalDateTime connectedAt) {
            this.connectionID = connectionID;
            this.userID = userID;
            this.sourceID = sourceID;
            this.status = status;
            this.connectedAt = connectedAt;
        }

        public String getConnectionID() {
            return connectionID;
        }

        public String getUserID() {
            return userID;
        }

        public String getSourceID() {
            return sourceID;
        }

        public String getStatus() {
            return status;
        }

        public String getSourceName() {
            return sourceName;
        }

        public LocalDateTime getConnectedAt() {
            return connectedAt;
        }

        public void setConnectionID(String v) {
            connectionID = v;
        }

        public void setUserID(String v) {
            userID = v;
        }

        public void setSourceID(String v) {
            sourceID = v;
        }

        public void setStatus(String v) {
            status = v;
        }

        public void setSourceName(String v) {
            sourceName = v;
        }

        public void setConnectedAt(LocalDateTime v) {
            connectedAt = v;
        }
    }

    public static class Feedback {
        private String feedbackID, itemID, userID, reason;
        private FeedbackType rating;
        private LocalDateTime submittedAt;

        public Feedback() {
        }

        public Feedback(String feedbackID, String itemID, String userID,
                FeedbackType rating, String reason, LocalDateTime submittedAt) {
            this.feedbackID = feedbackID;
            this.itemID = itemID;
            this.userID = userID;
            this.rating = rating;
            this.reason = reason;
            this.submittedAt = submittedAt;
        }

        public String getFeedbackID() {
            return feedbackID;
        }

        public String getItemID() {
            return itemID;
        }

        public String getUserID() {
            return userID;
        }

        public FeedbackType getRating() {
            return rating;
        }

        public String getReason() {
            return reason;
        }

        public LocalDateTime getSubmittedAt() {
            return submittedAt;
        }

        public void setFeedbackID(String v) {
            feedbackID = v;
        }

        public void setItemID(String v) {
            itemID = v;
        }

        public void setUserID(String v) {
            userID = v;
        }

        public void setRating(FeedbackType v) {
            rating = v;
        }

        public void setReason(String v) {
            reason = v;
        }

        public void setSubmittedAt(LocalDateTime v) {
            submittedAt = v;
        }
    }

    public static class CalendarExport {
        private String exportID, itemID, eventTitle, calendarProvider;
        private LocalDateTime eventDate, exportedAt;

        public CalendarExport() {
        }

        public CalendarExport(String exportID, String itemID, String eventTitle,
                LocalDateTime eventDate, String calendarProvider, LocalDateTime exportedAt) {
            this.exportID = exportID;
            this.itemID = itemID;
            this.eventTitle = eventTitle;
            this.eventDate = eventDate;
            this.calendarProvider = calendarProvider;
            this.exportedAt = exportedAt;
        }

        public String getExportID() {
            return exportID;
        }

        public String getItemID() {
            return itemID;
        }

        public String getEventTitle() {
            return eventTitle;
        }

        public LocalDateTime getEventDate() {
            return eventDate;
        }

        public String getCalendarProvider() {
            return calendarProvider;
        }

        public LocalDateTime getExportedAt() {
            return exportedAt;
        }
    }
}
