package com.wairesd.discordbm.velocity.commandbuilder.builder;

import com.wairesd.discordbm.velocity.config.configurators.Forms;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;

import java.util.UUID;

public class FormModalBuilder {
    public Modal buildModal(Forms.FormStructured form) {
        String modalID = "form_" + UUID.randomUUID();
        Modal.Builder modalBuilder = Modal.create(modalID, form.title());

        for (Forms.FormStructured.Field field : form.fields()) {
            TextInput input = TextInput.create(
                            field.variable(),
                            field.label(),
                            TextInputStyle.valueOf(field.type().toUpperCase()))
                    .setPlaceholder(field.placeholder())
                    .setRequired(field.required())
                    .build();
            modalBuilder.addActionRow(input);
        }

        return modalBuilder.build();
    }
}
