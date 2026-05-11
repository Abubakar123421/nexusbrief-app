package com.nexusbrief.service;

import com.nexusbrief.model.AppData.*;
import java.util.ArrayList;
import java.util.List;

interface DigestObserver {
    void onFeedbackSubmitted(String userID, String itemID, String rating);
}

interface PrioritizationStrategy {
    // Ranks items for the digst
    List<DigestItem> rank(List<DigestItem> items, int topN);
    String getName();
}

class RecencyStrategy implements PrioritizationStrategy {
    public List<DigestItem> rank(List<DigestItem> items, int topN) {
        List<DigestItem> sorted = new ArrayList<>(items);
        sorted.sort((a, b) -> {
            if (a.getPublishedAt() == null) return 1;
            if (b.getPublishedAt() == null) return -1;
            return b.getPublishedAt().compareTo(a.getPublishedAt());
        });
        return sorted.subList(0, Math.min(topN, sorted.size()));
    }
    public String getName() { return "Latest First"; }
}

class RelevanceStrategy implements PrioritizationStrategy {
    public List<DigestItem> rank(List<DigestItem> items, int topN) {
        List<DigestItem> sorted = new ArrayList<>(items);
        sorted.sort((a, b) -> Integer.compare(a.getItemRank(), b.getItemRank()));
        return sorted.subList(0, Math.min(topN, sorted.size()));
    }
    public String getName() { return "Most Relevant"; }
}

class PopularityStrategy implements PrioritizationStrategy {
    public List<DigestItem> rank(List<DigestItem> items, int topN) {
        List<DigestItem> sorted = new ArrayList<>(items);
        sorted.sort((a, b) -> {
            int lenA = a.getSummary() == null ? 0 : a.getSummary().length();
            int lenB = b.getSummary() == null ? 0 : b.getSummary().length();
            return Integer.compare(lenB, lenA);
        });
        return sorted.subList(0, Math.min(topN, sorted.size()));
    }
    public String getName() { return "Most Popular"; }
}

class StrategyFactory {
    static PrioritizationStrategy create(PrioritizationMode mode) {
        if (mode == null) return new RecencyStrategy();
        switch (mode) {
            case MOST_RELEVANT: return new RelevanceStrategy();
            case MOST_POPULAR:  return new PopularityStrategy();
            default:            return new RecencyStrategy();
        }
    }
}
