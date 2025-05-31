package com.wairesd.discordbm.velocity.commandbuilder.actions.components;

import com.wairesd.discordbm.velocity.commandbuilder.models.actions.CommandAction;
import com.wairesd.discordbm.velocity.commandbuilder.models.contexts.Context;
import com.wairesd.discordbm.velocity.commandbuilder.components.ButtonEditor;
import com.wairesd.discordbm.velocity.commandbuilder.message.MessageComponentFetcher;
import com.wairesd.discordbm.velocity.commandbuilder.message.MessageUpdater;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class EditComponentAction implements CommandAction {
    private final String targetMessageLabel;
    private final String componentId;
    private final String newLabel;
    private final String newStyle;
    private final Boolean disabled;

    public EditComponentAction(Map<String, Object> props) {
        this.targetMessageLabel = (String) props.get("target_message");
        this.componentId = (String) props.get("component_id");
        this.newLabel = (String) props.get("label");
        this.newStyle = (String) props.get("style");
        this.disabled = (Boolean) props.get("disabled");

        if (componentId == null || componentId.isEmpty()) {
            throw new IllegalArgumentException("component_id is required for EditComponentAction");
        }
    }

    @Override
    public CompletableFuture<Void> execute(Context context) {
        return CompletableFuture.runAsync(() -> {
            String messageId = targetMessageLabel != null
                    ? context.getMessageIdByLabel(targetMessageLabel)
                    : context.getMessageIdToEdit();

            if (!(context.getEvent().getChannel() instanceof TextChannel textChannel)) return;

            new MessageComponentFetcher(textChannel, messageId)
                    .fetchAndApply(rows -> {
                        context.setActionRows(rows);
                        new ButtonEditor(componentId, newLabel, newStyle, disabled)
                                .edit(context.getActionRows());
                        new MessageUpdater(textChannel, messageId, context.getActionRows()).update();
                    });
        });
    }
}
