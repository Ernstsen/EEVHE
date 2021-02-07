package dk.mmj.eevhe.entities;

import java.util.List;
import java.util.Objects;

/**
 * Simple entity for sending list of public information entities
 */
@SuppressWarnings("unused, JavaDocs")
public class PublicInfoList {
    private List<PublicInformationEntity> informationEntities;

    public PublicInfoList() {
    }

    public PublicInfoList(List<PublicInformationEntity> informationEntities) {
        this.informationEntities = informationEntities;
    }

    public List<PublicInformationEntity> getInformationEntities() {
        return informationEntities;
    }

    public void setInformationEntities(List<PublicInformationEntity> informationEntities) {
        this.informationEntities = informationEntities;
    }

    @Override
    public String toString() {
        return "PublicInfoList{" +
                "informationEntities=" + informationEntities +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PublicInfoList that = (PublicInfoList) o;
        return Objects.equals(informationEntities, that.informationEntities);
    }

    @Override
    public int hashCode() {
        return Objects.hash(informationEntities);
    }
}
