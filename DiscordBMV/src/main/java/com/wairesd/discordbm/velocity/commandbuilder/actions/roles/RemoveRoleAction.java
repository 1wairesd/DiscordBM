package com.wairesd.discordbm.velocity.commandbuilder.actions.roles;

import com.wairesd.discordbm.velocity.commandbuilder.models.actions.CommandAction;
import com.wairesd.discordbm.velocity.commandbuilder.models.contexts.Context;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class RemoveRoleAction implements CommandAction {
    private final String roleId;

    public RemoveRoleAction(Map<String, Object> properties) {
        this.roleId = (String) properties.get("role_id");
        if (this.roleId == null || this.roleId.isEmpty()) {
            throw new IllegalArgumentException("role_id is required for remove_role action");
        }
    }

    @Override
    public CompletableFuture<Void> execute(Context context) {
        return CompletableFuture.runAsync(() -> {
            var guild = context.getEvent().getGuild();
            var member = context.getEvent().getMember();
            var role = guild.getRoleById(roleId);
            if (role != null) {
                guild.removeRoleFromMember(member, role).queue();
            }
        });
    }
}