package dk.mmj.eevhe.entities;

import java.util.Objects;

public class DecryptionAuthorityInfo {
    private int id;
    private String address;

    public DecryptionAuthorityInfo(int id, String address) {
        this.id = id;
        this.address = address;
    }

    public DecryptionAuthorityInfo() {
    }

    public int getId() {
        return id;
    }

    public DecryptionAuthorityInfo setId(int id) {
        this.id = id;
        return this;
    }

    public String getAddress() {
        return address;
    }

    public DecryptionAuthorityInfo setAddress(String address) {
        this.address = address;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DecryptionAuthorityInfo that = (DecryptionAuthorityInfo) o;
        return id == that.id && Objects.equals(address, that.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, address);
    }

    @Override
    public String toString() {
        return "DecryptionAuthorityInfo{" +
                "id=" + id +
                ", address='" + address + '\'' +
                '}';
    }
}
