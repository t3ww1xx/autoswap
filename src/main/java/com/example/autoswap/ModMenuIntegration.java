package com.example.autoswap;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Кнопка настроек в Mod Menu. Мод работает и без Mod Menu (он нужен только для сборки). */
public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return AutoSwapScreen::new;
    }
}
