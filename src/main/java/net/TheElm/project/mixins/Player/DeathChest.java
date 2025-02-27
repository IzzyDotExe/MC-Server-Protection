/*
 * This software is licensed under the MIT License
 * https://github.com/GStefanowich/MC-Server-Protection
 *
 * Copyright (c) 2019 Gregory Stefanowich
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.TheElm.project.mixins.Player;

import net.TheElm.project.config.SewConfig;
import net.TheElm.project.interfaces.BackpackCarrier;
import net.TheElm.project.interfaces.BlockPlaceCallback;
import net.TheElm.project.interfaces.MoneyHolder;
import net.TheElm.project.interfaces.Nicknamable;
import net.TheElm.project.interfaces.PlayerData;
import net.TheElm.project.objects.PlayerBackpack;
import net.TheElm.project.utilities.DeathChestUtils;
import net.TheElm.project.utilities.InventoryUtils;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class DeathChest extends LivingEntity implements MoneyHolder, BackpackCarrier {
    
    // Backpack
    private PlayerBackpack backpack = null;
    
    // Player inventory
    @Shadow public PlayerInventory inventory;
    @Shadow protected abstract void vanishCursedItems();
    
    protected DeathChest(EntityType<? extends LivingEntity> entityType_1, World world_1) {
        super(entityType_1, world_1);
    }
    
    /* 
     * If player drops inventory (At death, stop that!)
     */
    
    @Inject(at = @At("HEAD"), method = "dropInventory", cancellable = true)
    public void onInventoryDrop(CallbackInfo callback) {
        boolean keepInventory = this.world.getGameRules().getBoolean(GameRules.KEEP_INVENTORY);
        if (!SewConfig.get(SewConfig.DO_DEATH_CHESTS)) {
            // Drop the backpack if we're not using death chests (And keep inventory is off)
            if (!keepInventory) {
                DeathChestUtils.createDeathSnapshotFor((PlayerEntity)(LivingEntity) this);
                
                // Drop the contents of the backpack (Only if the player HAS one)
                if (this.backpack != null)
                    this.backpack.dropAll(true);
            }
            return;
        }
        
        // Only do if we're not keeping the inventory, and the player is actually dead! (Death Chest!)
        if (!keepInventory && !this.isAlive()) {
            DeathChestUtils.createDeathSnapshotFor((PlayerEntity)(LivingEntity) this);
            BlockPos chestPos;
            
            // Check if player is in combat
            if (SewConfig.get(SewConfig.PVP_DISABLE_DEATH_CHEST) && (this.hitByOtherPlayerAt != null)) {
                // Drop the backpack as well as the inventory (Only if the player HAS one)
                if (this.backpack != null)
                    this.backpack.dropAll(true);
                
                // Tell the player that they didn't get a death chest
                this.sendSystemMessage(
                    new LiteralText("A death chest was not generated because you died in combat.").formatted(Formatting.RED),
                    Util.NIL_UUID
                );
                
                // Reset the hit by time
                this.hitByOtherPlayerAt = null;
                return;
            }
            
            // If the inventory is NOT empty, and we found a valid position for the death chest
            if ((!(InventoryUtils.isInvEmpty(this.inventory) && InventoryUtils.isInvEmpty(this.backpack))) && ((chestPos = DeathChestUtils.getChestPosition(this.getEntityWorld(), this.getBlockPos() )) != null)) {
                // Vanish cursed items
                this.vanishCursedItems();
                
                // If a death chest was successfully spawned
                if (DeathChestUtils.createDeathChestFor((PlayerEntity)(LivingEntity) this, chestPos))
                    callback.cancel();
            }
        }
    }
    
    /*
     * Stats of Mob Kills
     */
    
    @Inject(at = @At("HEAD"), method = "onKilledOther", cancellable = true)
    public void onKilledTarget(ServerWorld serverWorld, LivingEntity livingEntity, CallbackInfo callback) {
        if (livingEntity instanceof EnderDragonEntity)
            callback.cancel();
    }
    
    /*
     * Player Combat
     */
    
    private Long hitByOtherPlayerAt = null;
    
    @Inject(at = @At("TAIL"), method = "tick")
    public void onTick(CallbackInfo callback) {
        if ((!this.world.isClient) && ((Entity) this) instanceof ServerPlayerEntity) {
            if (this.hitByOtherPlayerAt != null && (this.hitByOtherPlayerAt < System.currentTimeMillis() - (SewConfig.get(SewConfig.PVP_COMBAT_SECONDS) * 1000))) {
                // Remove player from combat
                this.hitByOtherPlayerAt = null;
                
                // Send message about being out of combat
                this.sendSystemMessage(
                    new LiteralText("You are no longer in combat.").formatted(Formatting.YELLOW),
                    Util.NIL_UUID
                );
                
                // Clear players from the health bar
                ServerBossBar healthBar = ((PlayerData) this).getHealthBar(false);
                if (healthBar != null)
                    healthBar.clearPlayers();
            }
        }
    }
    
    @Inject(at = @At("RETURN"), method = "damage")
    public void onDamage(DamageSource source, float damage, CallbackInfoReturnable<Boolean> callback) {
        if ((!this.world.isClient) && ((Entity) this) instanceof ServerPlayerEntity) {
            if (source.getAttacker() instanceof PlayerEntity && callback.getReturnValue()) {
                // If player just entered combat
                if (this.hitByOtherPlayerAt == null)
                    this.sendSystemMessage(new LiteralText("You are now in combat.").formatted(Formatting.YELLOW), Util.NIL_UUID);
                
                // Set combat time to when hit
                this.hitByOtherPlayerAt = System.currentTimeMillis();
            }
        }
    }
    
    @Inject(at = @At("RETURN"), method = "getArrowType", cancellable = true)
    public void onCheckArrowType(ItemStack weapon, CallbackInfoReturnable<ItemStack> callback) {
        if ((weapon.getItem() instanceof RangedWeaponItem) && callback.getReturnValue().isEmpty() && (EnchantmentHelper.getLevel(Enchantments.INFINITY, weapon) > 0 ))
            callback.setReturnValue(new ItemStack(Items.ARROW));
    }
    
    /* 
     * Override the players display name to their nick
     */
    @Inject(at = @At("HEAD"), method = "getDisplayName", cancellable = true)
    public void getPlayerNickname(CallbackInfoReturnable<Text> callback) {
        if ((((LivingEntity)this) instanceof ServerPlayerEntity) && (((Nicknamable)this).getPlayerNickname() != null))
            callback.setReturnValue(((Nicknamable) this).getPlayerNickname());
    }
    
    /*
     * Tracked Data
     */
    @Inject(at = @At("RETURN"), method = "initDataTracker")
    public void onInitDataTracking(CallbackInfo callback) {
        this.dataTracker.startTracking( MONEY, SewConfig.get(SewConfig.STARTING_MONEY) );
    }
    @Inject(at = @At("TAIL"), method = "writeCustomDataToNbt")
    public void onSavingData(NbtCompound tag, CallbackInfo callback) {
        // Save the players money
        tag.putInt(MoneyHolder.SAVE_KEY, this.getPlayerWallet());
        
        // Store the players backpack
        if (this.backpack != null) {
            tag.putInt("BackpackSize", this.backpack.getRows());
            tag.put("Backpack", this.backpack.getTags());
            
            NbtList pickupTags = this.backpack.getPickupTags();
            if (!pickupTags.isEmpty())
                tag.put("BackpackPickup", pickupTags);
        }
    }
    @Inject(at = @At("TAIL"), method = "readCustomDataFromNbt")
    public void onReadingData(NbtCompound tag, CallbackInfo callback) {
        // Read the players money
        if (tag.contains(MoneyHolder.SAVE_KEY, NbtElement.NUMBER_TYPE))
            this.dataTracker.set( MONEY, tag.getInt( MoneyHolder.SAVE_KEY ) );
        
        // Read the players backpack
        if (tag.contains("BackpackSize", NbtElement.NUMBER_TYPE) && tag.contains("Backpack", NbtElement.LIST_TYPE)) {
            this.backpack = new PlayerBackpack((PlayerEntity)(LivingEntity)this, tag.getInt("BackpackSize"));
            this.backpack.readTags(tag.getList("Backpack", NbtElement.COMPOUND_TYPE));
            
            if (tag.contains("BackpackPickup", NbtElement.LIST_TYPE))
                this.backpack.readPickupTags(tag.getList("BackpackPickup", NbtElement.STRING_TYPE));
        } else {
            int startingBackpack = SewConfig.get(SewConfig.BACKPACK_STARTING_ROWS);
            if ( startingBackpack > 0 )
                this.backpack = new PlayerBackpack((PlayerEntity)(LivingEntity)this, Math.min(startingBackpack, 6));
        }
    }
    
    /*
     * Money
     */
    @Override
    public int getPlayerWallet() {
        return this.dataTracker.get( MONEY );
    }
    
    /*
     * Player Backpack
     */
    @Override
    public @Nullable PlayerBackpack getBackpack() {
        return this.backpack;
    }
    @Override
    public void setBackpack(PlayerBackpack backpack) {
        this.backpack = backpack;
    }
    @Inject(at = @At("TAIL"), method = "vanishCursedItems")
    public void onVanishCursedItems(CallbackInfo callback) {
        if (this.backpack == null)
            return;
        for (int i = 0; i < this.backpack.size(); i++) {
            ItemStack stack = this.backpack.getStack(i);
            if (!stack.isEmpty() && EnchantmentHelper.hasVanishingCurse(stack))
                this.backpack.removeStack(i);
        }
    }
    
    /*
     * Item placement
     */
    @Inject(at = @At("HEAD"), method = "canPlaceOn", cancellable = true)
    public void checkPlacement(BlockPos blockPos, Direction direction, ItemStack itemStack, CallbackInfoReturnable<Boolean> callback) {
        ActionResult result = BlockPlaceCallback.EVENT.invoker().interact((ServerPlayerEntity)(LivingEntity)this, this.world, blockPos, direction, itemStack);
        if (result != ActionResult.PASS)
            callback.setReturnValue(result == ActionResult.SUCCESS);
    }
    
}
