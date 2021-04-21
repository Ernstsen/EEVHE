package dk.mmj.eevhe.entities;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import dk.mmj.eevhe.server.bulletinboard.BulletinBoardState;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "class")
public interface BulletinBoardUpdatable {

    void update(BulletinBoardState bb);

}
