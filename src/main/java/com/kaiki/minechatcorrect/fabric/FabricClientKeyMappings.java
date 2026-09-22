// Disabled: only chat underlines and word commands are enabled.
// package com.kaiki.minechatcorrect.fabric;
//
// import com.kaiki.minechatcorrect.client.MineChatCorrectSettingsScreen;
// import com.mojang.blaze3d.platform.InputConstants;
// import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
// import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
// import net.minecraft.client.KeyMapping;
// import net.minecraft.resources.Identifier;
// import org.lwjgl.glfw.GLFW;
//
// /** Registers and handles Fabric client keybindings. */
// public final class FabricClientKeyMappings {
//     private static final KeyMapping OPEN_SETTINGS = new KeyMapping(
//             "key.mine_chatcorrect.open_settings",
//             InputConstants.Type.KEYSYM,
//             GLFW.GLFW_KEY_UNKNOWN,
//             KeyMapping.Category.register(Identifier.fromNamespaceAndPath("mine_chatcorrect", "main"))
//     );
//
//     private FabricClientKeyMappings() {
//     }
//
//     public static void register() {
//         KeyMappingHelper.registerKeyMapping(OPEN_SETTINGS);
//         ClientTickEvents.END_CLIENT_TICK.register(client -> {
//             while (OPEN_SETTINGS.consumeClick()) {
//                 client.gui.setScreen(new MineChatCorrectSettingsScreen(client.gui.screen()));
//             }
//         });
//     }
// }
