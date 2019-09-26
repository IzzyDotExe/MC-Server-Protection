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

import net.TheElm.project.CoreMod;
import net.TheElm.project.config.SewingMachineConfig;
import net.TheElm.project.enums.ClaimPermissions;
import net.TheElm.project.enums.ClaimRanks;
import net.TheElm.project.interfaces.IClaimedChunk;
import net.TheElm.project.protections.claiming.ClaimantPlayer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.dimension.DimensionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.UUID;

public final class ChunkUtils {

    /**
     * Check the database if a user can perform an action within the specified chunk
     */
    public static boolean canPlayerDoInChunk(@NotNull ClaimPermissions perm, @NotNull PlayerEntity player, @NotNull BlockPos blockPos) {
        return ChunkUtils.canPlayerDoInChunk( perm, player, player.getEntityWorld().getWorldChunk( blockPos ));
    }
    public static boolean canPlayerDoInChunk(@NotNull ClaimPermissions perm, @NotNull PlayerEntity player, @Nullable WorldChunk chunk) {
        // If claims are disabled
        if ((!SewingMachineConfig.INSTANCE.DO_CLAIMS.get()) || player.isCreative()) return true;
        
        // Return false (Chunks should never BE null, but this is our catch)
        if ( chunk == null ) return false;
        
        // Check if player can do action in chunk
        return ((IClaimedChunk) chunk).canUserDo( player.getUuid(), perm );
    }
    
    /**
     * Check the database if a user can ride entities within the specified chunk
     * @param player The player to check
     * @param blockPos The block position of the interaction
     * @return If the player can ride entities
     */
    public static boolean canPlayerRideInChunk(PlayerEntity player, BlockPos blockPos) {
        return ChunkUtils.canPlayerDoInChunk( ClaimPermissions.RIDING, player, blockPos );
    }
    
    /**
     * Check the database if a user can place/break blocks within the specified chunk
     * @param player The player to check
     * @param blockPos The block position of the interaction
     * @return If the player can break blocks
     */
    public static boolean canPlayerBreakInChunk(PlayerEntity player, BlockPos blockPos) {
        return ChunkUtils.canPlayerDoInChunk( ClaimPermissions.BLOCKS, player, blockPos );
    }
    
    /**
     * Check the database if a user can loot chests within the specified chunk
     * @param player The player to check
     * @param blockPos The block position of the interaction
     * @return If the player can loot storages
     */
    public static boolean canPlayerLootChestsInChunk(PlayerEntity player, BlockPos blockPos) {
        return ChunkUtils.canPlayerDoInChunk( ClaimPermissions.STORAGE, player, blockPos );
    }
    public static boolean canPlayerLootChestsInChunk(PlayerEntity player, WorldChunk chunk) {
        return ChunkUtils.canPlayerDoInChunk( ClaimPermissions.STORAGE, player, chunk );
    }
    
    /**
     * Check the database if a user can  within the specified chunk
     * @param player The player to check
     * @param blockPos The block position of the interaction
     * @return If player can pick up dropped items
     */
    public static boolean canPlayerLootDropsInChunk(PlayerEntity player, BlockPos blockPos) {
        return ChunkUtils.canPlayerDoInChunk( ClaimPermissions.PICKUP, player, blockPos );
    }
    
    /**
     * Check the database if a user can interact with doors within the specified chunk
     * @param player The player to check
     * @param blockPos The block position of the interaction
     * @return If player can interact with doors
     */
    public static boolean canPlayerToggleDoor(PlayerEntity player, BlockPos blockPos) {
        return ChunkUtils.canPlayerDoInChunk( ClaimPermissions.DOORS, player, blockPos );
    }
    public static boolean canPlayerToggleDoor(PlayerEntity player, WorldChunk chunk) {
        return ChunkUtils.canPlayerDoInChunk( ClaimPermissions.DOORS, player, chunk );
    }
    
    /**
     * Check the database if a user can interact with mobs within the specified chunk
     * @param player The player to check
     * @param blockPos The block position of the interaction
     * @return If player can harm or loot friendly entities
     */
    public static boolean canPlayerInteractFriendlies(PlayerEntity player, BlockPos blockPos) {
        return ChunkUtils.canPlayerDoInChunk( ClaimPermissions.CREATURES, player, blockPos );
    }
    
    /**
     * Check the database if a user can harvest crops within the specified chunk
     * @param player The player to check
     * @param blockPos The block position of the interaction
     * @return If the player is allowed to harvest crops
     */
    public static boolean canPlayerHarvestCrop(PlayerEntity player, BlockPos blockPos) {
        return ChunkUtils.canPlayerDoInChunk( ClaimPermissions.HARVEST, player, blockPos );
    }
    
    /**
     * @param player The player that wants to teleport
     * @param target The destination to teleport to
     * @return If the player is a high enough rank to teleport to the target
     */
    public static boolean canPlayerWarpTo(PlayerEntity player, UUID target) {
        // Check our chunk permissions
        ClaimantPlayer permissions = ClaimantPlayer.get( target );
        if ( permissions == null ) return false; // Permissions should not be NULL unless something is wrong
        
        // Get the ranks of the user and the rank required for performing
        ClaimRanks userRank = permissions.getFriendRank( player.getUuid() );
        ClaimRanks permReq = permissions.getPermissionRankRequirement( ClaimPermissions.WARP );
        
        // Return the test if the user can perform the action
        return permReq.canPerform( userRank );
    }
    
    /*
     * Get data about where the player is
     */
    @Nullable
    public static UUID getPlayerLocation(@NotNull final ServerPlayerEntity player) {
        return CoreMod.PLAYER_LOCATIONS.get( player );
    }
    public static boolean isPlayerWithinSpawn(@NotNull final ServerPlayerEntity player) {
        if (!SewingMachineConfig.INSTANCE.DO_CLAIMS.get())
            return true;
        return CoreMod.spawnID.equals(ChunkUtils.getPlayerLocation( player ));
    }
    public static int getPositionWithinChunk(BlockPos blockPos) {
        int chunkIndex = blockPos.getX() & 0xF;
        return (chunkIndex |= (blockPos.getZ() & 0xF) << 4);
    }
    
    public static Text getPlayerWorldWilderness(@NotNull final PlayerEntity player) {
        if (player.getEntityWorld().dimension.getType() == DimensionType.THE_END) {
            return TranslatableServerSide.text( player, "claim.wilderness.end" ).formatted( Formatting.BLACK );
            
        } else if (player.getEntityWorld().dimension.getType() == DimensionType.THE_NETHER) {
            return TranslatableServerSide.text( player, "claim.wilderness.nether" ).formatted( Formatting.RED );
            
        }
        return TranslatableServerSide.text( player, "claim.wilderness.general" ).formatted( Formatting.GREEN );
    }
    
    /*
     * Chunk claim classes
     */
    public static final class ClaimSlice {
        private final NavigableMap<Integer, InnerClaim> innerChunks = Collections.synchronizedNavigableMap(new TreeMap<>());
        
        public ClaimSlice() {
            this.innerChunks.put( -1, new InnerClaim( null ));
        }
        
        public void set(InnerClaim claim) {
            this.innerChunks.put( claim.lower(), claim );
        }
        @NotNull
        public InnerClaim get(int y) {
            return this.innerChunks.floorEntry( y ).getValue();
        }
        @NotNull
        public InnerClaim get(BlockPos blockPos) {
            return this.get(blockPos.getY());
        }
        
        public Iterator<InnerClaim> getClaims() {
            return this.innerChunks.values().iterator();
        }
    }
    public static final class InnerClaim {
        
        private final UUID owner;
        private final int yUpper;
        private final int yLower;
        
        public InnerClaim(@Nullable UUID owner) {
            this( owner, -1, -1 );
        }
        public InnerClaim(@Nullable UUID owner, int upper, int lower) {
            this.owner = owner;
            this.yUpper = ( upper > 256 ? 256 : Collections.max(Arrays.asList( upper, lower )));
            this.yLower = ( lower < -1 ? -1 : lower);
        }
        
        @Nullable
        public UUID getOwner() {
            return this.owner;
        }
        public int upper() {
            return this.yUpper;
        }
        public int lower() {
            return this.yLower;
        }
        
    }
    
}
