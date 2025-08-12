package in.pubbs.pubbsadmin.Model;

public class FAQList {
    private String question, answer;

    public FAQList(String question, String answer) {
        this.question = question;
        this.answer = answer;
    }

    public FAQList() {
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }
}
