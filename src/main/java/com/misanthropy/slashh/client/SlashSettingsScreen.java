package com.misanthropy.slashh.client;

import com.misanthropy.slashh.SlashClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;

public class SlashSettingsScreen extends Screen {
    private static final int W = 150;
    private static final int H = 20;
    private static final int GAP = 4;

    private final Screen parent;

    public SlashSettingsScreen(Screen parent) {
        super(Component.translatable("slashh.settings.title"));
        this.parent = parent;
    }

    public static void register() {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new SlashSettingsScreen(parent)));
    }

    @Override
    protected void init() {
        int left = this.width / 2 - W - GAP / 2;
        int right = this.width / 2 + GAP / 2;
        int y = 40;
        addRenderableWidget(toggle(left, y, "show", SlashClientConfig.SHOW));
        addRenderableWidget(toggle(right, y, "others", SlashClientConfig.OTHERS));
        y += H + GAP;
        addRenderableWidget(new Slider(left, y, "size", SlashClientConfig.SIZE, 0.4, 2.5));
        addRenderableWidget(new Slider(right, y, "opacity", SlashClientConfig.OPACITY, 0.1, 1.0));
        y += H + GAP;
        addRenderableWidget(toggle(left, y, "colours", SlashClientConfig.COLOURS));
        addRenderableWidget(toggle(right, y, "finishers", SlashClientConfig.FINISHERS));
        y += H + GAP;
        addRenderableWidget(toggle(left, y, "sweeps", SlashClientConfig.SWEEPS));
        addRenderableWidget(toggle(right, y, "spray", SlashClientConfig.SPRAY));
        y += H + GAP;
        addRenderableWidget(new Slider(left, y, "shake", SlashClientConfig.SHAKE, 0.0, 2.0));
        addRenderableWidget(toggle(right, y, "classic", SlashClientConfig.CLASSIC));
        y += H + GAP * 4;
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose()).bounds(this.width / 2 - 100, y, 200, H).build());
    }

    private static CycleButton<Boolean> toggle(int x, int y, String key, ForgeConfigSpec.BooleanValue value) {
        return CycleButton.onOffBuilder(value.get())
                .withTooltip(v -> Tooltip.create(Component.translatable("slashh.settings." + key + ".tip")))
                .create(x, y, W, H, Component.translatable("slashh.settings." + key), (button, v) -> {
                    value.set(v);
                    SlashClientConfig.bake();
                });
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 16, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        SlashClientConfig.SPEC.save();
        SlashClientConfig.bake();
        if (this.minecraft != null) this.minecraft.setScreen(this.parent);
    }

    private static final class Slider extends AbstractSliderButton {
        private final String key;
        private final ForgeConfigSpec.DoubleValue config;
        private final double min;
        private final double max;

        Slider(int x, int y, String key, ForgeConfigSpec.DoubleValue config, double min, double max) {
            super(x, y, W, H, Component.empty(), (config.get() - min) / (max - min));
            this.key = key;
            this.config = config;
            this.min = min;
            this.max = max;
            updateMessage();
        }

        private double current() {
            return Math.round((this.min + this.value * (this.max - this.min)) * 20.0) / 20.0;
        }

        @Override
        protected void updateMessage() {
            double v = current();
            Component shown = v <= 0.0 ? CommonComponents.OPTION_OFF : Component.literal(Math.round(v * 100) + "%");
            setMessage(Component.translatable("slashh.settings." + this.key).append(": ").append(shown));
        }

        @Override
        protected void applyValue() {
            this.config.set(current());
            SlashClientConfig.bake();
        }
    }
}
