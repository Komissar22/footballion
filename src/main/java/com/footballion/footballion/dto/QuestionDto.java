package com.footballion.footballion.dto;

public class QuestionDto {
    private String text;
    private String type;
    private String targetValue;

    public QuestionDto(String text, String type, String targetValue) {
        this.text = text;
        this.type = type;
        this.targetValue = targetValue;
    }

    public String getText() { return text; }
    public String getType() { return type; }
    public String getTargetValue() { return targetValue; }
}