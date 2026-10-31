package com.baraaoi.reallife.util;

import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.UUID;

/** Мелкие помощники для NBT (в 26.x пропали putUniqueId/getUniqueId и т.п.). */
public final class Nbt {
    private Nbt() {
    }

    public static void putUUID(CompoundTag t, String key, UUID id) {
        t.store(key, UUIDUtil.CODEC, id);
    }

    public static boolean hasUUID(CompoundTag t, String key) {
        return t.read(key, UUIDUtil.CODEC).isPresent();
    }

    public static UUID getUUID(CompoundTag t, String key) {
        return t.read(key, UUIDUtil.CODEC).orElse(new UUID(0L, 0L));
    }

    public static BlockState readBlockState(CompoundTag t) {
        return NbtUtils.readBlockState(BuiltInRegistries.BLOCK, t);
    }

    public static void putUUID(ValueOutput t, String key, UUID id) {
        t.store(key, UUIDUtil.CODEC, id);
    }

    public static boolean hasUUID(ValueInput t, String key) {
        return t.read(key, UUIDUtil.CODEC).isPresent();
    }

    public static UUID getUUID(ValueInput t, String key) {
        return t.read(key, UUIDUtil.CODEC).orElse(new UUID(0L, 0L));
    }

    /** Есть ли ключ (аналог CompoundTag.contains для ValueInput). */
    public static boolean has(ValueInput t, String key) {
        return t.read(key, com.mojang.serialization.Codec.PASSTHROUGH).isPresent();
    }

    /** Вложенный CompoundTag в ValueInput/ValueOutput. */
    public static void putTag(ValueOutput t, String key, CompoundTag tag) {
        t.store(key, CompoundTag.CODEC, tag);
    }

    public static CompoundTag getTag(ValueInput t, String key) {
        return t.read(key, CompoundTag.CODEC).orElseGet(CompoundTag::new);
    }

    // ------------------------------------------------------------------ данные предметов (CUSTOM_DATA)

    /** Копия пользовательских данных предмета или null, если их нет. */
    public static CompoundTag itemTag(net.minecraft.world.item.ItemStack st) {
        net.minecraft.world.item.component.CustomData d = st.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        return d == null || d.isEmpty() ? null : d.copyTag();
    }

    /** Изменить пользовательские данные предмета. */
    public static void editItem(net.minecraft.world.item.ItemStack st, java.util.function.Consumer<CompoundTag> fn) {
        net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, st, fn);
    }

    public static void setItemTag(net.minecraft.world.item.ItemStack st, CompoundTag t) {
        st.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(t));
    }

    // ------------------------------------------------------------------ блочные сущности

    public static CompoundTag saveBE(net.minecraft.world.level.block.entity.BlockEntity te) {
        return te.saveWithoutMetadata(te.getLevel().registryAccess());
    }

    public static void loadBE(net.minecraft.world.level.block.entity.BlockEntity te, CompoundTag t) {
        te.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(
                net.minecraft.util.ProblemReporter.DISCARDING, te.getLevel().registryAccess(), t));
    }
}
