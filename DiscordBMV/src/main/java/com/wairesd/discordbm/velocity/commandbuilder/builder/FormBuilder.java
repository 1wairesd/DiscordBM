package com.wairesd.discordbm.velocity.commandbuilder.builder;

import com.wairesd.discordbm.velocity.config.configurators.Forms;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;

public class FormBuilder {

    public Modal build(String modalId, Forms.FormStructured form) {
        Modal.Builder modalBuilder = Modal.create(modalId, form.title());
        for (Forms.FormStructured.Field field : form.fields()) {
            modalBuilder.addActionRow(createTextInput(field));
        }
        return modalBuilder.build();
    }

    private TextInput createTextInput(Forms.FormStructured.Field field) {
        return TextInput.create(
                        field.variable(),
                        field.label(),
                        TextInputStyle.valueOf(field.type().toUpperCase()))
                .setPlaceholder(field.placeholder())
                .setRequired(field.required())
                .build();
    }
}
