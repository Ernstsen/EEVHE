package dk.mmj.eevhe.entities;

import java.util.Objects;

public class BBPeerInfo extends PeerInfo {
    private String certificate;

    public BBPeerInfo(int id, String address, String certificate) {
        super(id, address);
        this.certificate = certificate;
    }

    public BBPeerInfo() {}

    public String getCertificate() {
        return certificate;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        BBPeerInfo that = (BBPeerInfo) o;
        return Objects.equals(certificate, that.certificate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), certificate);
    }

    @Override
    public String toString() {
        return "BBPeerInfo{" +
                "id=" + getId() +
                ", address='" + getAddress() + '\'' +
                ", certificate='" + certificate + '\'' +
                '}';
    }
}
