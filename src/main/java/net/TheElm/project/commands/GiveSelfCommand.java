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

package net.TheElm.project.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.TheElm.project.ServerCore;
import net.TheElm.project.enums.OpLevels;
import net.TheElm.project.interfaces.CommandPredicate;
import net.minecraft.command.argument.ItemStackArgument;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;

/**
 * Created on Aug 13 2021 at 1:58 PM.
 * By greg in SewingMachineMod
 */
public final class GiveSelfCommand {

    public static void register(@NotNull CommandDispatcher<ServerCommandSource> dispatcher) {
        ServerCore.register(dispatcher, "I", "Give Self", builder -> builder
            .then(CommandManager.argument("item", ItemStackArgumentType.itemStack())
                .requires(CommandPredicate.opLevel(OpLevels.CHEATING))
                .then(CommandManager.argument("count", IntegerArgumentType.integer(1))
                    .executes(GiveSelfCommand::giveSelfAmount)
                )
                .executes(GiveSelfCommand::giveDefaultSize)
            )
        );
    }
    
    private static int giveDefaultSize(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return GiveSelfCommand.giveSelfAmount(context, 1);
    }
    private static int giveSelfAmount(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return GiveSelfCommand.giveSelfAmount(context, IntegerArgumentType.getInteger(context, "count"));
    }
    private static int giveSelfAmount(@NotNull CommandContext<ServerCommandSource> context, final int amount) throws CommandSyntaxException {
        final ServerCommandSource source = context.getSource();
        final ServerPlayerEntity player = source.getPlayer();
        final ItemStackArgument itemArg = ItemStackArgumentType.getItemStackArgument(context, "item");
        final Item item = itemArg.getItem();
        final int stackSize = Math.min(item.getMaxCount(), amount);
        int remainder = amount;
        
        do {
            ItemEntity itemEntity;
            ItemStack itemStack = itemArg.createStack(MathHelper.clamp(remainder, 1, stackSize), false);
            
            if (player.getInventory().insertStack(itemStack) && itemStack.isEmpty()) {
                itemStack.setCount(1);
                itemEntity = player.dropItem(itemStack, false);
                if (itemEntity != null)
                    itemEntity.setDespawnImmediately();
                
                player.world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 0.2F, ((player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F);
                player.playerScreenHandler.sendContentUpdates();
            } else {
                itemEntity = player.dropItem(itemStack, false);
                if (itemEntity != null) {
                    itemEntity.resetPickupDelay();
                    itemEntity.setOwner(player.getUuid());
                }
            }
        } while ((remainder -= stackSize) > 0);
        
        source.sendFeedback(new TranslatableText("commands.give.success.single", amount, itemArg.createStack(amount, false).toHoverableText(), player.getDisplayName()), true);
        return amount;
    }
    
}
