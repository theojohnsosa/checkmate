package com.example.authtest;

// Data model for FAQ question/answer pairs
public class FaqItem {

    private String question;
    private String answer;
    private String category;

    /*
        Takes question, answer, and category strings
        Stores all three fields for retrieval
     */
    public FaqItem(
            String question,
            String answer,
            String category
    ) {
        this.question = question;
        this.answer = answer;
        this.category = category;
    }

    /*
        Provides access to all three fields
        Allows filtering by category and searching by question/answer text
     */

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}