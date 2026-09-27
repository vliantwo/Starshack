package starshack.mixin.interfaces;

import net.minecraft.client.shader.ShaderGroup;

public interface ISaturationRenderer {
    ShaderGroup stars$getSaturationShader();

    void stars$setSaturationShader(ShaderGroup shader);
}
