package com.clutch.shopmoney.listener;

import com.clutch.shopmoney.service.ChatInputService;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ChatListener implements Listener {
    private final ChatInputService chatInputService;

    public ChatListener(ChatInputService chatInputService) {
        this.chatInputService = chatInputService;
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        if (chatInputService.handleChat(event.getPlayer(), message)) {
            event.setCancelled(true);
        }
    }
}
