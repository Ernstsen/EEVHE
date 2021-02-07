package dk.mmj.eevhe.entities;

import java.util.List;
import java.util.Objects;

@SuppressWarnings("JavaDocs, unused")
public class ResultList {

    private List<PartialResultList> results;

    public ResultList() {
    }

    public ResultList(List<PartialResultList> results) {
        this.results = results;
    }

    public List<PartialResultList> getResults() {
        return results;
    }

    public void setResults(List<PartialResultList> results) {
        this.results = results;
    }

    @Override
    public String toString() {
        return "ResultList{" +
                "results=" + results +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResultList that = (ResultList) o;
        return Objects.equals(results, that.results);
    }

    @Override
    public int hashCode() {
        return Objects.hash(results);
    }
}
