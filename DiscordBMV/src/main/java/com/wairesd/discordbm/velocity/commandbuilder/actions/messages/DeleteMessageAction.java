package com.wairesd.discordbm.velocity.commandbuilder.actions.messages;

import com.wairesd.discordbm.velocity.commandbuilder.channel.ChannelFetcher;
import com.wairesd.discordbm.velocity.commandbuilder.message.MessageDeleter;
import com.wairesd.discordbm.velocity.commandbuilder.message.MessageReferenceResolver;
import com.wairesd.discordbm.velocity.commandbuilder.models.actions.CommandAction;
import com.wairesd.discordbm.velocity.commandbuilder.models.contexts.Context;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class DeleteMessageAction implements CommandAction {
    private final String label;
    private final String channelId;
    private final String messageId;

    private final MessageReferenceResolver resolver = new MessageReferenceResolver();
    private final ChannelFetcher fetcher = new ChannelFetcher();
    private final MessageDeleter deleter = new MessageDeleter();

    public DeleteMessageAction(Map<String, Object> properties) {
        this.label = (String) properties.get("label");
        this.channelId = (String) properties.get("channel_id");
        this.messageId = (String) properties.get("message_id");
    }

    @Override
    public CompletableFuture<Void> execute(Context context) {
        return CompletableFuture.runAsync(() -> {
            MessageReferenceResolver.MessageReference ref = resolver.resolve(label, channelId, messageId, context);
            TextChannel channel = fetcher.getTextChannel(context.getEvent().getJDA(), ref.channelId());
            deleter.deleteMessage(channel, ref.messageId());
        });
    }
}
