package com.wairesd.discordbm.velocity.commandbuilder.components;

import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;

import java.util.ArrayList;
import java.util.List;

public class ButtonEditor {
    private final String componentId;
    private final String newLabel;
    private final String newStyle;
    private final Boolean disabled;

    public ButtonEditor(String componentId, String newLabel, String newStyle, Boolean disabled) {
        this.componentId = componentId;
        this.newLabel = newLabel;
        this.newStyle = newStyle;
        this.disabled = disabled;
    }

    public void edit(List<ActionRow> actionRows) {
        for (int i = 0; i < actionRows.size(); i++) {
            ActionRow row = actionRows.get(i);
            List<ItemComponent> components = row.getComponents();

            for (int j = 0; j < components.size(); j++) {
                ItemComponent component = components.get(j);
                if (component instanceof Button button && button.getId() != null && button.getId().equals(componentId)) {
                    ButtonStyle style = newStyle != null ? ButtonStyle.valueOf(newStyle.toUpperCase()) : button.getStyle();
                    String label = newLabel != null ? newLabel : button.getLabel();
                    boolean isDisabled = disabled != null ? disabled : button.isDisabled();

                    Button newButton = button.withStyle(style).withLabel(label).withDisabled(isDisabled);
                    List<ItemComponent> newComponents = new ArrayList<>(components);
                    newComponents.set(j, newButton);

                    actionRows.set(i, ActionRow.of(newComponents));
                    return;
                }
            }
        }
    }
}
