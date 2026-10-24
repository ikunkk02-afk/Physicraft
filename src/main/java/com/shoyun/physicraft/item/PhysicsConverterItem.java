package com.shoyun.physicraft.item;

import com.shoyun.physicraft.Config;
import com.shoyun.physicraft.entity.PhysicsBlockEntity;
import com.shoyun.physicraft.physics.BulletNative;
import com.shoyun.physicraft.physics.LevelPhysics;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class PhysicsConverterItem extends Item {
    public PhysicsConverterItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Player player = context.getPlayer();
        if (player != null && !player.getAbilities().mayBuild) {
            return InteractionResult.FAIL;
        }
        if (!PhysicsBlockEntity.canConvert(level, pos, state)) {
            if (player != null && !level.isClientSide()) {
                player.displayClientMessage(Component.translatable("physicraft.converter.not_full_cube"), true);
            }
            return InteractionResult.FAIL;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ServerLevel server = (ServerLevel) level;
        LevelPhysics physics = LevelPhysics.get(server);
        if (!physics.isReady()) {
            if (player != null) {
                player.displayClientMessage(Component.translatable("physicraft.converter.native_failed", BulletNative.failureReason()), true);
            }
            return InteractionResult.FAIL;
        }
        if (physics.dynamicCount() >= Config.MAX_BODIES.get()) {
            if (player != null) {
                player.displayClientMessage(Component.translatable("physicraft.converter.too_many", Config.MAX_BODIES.get()), true);
            }
            return InteractionResult.FAIL;
        }

        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        PhysicsBlockEntity entity = PhysicsBlockEntity.converted(server, pos, state);
        if (!level.addFreshEntity(entity)) {
            level.setBlock(pos, state, Block.UPDATE_ALL);
            if (player != null) {
                player.displayClientMessage(Component.translatable("physicraft.converter.failed"), true);
            }
            return InteractionResult.FAIL;
        }
        level.levelEvent(2001, pos, Block.getId(state));
        level.playSound(null, pos, state.getSoundType(level, pos, player).getBreakSound(), SoundSource.BLOCKS, 0.7F, 1.15F);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.physicraft.physics_converter.hint").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
