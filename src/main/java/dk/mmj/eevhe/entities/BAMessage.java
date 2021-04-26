package dk.mmj.eevhe.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dk.mmj.eevhe.protocols.agreement.mvba.Communicator;
import dk.mmj.eevhe.protocols.agreement.mvba.CompositeIncoming;
import dk.mmj.eevhe.protocols.agreement.mvba.SenderIdentityHaving;

import java.util.Objects;
import java.util.function.BiConsumer;

public class BAMessage implements SenderIdentityHaving {
    private String baId;
    private String msgString;
    private Boolean msgBoolean;
    private String senderId;

    public BAMessage(String baId, String msgString, Boolean msgBoolean, String senderId) {
        this.baId = baId;
        this.msgString = msgString;
        this.msgBoolean = msgBoolean;
        this.senderId = senderId;
    }

    public BAMessage() {
    }

    public String getBaId() {
        return baId;
    }

    public void setBaId(String baId) {
        this.baId = baId;
    }

    public String getMsgString() {
        return msgString;
    }

    public void setMsgString(String msgString) {
        this.msgString = msgString;
    }

    public Boolean getMsgBoolean() {
        return msgBoolean;
    }

    public void setMsgBoolean(Boolean msgBoolean) {
        this.msgBoolean = msgBoolean;
    }

    @Override
    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    @JsonIgnore
    public BiConsumer<Communicator, SignedEntity<BAMessage>> getCommunicatorConsumer() {
        if (msgString != null && !msgString.isEmpty()) {
            return (c, sba) -> {
                BAMessage ba = sba.getEntity();
                CompositeIncoming<Communicator.Message<String>> inc = new CompositeIncoming<>(
                        new Communicator.Message<>(
                                ba.baId,
                                ba.msgString),
                        ba.senderId, null);
                c.receiveString(
                        inc
                );
            };
        } else {
            return (c, sba) -> {
                BAMessage ba = sba.getEntity();
                CompositeIncoming<Communicator.Message<Boolean>> inc = new CompositeIncoming<>(
                        new Communicator.Message<>(
                                ba.baId,
                                ba.msgBoolean),
                        ba.senderId, null);
                c.receiveBool(
                        inc
                );
            };
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BAMessage baMessage = (BAMessage) o;
        return Objects.equals(baId, baMessage.baId) && Objects.equals(msgString, baMessage.msgString) && Objects.equals(msgBoolean, baMessage.msgBoolean) && Objects.equals(senderId, baMessage.senderId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(baId, msgString, msgBoolean, senderId);
    }

    @Override
    public String toString() {
        return "BAMessage{" +
                "baId='" + baId + '\'' +
                ", msgString='" + msgString + '\'' +
                ", msgBoolean=" + msgBoolean +
                ", senderId='" + senderId + '\'' +
                '}';
    }

}
