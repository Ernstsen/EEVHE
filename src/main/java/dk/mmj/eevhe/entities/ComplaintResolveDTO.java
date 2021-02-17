package dk.mmj.eevhe.entities;

import java.math.BigInteger;
import java.util.Objects;

@SuppressWarnings("unused")
public class ComplaintResolveDTO {
    private int id;
    private BigInteger value;

    public ComplaintResolveDTO(int id, BigInteger value) {
        this.id = id;
        this.value = value;
    }

    public ComplaintResolveDTO() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public BigInteger getValue() {
        return value;
    }

    public void setValue(BigInteger value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ComplaintResolveDTO that = (ComplaintResolveDTO) o;
        return id == that.id && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, value);
    }
}
