package dk.mmj.eevhe.entities;

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
}
