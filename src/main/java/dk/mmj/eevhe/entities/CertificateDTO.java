package dk.mmj.eevhe.entities;

import java.util.Objects;

/**
 * Simple DTO representing a certificate used in the election
 */
@SuppressWarnings("unused")
public class CertificateDTO {
    private String cert;
    private int id;

    CertificateDTO() {
    }

    public CertificateDTO(String cert, int id) {
        this.cert = cert;
        this.id = id;
    }

    public String getCert() {
        return cert;
    }

    public void setCert(String cert) {
        this.cert = cert;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CertificateDTO that = (CertificateDTO) o;
        return id == that.id && Objects.equals(cert, that.cert);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cert, id);
    }

    @Override
    public String toString() {
        return "CertificateDTO{" +
                "cert='" + cert + '\'' +
                ", id=" + id +
                '}';
    }
}
