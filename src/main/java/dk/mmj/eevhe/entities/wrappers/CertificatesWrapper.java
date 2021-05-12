package dk.mmj.eevhe.entities.wrappers;

import dk.mmj.eevhe.entities.CertificateDTO;
import dk.mmj.eevhe.entities.SignedEntity;

import java.util.List;
import java.util.Objects;

public class CertificatesWrapper implements Wrapper<List<SignedEntity<CertificateDTO>>> {
    private List<SignedEntity<CertificateDTO>> content;

    @SuppressWarnings("unused")
    public CertificatesWrapper() {
    }

    public CertificatesWrapper(List<SignedEntity<CertificateDTO>> content) {
        this.content = content;
    }

    public List<SignedEntity<CertificateDTO>> getContent() {
        return content;
    }

    public void setContent(List<SignedEntity<CertificateDTO>> content) {
        this.content = content;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CertificatesWrapper that = (CertificatesWrapper) o;
        return Objects.equals(content, that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(content);
    }

    @Override
    public String toString() {
        return "CertificatesWrapper{" +
                "content=" + content +
                '}';
    }
}
