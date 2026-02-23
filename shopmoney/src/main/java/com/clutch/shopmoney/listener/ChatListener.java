package com.clutch.shopmoney.listener;

import com.clutch.shopmoney.service.ChatInputService;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ChatListener implements Listener {
    private final ChatInputService chatInputService;

    public ChatListener(ChatInputService chatInputService) {
        this.chatInputService = chatInputService;
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        if (chatInputService.consume(event.getPlayer().getUniqueId(), net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.message()))) {
            event.setCancelled(true);
        }
    }
}
