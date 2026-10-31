package com.baraaoi.reallife.util;

import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.Heightmap;

/** В 26.x Level.getHeight возвращает minY для незагруженного чанка — сначала загружаем чанк. */
public final class Ground {
    private Ground() {}

    public static int height(LevelReader w, Heightmap.Types type, int x, int z) {
        w.getChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z));
        return w.getHeight(type, x, z);
    }
}
