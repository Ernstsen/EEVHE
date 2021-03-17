package dk.mmj.eevhe.entities;

import java.util.Objects;

@SuppressWarnings("unused")
public class ComplaintResolveDTO {
    private int complaintSenderId;
    private int complaintResolverId;
    private PartialSecretMessageDTO value;

    public ComplaintResolveDTO() {
    }

    public ComplaintResolveDTO(int complaintSenderId, int complaintResolverId, PartialSecretMessageDTO value) {
        this.complaintSenderId = complaintSenderId;
        this.complaintResolverId = complaintResolverId;
        this.value = value;
    }

    public int getComplaintSenderId() {
        return complaintSenderId;
    }

    public void setComplaintSenderId(int complaintSenderId) {
        this.complaintSenderId = complaintSenderId;
    }

    public int getComplaintResolverId() {
        return complaintResolverId;
    }

    public void setComplaintResolverId(int complaintResolverId) {
        this.complaintResolverId = complaintResolverId;
    }

    public PartialSecretMessageDTO getValue() {
        return value;
    }

    public void setValue(PartialSecretMessageDTO value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ComplaintResolveDTO that = (ComplaintResolveDTO) o;
        return complaintSenderId == that.complaintSenderId && complaintResolverId == that.complaintResolverId && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(complaintSenderId, complaintResolverId, value);
    }

    @Override
    public String toString() {
        return "ComplaintResolveDTO{" +
                "complaintSenderId=" + complaintSenderId +
                ", complaintResolverId=" + complaintResolverId +
                ", value=" + value +
                '}';
    }
}
