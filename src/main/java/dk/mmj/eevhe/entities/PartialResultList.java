package dk.mmj.eevhe.entities;

import java.util.List;

public class PartialResultList {
    private List<PartialResult> results;

    public PartialResultList(List<PartialResult> results) {
        this.results = results;
    }

    public PartialResultList() {
    }

    public List<PartialResult> getResults() {
        return results;
    }

    public PartialResultList setResults(List<PartialResult> results) {
        this.results = results;
        return this;
    }
}
