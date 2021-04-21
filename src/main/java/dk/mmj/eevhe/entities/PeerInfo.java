package dk.mmj.eevhe.entities;

import java.util.Objects;

public class PeerInfo {
    private int id;
    private String address;

    public PeerInfo(int id, String address) {
        this.id = id;
        this.address = address;
    }

    public PeerInfo() {
    }

    public int getId() {
        return id;
    }

    public PeerInfo setId(int id) {
        this.id = id;
        return this;
    }

    public String getAddress() {
        return address;
    }

    public PeerInfo setAddress(String address) {
        this.address = address;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PeerInfo that = (PeerInfo) o;
        return id == that.id && Objects.equals(address, that.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, address);
    }

    @Override
    public String toString() {
        return "PeerInfo{" +
                "id=" + id +
                ", address='" + address + '\'' +
                '}';
    }
}
