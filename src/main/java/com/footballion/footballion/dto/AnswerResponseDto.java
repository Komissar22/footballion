package com.footballion.footballion.dto;

public class AnswerResponseDto {
    private boolean correct;
    private int rarityScore;
    private String rarityLabel;
    private String entityName;
    private String imageUrl;
    private double pickPercentage;

    public AnswerResponseDto() {}

    public AnswerResponseDto(boolean correct, int rarityScore, String rarityLabel, String entityName, String imageUrl, double pickPercentage) {
        this.correct = correct;
        this.rarityScore = rarityScore;
        this.rarityLabel = rarityLabel;
        this.entityName = entityName;
        this.imageUrl = imageUrl;
        this.pickPercentage = pickPercentage;
    }

    public boolean isCorrect() { return correct; }
    public void setCorrect(boolean correct) { this.correct = correct; }

    public int getRarityScore() { return rarityScore; }
    public void setRarityScore(int rarityScore) { this.rarityScore = rarityScore; }

    public String getRarityLabel() { return rarityLabel; }
    public void setRarityLabel(String rarityLabel) { this.rarityLabel = rarityLabel; }

    public String getEntityName() { return entityName; }
    public void setEntityName(String entityName) { this.entityName = entityName; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public double getPickPercentage() { return pickPercentage; }
    public void setPickPercentage(double pickPercentage) { this.pickPercentage = pickPercentage; }
}