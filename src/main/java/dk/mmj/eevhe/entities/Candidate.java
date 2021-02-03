package dk.mmj.eevhe.entities;

/**
 * DTO for a candidate in the election
 */
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
}
