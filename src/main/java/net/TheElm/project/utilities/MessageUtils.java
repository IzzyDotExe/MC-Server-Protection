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

package net.TheElm.project.utilities;

import net.TheElm.project.ServerCore;
import net.TheElm.project.enums.ChatRooms;
import net.TheElm.project.interfaces.PlayerData;
import net.TheElm.project.protections.claiming.ClaimantPlayer;
import net.TheElm.project.protections.claiming.ClaimantTown;
import net.minecraft.network.MessageType;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.List;
import java.util.stream.Stream;

public final class MessageUtils {
    
    private MessageUtils() {}
    
    // Send a translation blob to a local area
    public static void sendToLocal(final World world, final BlockPos blockPos, Text text) {
        // Log to the server
        ((ServerWorld) world).getServer().sendMessage(text);
        
        // Get the players in the area
        BlockPos outerA = new BlockPos(blockPos.getX() + 800, 0, blockPos.getZ() + 800);
        BlockPos outerB = new BlockPos(blockPos.getX() - 800, 800, blockPos.getZ() - 800);
        List<ServerPlayerEntity> players = world.getEntities(ServerPlayerEntity.class, new Box(outerA, outerB), EntityPredicates.VALID_ENTITY);
        
        // Send the message to the players
        MessageUtils.sendChat(
            players.stream(),
            text
        );
    }
    
    // Send a translation blob to all Players
    public static void sendToAll(final String translationKey, final Object... objects) {
        final MinecraftServer server = ServerCore.get();
        MessageUtils.sendSystem(
            server.getPlayerManager().getPlayerList().stream(),
            translationKey,
            objects
        );
    }
    public static void sendToAll(final Text text) {
        final MinecraftServer server = ServerCore.get();
        // Log to the server
        server.sendMessage(text);
        
        // Send to the players
        MessageUtils.sendChat(
            server.getPlayerManager().getPlayerList().stream(),
            text
        );
    }
    
    // Send a translation blob to a Town
    public static void sendToTown(final ClaimantTown town, final String translationKey, final Object... objects) {
        final MinecraftServer server = ServerCore.get();
        MessageUtils.sendSystem(
            server.getPlayerManager().getPlayerList().stream().filter((player) -> {
                ClaimantPlayer claimant = ((PlayerData) player).getClaim();
                return (claimant != null) && (claimant.getTown() != null) && town.getId().equals(claimant.getTown().getId());
            }),
            translationKey,
            objects
        );
    }
    public static void sendToTown(final ClaimantTown town, final Text text) {
        final MinecraftServer server = ServerCore.get();
        // Log to the server
        server.sendMessage(text);
        
        // Send to the players
        MessageUtils.sendChat(
            server.getPlayerManager().getPlayerList().stream().filter((player) -> {
                ClaimantPlayer claimant = ((PlayerData) player).getClaim();
                return (claimant != null) && (claimant.getTown() != null) && town.getId().equals(claimant.getTown().getId());
            }),
            text
        );
    }
    
    // Send a translation blob to OPs
    public static void sendToOps(final String translationKey, final Object... objects) {
        MessageUtils.sendToOps( 1, translationKey, objects );
    }
    public static void sendToOps(final int opLevel, final String translationKey, final Object... objects) {
        final MinecraftServer server = ServerCore.get();
        MessageUtils.sendSystem(
            server.getPlayerManager().getPlayerList().stream().filter((player) -> player.allowsPermissionLevel( opLevel )),
            translationKey,
            objects
        );
    }
    
    // Send a translation blob to a stream of players
    private static void sendSystem(final Stream<ServerPlayerEntity> players, final String translationKey, final Object... objects) {
        players.forEach((player) -> player.sendMessage(
            TranslatableServerSide.text(player, translationKey, objects).formatted(Formatting.YELLOW)
        ));
    }
    private static void sendChat(final Stream<ServerPlayerEntity> players, final Text text) {
        players.forEach((player) -> player.sendChatMessage( text, MessageType.CHAT ));
    }
    
    // Convert a Block Position to a Text component
    public static Text blockPosToTextComponent(final BlockPos pos) {
        return MessageUtils.blockPosToTextComponent( pos, ", " );
    }
    public static Text blockPosToTextComponent(final BlockPos pos, final String separator) {
        return MessageUtils.dimensionToTextComponent( separator, pos.getX(), pos.getY(), pos.getZ() );
    }
    public static Text dimensionToTextComponent(final String separator, final int x, final int y, final int z) {
        return new LiteralText("")
            .append(new LiteralText("" + x).formatted(Formatting.AQUA))
            .append(separator)
            .append(new LiteralText("" + y).formatted(Formatting.AQUA))
            .append(separator)
            .append(new LiteralText("" + z).formatted(Formatting.AQUA));
    }
    
    // Format a message to chat from a player
    public static Text formatPlayerMessage(ServerPlayerEntity player, ChatRooms chatRoom, String raw) {
        return PlayerNameUtils.getPlayerChatDisplay( player, chatRoom )
            .append(new LiteralText( ": " ).formatted(Formatting.GRAY))
            .append(new LiteralText( raw ).formatted(chatRoom.getFormatting()));
    }
    
}
