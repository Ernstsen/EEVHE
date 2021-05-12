package dk.mmj.eevhe.entities;

import java.util.List;
import java.util.Objects;

public class BBInput {
    private List<BBPeerInfo> peers;
    private List<PeerInfo> edges;

    public BBInput(List<BBPeerInfo> peers, List<PeerInfo> edges) {
        this.peers = peers;
        this.edges = edges;
    }

    public BBInput(){}

    public List<BBPeerInfo> getPeers() {
        return peers;
    }

    public void setPeers(List<BBPeerInfo> peers) {
        this.peers = peers;
    }

    public List<PeerInfo> getEdges() {
        return edges;
    }

    public void setEdges(List<PeerInfo> edges) {
        this.edges = edges;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BBInput that = (BBInput) o;
        return Objects.equals(peers, that.peers) && Objects.equals(edges, that.edges);
    }

    @Override
    public int hashCode() {
        return Objects.hash(peers, edges);
    }

    @Override
    public String toString() {
        return "BBInput{" +
                "peers='" + peers + '\'' +
                ", edges='" + edges + '\'' +
                '}';
    }
}
