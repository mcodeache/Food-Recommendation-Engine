import java.util.HashMap;
import java.util.Map;

public class SentimentAnalysis {

    // Example keywords indicating sentiment for each aspect
    private static final Map<String, String[]> ASPECT_KEYWORDS = new HashMap<>();
    static {
        ASPECT_KEYWORDS.put("taste", new String[]{"tasty", "delicious", "good"});
        ASPECT_KEYWORDS.put("service", new String[]{"excellent", "great", "good service"});
    }

    public static SentimentAnalysisResult analyzeFeedback(String feedback) {
        // Initialize aspect-based sentiment scores
        Map<String, Integer> aspectSentimentScores = new HashMap<>();
        aspectSentimentScores.put("taste", 0);
        aspectSentimentScores.put("service", 0);

        // Initialize general sentiment
        String generalSentiment = "neutral"; // Default to neutral sentiment

        // Simulated sentiment analysis based on keywords
        String[] words = feedback.toLowerCase().split("\\s+");

        for (String word : words) {
            for (Map.Entry<String, String[]> entry : ASPECT_KEYWORDS.entrySet()) {
                String aspect = entry.getKey();
                String[] keywords = entry.getValue();

                // Check if the word matches any keywords for the current aspect
                boolean isPositive = false;
                boolean isNegative = false;

                for (String keyword : keywords) {
                    if (word.contains(keyword)) {
                        isPositive = true;
                        break;
                    }
                }

                if (word.contains("not") || word.contains("bad")) {
                    isNegative = true;
                }

                // Update sentiment scores based on positive or negative match
                if (isPositive) {
                    aspectSentimentScores.put(aspect, aspectSentimentScores.get(aspect) + 1);
                    generalSentiment = "positive"; // Update general sentiment
                } else if (isNegative) {
                    aspectSentimentScores.put(aspect, aspectSentimentScores.get(aspect) - 1);
                    generalSentiment = "negative"; // Update general sentiment
                }
            }
        }

        // Create and return sentiment analysis result object
        return new SentimentAnalysisResult(aspectSentimentScores, generalSentiment);
    }

    public static String getSentimentLabel(String aspect, int sentimentScore) {
        // Determine the sentiment label based on the sentiment score
        if (sentimentScore > 0) {
            return aspect + ": positive";
        } else if (sentimentScore < 0) {
            return aspect + ": negative";
        } else {
            return aspect + ": neutral";
        }
    }
}
