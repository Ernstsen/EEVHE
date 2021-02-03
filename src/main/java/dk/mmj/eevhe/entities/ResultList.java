package dk.mmj.eevhe.entities;

import java.util.List;

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
}
