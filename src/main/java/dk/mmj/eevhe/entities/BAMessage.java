package dk.mmj.eevhe.entities;

import dk.mmj.eevhe.protocols.agreement.mvba.Communicator;
import dk.mmj.eevhe.protocols.agreement.mvba.IncomingImpl;

import java.util.Objects;

public class BAMessage {
    private String baId;
    private String msgString;
    private Boolean msgBoolean;

    public BAMessage(String baId, String msgString, Boolean msgBoolean) {
        this.baId = baId;
        this.msgString = msgString;
        this.msgBoolean = msgBoolean;
    }

    public BAMessage() {}

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

    public void communicatorReceive(Communicator communicator) {
        if (msgString != null && !msgString.isEmpty()) {
//            communicator.receiveString(baId, msgString); //TODO!
        } else {
//          communicator.receive(baId, msgBoolean); //TODO!
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BAMessage that = (BAMessage) o;
        return Objects.equals(baId, that.baId) && Objects.equals(msgString, that.msgString) && Objects.equals(msgBoolean, that.msgBoolean);
    }

    @Override
    public int hashCode() {
        return Objects.hash(baId, msgString, msgBoolean);
    }

    @Override
    public String toString() {
        return "BrachaMessage{" +
                "baId='" + baId + '\'' +
                ", msgString='" + msgString + '\'' +
                ", msgBoolean=" + msgBoolean +
                '}';
    }
}
