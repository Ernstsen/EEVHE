package dk.mmj.eevhe.entities;

import java.util.List;
import java.util.Objects;

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

    @Override
    public String toString() {
        return "PartialResultList{" +
                "results=" + results +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PartialResultList that = (PartialResultList) o;
        return Objects.equals(results, that.results);
    }

    @Override
    public int hashCode() {
        return Objects.hash(results);
    }
}
