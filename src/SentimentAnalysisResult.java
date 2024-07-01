import java.util.Map;

public class SentimentAnalysisResult {
    private Map<String, Integer> aspectSentimentScores;
    private String generalSentiment;

    public SentimentAnalysisResult(Map<String, Integer> aspectSentimentScores, String generalSentiment) {
        this.aspectSentimentScores = aspectSentimentScores;
        this.generalSentiment = generalSentiment;
    }

    public Map<String, Integer> getAspectSentimentScores() {
        return aspectSentimentScores;
    }

}
