package com.wairesd.discordbm.host.common.commandbuilder.interaction.roles;

import com.wairesd.discordbm.host.common.commandbuilder.core.models.actions.CommandAction;
import com.wairesd.discordbm.host.common.commandbuilder.core.models.context.Context;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class AddRoleAction implements CommandAction {
    private final String roleId;

    public AddRoleAction(Map<String, Object> properties) {
        this.roleId = (String) properties.get("role_id");
        if (this.roleId == null || this.roleId.isEmpty()) {
            throw new IllegalArgumentException("role_id is required for add_role action");
        }
    }

    @Override
    public CompletableFuture<Void> execute(Context context) {
        return CompletableFuture.runAsync(() -> {
            var guild = context.getEvent().getGuild();
            var member = context.getEvent().getMember();
            var role = guild.getRoleById(roleId);
            if (role != null) {
                guild.addRoleToMember(member, role).queue();
            }
        });
    }
}