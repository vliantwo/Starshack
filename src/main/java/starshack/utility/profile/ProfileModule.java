package starshack.utility.profile;

import starshack.Stars;
import starshack.clickgui.ClickGui;
import starshack.module.Module;
import starshack.module.impl.client.Settings;
import starshack.module.setting.impl.ButtonSetting;
import starshack.module.setting.impl.TextSetting;
import starshack.utility.Utils;

public class ProfileModule extends Module {
    private final Profile profile;
    private final TextSetting profileNameSetting;
    private String displayName;
    public boolean saved = true;

    public ProfileModule(Profile profile, String name, int bind) {
        super(name, category.configs, bind);
        this.profile = profile;
        this.displayName = name;
        this.registerSetting(profileNameSetting = new TextSetting("Profile name", name, "Type a new profile name...", 32, this::renameProfile));
        this.registerSetting(new ButtonSetting("Save profile", () -> {
            Utils.sendMessage("&7Saved profile: &b" + getName());
            Stars.profileManager.saveProfile(this.profile);
            saved = true;
        }));
        this.registerSetting(new ButtonSetting("Remove profile", () -> {
            String profileName = getName();
            if (Stars.profileManager.deleteProfile(profileName)) {
                Utils.sendMessage("&7Removed profile: &b" + profileName);
            }
        }));
    }

    @Override
    public void toggle() {
        if (mc.currentScreen instanceof ClickGui || mc.currentScreen == null) {
            Stars.profileManager.loadProfile(this.getName());

            Stars.currentProfile = profile;

            if (Settings.sendMessage.isToggled()) {
                Utils.sendMessage("&7Enabled profile: &b" + this.getName());
            }
            saved = true;
        }
    }

    @Override
    public boolean isEnabled() {
        if (Stars.currentProfile == null) {
            return false;
        }
        return Stars.currentProfile.getModule() == this;
    }

    @Override
    public String getName() {
        return displayName;
    }

    public void setProfileName(String profileName) {
        this.displayName = profileName;
        profileNameSetting.setText(profileName);
    }

    private void renameProfile() {
        if (Stars.profileManager == null) {
            return;
        }

        String oldName = getName();
        if (Stars.profileManager.renameProfile(profile, profileNameSetting.getText())) {
            profileNameSetting.setText(profile.getName());
            if (!oldName.equals(profile.getName())) {
                Utils.sendMessage("&7Renamed profile: &b" + oldName + " &7to &b" + profile.getName());
            }
        }
    }
}
