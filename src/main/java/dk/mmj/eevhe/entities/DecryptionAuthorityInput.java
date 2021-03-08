package dk.mmj.eevhe.entities;

import java.util.Date;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("unused")
public class DecryptionAuthorityInput {
    private String pHex;
    private String gHex;
    private String eHex;
    private long endTime;
    private List<DecryptionAuthorityInfo> infos;

    public DecryptionAuthorityInput(String pHex, String gHex, String eHex, long endTime, List<DecryptionAuthorityInfo> infos) {
        this.pHex = pHex;
        this.gHex = gHex;
        this.eHex = eHex;
        this.endTime = endTime;
        this.infos = infos;
    }

    public DecryptionAuthorityInput() {
    }

    public String getpHex() {
        return pHex;
    }

    public void setpHex(String pHex) {
        this.pHex = pHex;
    }

    public String getgHex() {
        return gHex;
    }

    public void setgHex(String gHex) {
        this.gHex = gHex;
    }

    public String geteHex() {
        return eHex;
    }

    public void seteHex(String eHex) {
        this.eHex = eHex;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public List<DecryptionAuthorityInfo> getInfos() {
        return infos;
    }

    public void setInfos(List<DecryptionAuthorityInfo> infos) {
        this.infos = infos;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DecryptionAuthorityInput that = (DecryptionAuthorityInput) o;
        return endTime == that.endTime && Objects.equals(pHex, that.pHex) && Objects.equals(gHex, that.gHex) && Objects.equals(eHex, that.eHex) && Objects.equals(infos, that.infos);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pHex, gHex, eHex, endTime, infos);
    }

    @Override
    public String toString() {
        return "DecryptionAuthorityInput{" +
                "pHex='" + pHex + '\'' +
                ", gHex='" + gHex + '\'' +
                ", eHex='" + eHex + '\'' +
                ", endTime=" + endTime +
                ", infos=" + infos +
                '}';
    }
}
