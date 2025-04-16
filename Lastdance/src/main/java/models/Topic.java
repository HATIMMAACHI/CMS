package models;



import java.util.Objects;

public class Topic {
    private int topicId;
    private int conferenceId;
    private String name;
    private Integer parentTopicId; // Utiliser Integer pour permettre NULL

    public Topic() {}

    // --- Getters and Setters ---
    public int getTopicId() { return topicId; }
    public void setTopicId(int topicId) { this.topicId = topicId; }
    public int getConferenceId() { return conferenceId; }
    public void setConferenceId(int conferenceId) { this.conferenceId = conferenceId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getParentTopicId() { return parentTopicId; }
    public void setParentTopicId(Integer parentTopicId) { this.parentTopicId = parentTopicId; }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Topic topic = (Topic) o;
        return topicId == topic.topicId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(topicId);
    }
}
