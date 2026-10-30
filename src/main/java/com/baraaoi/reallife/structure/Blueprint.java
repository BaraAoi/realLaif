package com.baraaoi.reallife.structure;

import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.WallSignBlock;
import net.minecraft.state.properties.BedPart;
import net.minecraft.state.properties.DoubleBlockHalf;
import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.SignTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Помощник для программной стройки с поворотом.
 *
 * Локальные координаты: x — влево (если смотреть от игрока), y — вверх, z — вперёд (от игрока).
 * Блоки с направлением задаются так, как если бы игрок смотрел на юг (SOUTH),
 * т.е. «к игроку» = NORTH, «от игрока» = SOUTH, «влево» = EAST, «вправо» = WEST.
 */
public class Blueprint {
    public static final int FLAGS = 2 | 16;

    private final World world;
    private final BlockPos origin;
    private final Direction forward;
    private final Direction left;
    private final Rotation rotation;
    private int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
    private int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
    /** Снимок исходных блоков (для временных построек), null — не записывать. */
    private ListNBT snapshot;
    private final Set<Long> seen = new HashSet<>();

    public Blueprint(World world, BlockPos origin, Direction forward) {
        this.world = world;
        this.origin = origin;
        this.forward = forward.getAxis().isHorizontal() ? forward : Direction.SOUTH;
        this.left = this.forward.rotateYCCW();
        switch (this.forward) {
            case WEST: rotation = Rotation.CLOCKWISE_90; break;
            case NORTH: rotation = Rotation.CLOCKWISE_180; break;
            case EAST: rotation = Rotation.COUNTERCLOCKWISE_90; break;
            default: rotation = Rotation.NONE;
        }
    }

    /** Включить запись исходных блоков перед перезаписью. */
    public Blueprint recordSnapshot() {
        this.snapshot = new ListNBT();
        return this;
    }

    public ListNBT snapshot() {
        return snapshot == null ? new ListNBT() : snapshot;
    }

    public World world() {
        return world;
    }

    private void remember(BlockPos p) {
        if (snapshot == null || !seen.add(p.toLong())) return;
        CompoundNBT t = new CompoundNBT();
        t.putLong("p", p.toLong());
        t.put("s", NBTUtil.writeBlockState(world.getBlockState(p)));
        TileEntity te = world.getTileEntity(p);
        if (te != null) {
            t.put("t", te.write(new CompoundNBT()));
        }
        snapshot.add(t);
    }

    /** Вернуть всё, как было до постройки. */
    public static void restore(World world, ListNBT snap) {
        List<CompoundNBT> list = new ArrayList<>();
        for (INBT n : snap) list.add((CompoundNBT) n);
        // сначала убрать временную постройку (без выпадения вещей)
        for (CompoundNBT t : list) {
            BlockPos p = BlockPos.fromLong(t.getLong("p"));
            TileEntity te = world.getTileEntity(p);
            if (te instanceof IInventory) ((IInventory) te).clear();
            world.setBlockState(p, Blocks.AIR.getDefaultState(), FLAGS);
        }
        for (CompoundNBT t : list) {
            BlockPos p = BlockPos.fromLong(t.getLong("p"));
            BlockState st = NBTUtil.readBlockState(t.getCompound("s"));
            world.setBlockState(p, st, FLAGS);
            if (t.contains("t")) {
                TileEntity te = world.getTileEntity(p);
                if (te != null) {
                    te.read(st, t.getCompound("t"));
                    te.markDirty();
                }
            }
        }
    }

    public BlockPos pos(int x, int y, int z) {
        return origin.offset(left, x).offset(forward, z).up(y);
    }

    public Direction dir(Direction local) {
        return rotation.rotate(local);
    }

    public void set(int x, int y, int z, BlockState state) {
        BlockPos p = pos(x, y, z);
        remember(p);
        world.setBlockState(p, state.rotate(rotation), FLAGS);
        track(p);
    }

    private void track(BlockPos p) {
        minX = Math.min(minX, p.getX()); minY = Math.min(minY, p.getY()); minZ = Math.min(minZ, p.getZ());
        maxX = Math.max(maxX, p.getX()); maxY = Math.max(maxY, p.getY()); maxZ = Math.max(maxZ, p.getZ());
    }

    public void fill(int x1, int y1, int z1, int x2, int y2, int z2, BlockState state) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++)
                    set(x, y, z, state);
    }

    /** Полая коробка: только стены (без пола и потолка). */
    public void walls(int x1, int y1, int z1, int x2, int y2, int z2, BlockState state) {
        for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
            for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
                set(x, y, z1, state);
                set(x, y, z2, state);
            }
            for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                set(x1, y, z, state);
                set(x2, y, z, state);
            }
        }
    }

    public void clear(int x1, int y1, int z1, int x2, int y2, int z2) {
        fill(x1, y1, z1, x2, y2, z2, Blocks.AIR.getDefaultState());
    }

    /** Фундамент: заполняет пустоты под постройкой до земли (до 12 блоков вниз). */
    public void foundation(int x1, int z1, int x2, int z2, BlockState state) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
            for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                set(x, -1, z, state);
                for (int y = -2; y >= -12; y--) {
                    BlockPos p = pos(x, y, z);
                    BlockState s = world.getBlockState(p);
                    if (!s.isAir(world, p) && !s.getMaterial().isLiquid() && !s.getMaterial().isReplaceable()) break;
                    world.setBlockState(p, state, FLAGS);
                }
            }
    }

    /** Дверь (две половинки). facing — куда смотрит дверь в локальной системе. */
    public void door(int x, int y, int z, BlockState door, Direction facing) {
        BlockState base = door.with(DoorBlock.FACING, facing);
        set(x, y + 1, z, base.with(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        set(x, y, z, base.with(DoorBlock.HALF, DoubleBlockHalf.LOWER));
    }

    /** Кровать. facing — направление от ног к голове (локально). */
    public void bed(int x, int y, int z, BlockState bed, Direction facing) {
        BlockState base = bed.with(BedBlock.HORIZONTAL_FACING, facing);
        set(x, y, z, base.with(BedBlock.PART, BedPart.FOOT));
        int hx = x + offX(facing), hz = z + offZ(facing);
        set(hx, y, hz, base.with(BedBlock.PART, BedPart.HEAD));
    }

    public BlockState stairs(BlockState stairs, Direction facing) {
        return stairs.with(StairsBlock.FACING, facing);
    }

    /** Табличка на стене. facing — куда смотрит лицевая сторона (локально). */
    public void sign(int x, int y, int z, BlockState wallSign, Direction facing, String... lines) {
        set(x, y, z, wallSign.with(WallSignBlock.FACING, facing));
        TileEntity te = world.getTileEntity(pos(x, y, z));
        if (te instanceof SignTileEntity) {
            SignTileEntity sign = (SignTileEntity) te;
            for (int i = 0; i < 4 && i < lines.length; i++) {
                sign.setText(i, new StringTextComponent(lines[i]));
            }
            sign.markDirty();
            BlockState st = world.getBlockState(pos(x, y, z));
            world.notifyBlockUpdate(pos(x, y, z), st, st, 3);
        }
    }

    /**
     * Двускатная крыша из ступенек: конёк вдоль оси X, скаты к z1 и z2.
     * y — высота первого ряда ступенек.
     */
    public void gableRoof(int x1, int x2, int z1, int z2, int y, BlockState stairs, BlockState gable, BlockState top) {
        int depth = z2 - z1;
        int layers = (depth + 1) / 2;
        for (int k = 0; k < layers; k++) {
            int zf = z1 + k, zb = z2 - k;
            int yy = y + k;
            if (zf >= zb) break;
            for (int x = x1; x <= x2; x++) {
                set(x, yy, zf, stairs(stairs, Direction.SOUTH));
                set(x, yy, zb, stairs(stairs, Direction.NORTH));
            }
            // фронтоны
            for (int z = zf + 1; z < zb; z++) {
                set(x1 + 1, yy, z, gable);
                set(x2 - 1, yy, z, gable);
            }
        }
        if (depth % 2 == 0) {
            int zc = z1 + depth / 2;
            for (int x = x1; x <= x2; x++) set(x, y + depth / 2, zc, top);
        }
    }

    private static int offX(Direction d) { return d == Direction.EAST ? 1 : d == Direction.WEST ? -1 : 0; }
    private static int offZ(Direction d) { return d == Direction.SOUTH ? 1 : d == Direction.NORTH ? -1 : 0; }

    public BlockPos min() { return new BlockPos(minX, minY, minZ); }
    public BlockPos max() { return new BlockPos(maxX, maxY, maxZ); }
}
