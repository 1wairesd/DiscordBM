package com.wairesd.discordbm.velocity.commands.commandbuilder.repository;

import com.wairesd.discordbm.velocity.config.configurators.Forms;

public class FormRepository {
    public Forms.FormStructured getForm(String formName) {
        Forms.FormStructured form = Forms.getForms().get(formName);
        if (form == null) {
            throw new IllegalArgumentException("Form not found: " + formName);
        }
        return form;
    }
}
