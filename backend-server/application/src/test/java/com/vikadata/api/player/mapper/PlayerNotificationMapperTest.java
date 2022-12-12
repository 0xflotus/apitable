package com.vikadata.api.player.mapper;

import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.vikadata.api.AbstractMyBatisMapperTest;
import com.vikadata.api.player.dto.NotificationModelDTO;
import com.vikadata.api.player.mapper.PlayerNotificationMapper;
import com.vikadata.api.player.ro.NotificationPageRo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <p>
 * Player Notification Mapper Test
 * </p>
 */
@Disabled
public class PlayerNotificationMapperTest extends AbstractMyBatisMapperTest {

    @Autowired
    PlayerNotificationMapper playerNotificationMapper;

    @Test
    @Sql("/sql/player-notification-data.sql")
    void testSelectPlayerNotificationPage() {
        NotificationPageRo notificationPageRo = new NotificationPageRo();
        notificationPageRo.setIsRead(1);
        notificationPageRo.setNotifyType("member");
        notificationPageRo.setRowNo(1);
        List<NotificationModelDTO> entities = playerNotificationMapper.selectPlayerNotificationPage(notificationPageRo, 41L, 1);
        assertThat(entities).isNotEmpty();
    }

    @Test
    @Sql("/sql/player-notification-data.sql")
    void testSelectCountByUserIdAndIsRead() {
        Integer count = playerNotificationMapper.selectCountByUserIdAndIsRead(41L, 1);
        assertThat(count).isEqualTo(1);
    }

    @Test
    @Sql("/sql/player-notification-data.sql")
    void testSelectTotalCountByUserIds() {
        Integer count = playerNotificationMapper.selectTotalCountByUserId(41L);
        assertThat(count).isEqualTo(1);
    }

    @Test
    @Sql("/sql/player-notification-data.sql")
    void testSelectNotifyBodyById() {
        String body = playerNotificationMapper.selectNotifyBodyById(41L);
        assertThat(body).isEqualTo("{\"key\": \"value\"}");
    }

    @Test
    @Sql("/sql/player-notification-data.sql")
    void testSelectTotalCountByRoAndToUser() {
        NotificationPageRo notificationPageRo = new NotificationPageRo();
        notificationPageRo.setIsRead(1);
        notificationPageRo.setNotifyType("member");
        Integer count = playerNotificationMapper.selectTotalCountByRoAndToUser(notificationPageRo, 41L);
        assertThat(count).isEqualTo(1);
    }

    @Test
    @Sql("/sql/player-notification-data.sql")
    void testSelectDtoByTypeAndIsRead() {
        List<NotificationModelDTO> entities = playerNotificationMapper.selectDtoByTypeAndIsRead(41L, 1);
        assertThat(entities).isNotEmpty();
    }

}
