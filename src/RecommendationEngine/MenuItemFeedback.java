package RecommendationEngine;

import java.util.HashSet;
import java.util.Set;

public class MenuItemFeedback {
    private final int menuItemId;
    private final String itemName;
    private int totalRating;
    private double totalSentiment;
    private int feedbackCount;
    private Set<String> keywords;

    public MenuItemFeedback(int menuItemId, String itemName) {
        this.menuItemId = menuItemId;
        this.itemName = itemName;
    }

    public void addFeedback(int rating, double sentimentScore, String sentiment) {
        this.totalRating += rating;
        this.totalSentiment += sentimentScore;
        this.feedbackCount++;
    }
    public int getMenuItemId() {
        return menuItemId;
    }

    public String getItemName() {
        return itemName;
    }

    public double getAverageRating() {
        return (double) totalRating / feedbackCount;
    }

    public double getAverageSentiment() {
        return totalSentiment / feedbackCount;
    }

    public int getFeedbackCount() {
        return feedbackCount;
    }

    public void extractKeywordsFromFeedback(String feedback) {
        keywords = new HashSet<>();

        String[] words = feedback.split("\\s+");
        String[] categories = {"service", "taste", "quality"};

        for (String word : words) {
            for (String category : categories) {
                if (word.toLowerCase().contains(category)) {
                    keywords.add(category);
                    break; // Once a category match is found, break the inner loop
                }
            }
        }
    }

    public Set<String> getKeywords() {
        return keywords;
    }

    @Override
    public String toString() {
        return "Menu Item: " + itemName +
                " (ID: " + menuItemId + ")" +
                ", Average Rating: " + String.format("%.2f", getAverageRating()) +
                ", Average Sentiment: " + String.format("%.2f", getAverageSentiment()) +
                ", Feedback Count: " + feedbackCount;
    }
}