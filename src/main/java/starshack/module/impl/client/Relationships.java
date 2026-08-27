package starshack.module.impl.client;

import starshack.Stars;
import starshack.module.Module;
import starshack.module.setting.impl.ButtonSetting;
import starshack.module.setting.impl.GroupSetting;
import starshack.module.setting.impl.PlayerListSetting;
import starshack.utility.PlayerRelationsManager;

public class Relationships extends Module {
    public final GroupSetting friendsGroup = new GroupSetting("Friends");
    public final GroupSetting enemiesGroup = new GroupSetting("Enemies");
    public final GroupSetting middleClickGroup = new GroupSetting("Middle click");

    public final PlayerListSetting friends = new PlayerListSetting(friendsGroup, "Players", PlayerRelationsManager.RelationType.FRIEND, "Type a username", 32);
    public final PlayerListSetting enemies = new PlayerListSetting(enemiesGroup, "Players", PlayerRelationsManager.RelationType.ENEMY, "Type a username", 32);
    public final ButtonSetting middleClickFriends = new ButtonSetting(middleClickGroup, "Middle click friends",
            Stars.playerRelationsManager != null && Stars.playerRelationsManager.isMiddleClickFriends());

    public Relationships() {
        super("Relationships", category.configs, 0);

        friendsGroup.setOpened(true);
        enemiesGroup.setOpened(true);
        middleClickGroup.setOpened(true);

        registerSetting(friendsGroup);
        registerSetting(friends);
        registerSetting(enemiesGroup);
        registerSetting(enemies);
        registerSetting(middleClickGroup);
        registerSetting(middleClickFriends);

        this.ignoreOnSave = true;
        this.hidden = true;
    }

    @Override
    public void guiButtonToggled(ButtonSetting buttonSetting) {
        if (buttonSetting == middleClickFriends && Stars.playerRelationsManager != null) {
            Stars.playerRelationsManager.setMiddleClickFriends(buttonSetting.isToggled());
        }
    }

    @Override
    public void onEnable() {
        if (Stars.playerRelationsManager != null) {
            Stars.playerRelationsManager.setActive(true);
        }
    }

    @Override
    public void onDisable() {
        if (Stars.playerRelationsManager != null) {
            Stars.playerRelationsManager.setActive(false);
        }
    }
}
