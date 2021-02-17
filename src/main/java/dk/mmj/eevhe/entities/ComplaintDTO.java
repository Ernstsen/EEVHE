package dk.mmj.eevhe.entities;

import java.util.Objects;

@SuppressWarnings("unused")
public class ComplaintDTO {
    int id;

    public ComplaintDTO() {
    }

    public ComplaintDTO(int id) {
        this.id = id;
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
        ComplaintDTO that = (ComplaintDTO) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
