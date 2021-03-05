package dk.mmj.eevhe.interfaces;

import dk.mmj.eevhe.entities.PartialPublicInfo;

import java.util.List;

/**
 * Functional Interface for fetching {@link dk.mmj.eevhe.entities.PartialPublicInfo}s
 */
public interface PublicInfoFetcher {

    /**
     * Fetches and validates PartialPublicInfo
     *
     * @return list of PartialPublicInfos - all valid
     */
    List<PartialPublicInfo> fetch();

}
