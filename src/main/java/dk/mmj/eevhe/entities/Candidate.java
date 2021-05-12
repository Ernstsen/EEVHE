package dk.mmj.eevhe.entities;

import java.util.Objects;

/**
 * DTO for a candidate in the election
 */
@SuppressWarnings("unused")
public class Candidate {
    private int idx;
    private String name;
    private String description;

    public Candidate(int idx, String name, String description) {
        this.idx = idx;
        this.name = name;
        this.description = description;
    }

    public Candidate() {
    }

    public int getIdx() {
        return idx;
    }

    public Candidate setIdx(int idx) {
        this.idx = idx;
        return this;
    }

    public String getName() {
        return name;
    }

    public Candidate setName(String name) {
        this.name = name;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public Candidate setDescription(String description) {
        this.description = description;
        return this;
    }

    @Override
    public String toString() {
        return "Candidate{" +
                "idx=" + idx +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Candidate candidate = (Candidate) o;
        return idx == candidate.idx &&
                Objects.equals(name, candidate.name) &&
                Objects.equals(description, candidate.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idx, name, description);
    }
}
