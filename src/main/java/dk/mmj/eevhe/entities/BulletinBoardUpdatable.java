package dk.mmj.eevhe.entities;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import dk.mmj.eevhe.server.bulletinboard.BulletinBoardState;

/**
 * Interface informing that an entity is able to update the state of a bulletin board peer
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "class")
public interface BulletinBoardUpdatable {

    void update(BulletinBoardState bb);

}
