package starshack.module.setting.impl;

import com.google.gson.JsonObject;
import starshack.Stars;
import starshack.module.setting.Setting;
import starshack.utility.PlayerRelationsManager;

import java.util.Collections;
import java.util.List;

public class PlayerListSetting extends Setting {
    private final PlayerRelationsManager.RelationType relationType;
    private final String placeholder;
    private final int maxLength;
    public GroupSetting group;

    public PlayerListSetting(GroupSetting group, String name, PlayerRelationsManager.RelationType relationType, String placeholder, int maxLength) {
        super(name);
        this.group = group;
        this.relationType = relationType;
        this.placeholder = placeholder == null ? "" : placeholder;
        this.maxLength = Math.max(1, maxLength);
    }

    public PlayerRelationsManager.RelationType getRelationType() {
        return relationType;
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public boolean addPlayer(String name) {
        return Stars.playerRelationsManager != null && Stars.playerRelationsManager.addRelation(relationType, name);
    }

    public boolean removePlayer(String name) {
        return Stars.playerRelationsManager != null && Stars.playerRelationsManager.removeRelation(relationType, name);
    }

    public void clearPlayers() {
        if (Stars.playerRelationsManager != null) {
            Stars.playerRelationsManager.clearRelation(relationType);
        }
    }

    public List<PlayerRelationsManager.PlayerEntry> getEntries() {
        if (Stars.playerRelationsManager == null) {
            return Collections.emptyList();
        }
        return Stars.playerRelationsManager.getEntries(relationType);
    }

    @Override
    public String getProfileKey() {
        return group == null ? getName() : group.getName() + "." + getName();
    }

    @Override
    public void loadProfile(JsonObject data) {
    }
}
