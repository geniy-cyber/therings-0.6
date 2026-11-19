package org.stringers.therings.concentrating;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.stringers.therings.items.modItems;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class concentating {

    // ============================================================
    // 1. Конвертация LUMERITE на соул-песке
    // ============================================================
    @Mod.EventBusSubscriber(modid = "therings", bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class SoulSandHandler {
        private static final Map<BlockPos, Integer> timers = new HashMap<>();
        private static final int REQUIRED_TICKS = 200;

        @SubscribeEvent
        public static void onLevelTick(TickEvent.LevelTickEvent event) {
            if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
            if (level.getGameTime() % 5 != 0) return;

            for (ServerPlayer player : level.players()) {
                BlockPos center = player.blockPosition();
                for (BlockPos pos : BlockPos.betweenClosed(
                        center.offset(-16, -8, -16),
                        center.offset(16, 8, 16))) {
                    if (level.getBlockState(pos).is(Blocks.SOUL_SAND)) {
                        checkAndConvert(level, pos.immutable());
                    }
                }
            }
        }

        public static void checkAndConvert(ServerLevel level, BlockPos pos) {
            if (!level.getBlockState(pos).is(Blocks.SOUL_SAND)) {
                timers.remove(pos);
                return;
            }

            AABB area = new AABB(
                    pos.getX() - 0.5, pos.getY() + 0.5, pos.getZ() - 0.5,
                    pos.getX() + 1.5, pos.getY() + 3.5, pos.getZ() + 1.5
            );

            List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, area);

            ItemEntity target = null;
            for (ItemEntity ie : items) {
                if (ie.getItem().is(modItems.LUMERITE_INGOT.get())) {
                    target = ie;
                    break;
                }
            }
            if (target == null) {
                timers.remove(pos);
                return;
            }

            int time = timers.getOrDefault(pos, 0) + 5;
            timers.put(pos, time);

            level.sendParticles(
                    ParticleTypes.SOUL_FIRE_FLAME,
                    target.getX(), target.getY() + 0.2, target.getZ(),
                    3, 0.15, 0.1, 0.15, 0.02
            );

            if (time >= REQUIRED_TICKS) {
                double x = target.getX();
                double y = target.getY();
                double z = target.getZ();

                ItemStack stack = target.getItem();
                int count = stack.getCount();

                if (count > 1) {
                    stack.shrink(1);
                    ChargingItem.setCharge(stack, 0); // сброс заряда на оставшемся стеке
                } else {
                    target.discard();
                }

                ItemEntity newItem = new ItemEntity(
                        level, x, y, z,
                        new ItemStack(modItems.CONCENTRATED_LUMERITE_INGOT.get())
                );
                level.addFreshEntity(newItem);

                level.sendParticles(
                        ParticleTypes.SOUL,
                        x, y + 0.5, z,
                        15, 0.3, 0.3, 0.3, 0.05
                );

                timers.remove(pos);
            }
        }
    }

    // ============================================================
    // 2. Предмет с NBT-зарядом
    //    MAX_CHARGE = 31 ≈ 4 уровня игрока
    // ============================================================
    public static class ChargingItem extends Item {

        public static final String CHARGE_TAG = "Charge";
        public static final int MAX_CHARGE = 31;

        public ChargingItem(Properties properties) {
            super(properties);
        }

        public static int getCharge(ItemStack stack) {
            CompoundTag tag = stack.getTag();
            return tag == null ? 0 : tag.getInt(CHARGE_TAG);
        }

        public static void setCharge(ItemStack stack, int charge) {
            if (charge <= 0) {
                // полностью убираем тег, чтобы не тащить "Charge:0"
                if (stack.hasTag()) {
                    stack.getTag().remove(CHARGE_TAG);
                }
                return;
            }
            stack.getOrCreateTag().putInt(CHARGE_TAG,
                    Math.min(MAX_CHARGE, charge));
        }

        @Override
        public boolean isBarVisible(ItemStack stack) {
            return getCharge(stack) > 0;
        }

        @Override
        public int getBarWidth(ItemStack stack) {
            return Math.round(13.0F * getCharge(stack) / MAX_CHARGE);
        }

        @Override
        public int getBarColor(ItemStack stack) {
            return 0xAA00FF;
        }
    }

    // ============================================================
    // 3. Зарядка LUMERITE в оффхенде за счёт опыта
    // ============================================================
    @Mod.EventBusSubscriber(modid = "therings", bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class OffhandXpChargingHandler {

        @SubscribeEvent
        public static void onXpChange(PlayerXpEvent.XpChange event) {
            Player player = event.getEntity();
            int incoming = event.getAmount();
            if (incoming <= 0 || player.level().isClientSide) return;

            ItemStack offhand = player.getItemInHand(InteractionHand.OFF_HAND);
            if (offhand.isEmpty()) return;
            if (!offhand.is(modItems.LUMERITE_INGOT.get())) return;

            int currentCharge = ChargingItem.getCharge(offhand);
            if (currentCharge >= ChargingItem.MAX_CHARGE) return;

            // 3/4 опыта идёт в заряд
            int toCharge = (incoming * 3) / 4;
            if (toCharge <= 0) return;

            int newCharge = Math.min(ChargingItem.MAX_CHARGE, currentCharge + toCharge);
            int actuallyUsed = newCharge - currentCharge;

            ChargingItem.setCharge(offhand, newCharge);
            event.setAmount(incoming - actuallyUsed);

            if (player.level() instanceof ServerLevel level) {
                level.sendParticles(
                        ParticleTypes.SOUL,
                        player.getX(), player.getY() + 1.0, player.getZ(),
                        3, 0.2, 0.3, 0.2, 0.05
                );
            }

            if (newCharge >= ChargingItem.MAX_CHARGE) {
                offhand.shrink(1);

                // ✅ ЯВНЫЙ РЕСЕТ заряда на оставшемся стеке
                if (!offhand.isEmpty()) {
                    ChargingItem.setCharge(offhand, 0);
                }

                ItemStack result = new ItemStack(modItems.CONCENTRATED_LUMERITE_INGOT.get());
                if (!player.getInventory().add(result)) {
                    player.drop(result, false);
                }

                if (player.level() instanceof ServerLevel level) {
                    level.sendParticles(
                            ParticleTypes.SOUL,
                            player.getX(), player.getY() + 1.2, player.getZ(),
                            20, 0.4, 0.4, 0.4, 0.05
                    );
                }
            }
        }
    }
}