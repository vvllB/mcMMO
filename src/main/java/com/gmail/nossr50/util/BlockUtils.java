package com.gmail.nossr50.util;

import com.gmail.nossr50.config.experience.ExperienceConfig;
import com.gmail.nossr50.datatypes.meta.BonusDropMeta;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.datatypes.skills.SubSkillType;
import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.skills.repair.Repair;
import com.gmail.nossr50.skills.salvage.Salvage;
import com.gmail.nossr50.util.random.RandomChanceSkill;
import com.gmail.nossr50.util.random.RandomChanceUtil;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;

public final class BlockUtils {

    private BlockUtils() {
    }

    /**
     * Mark a block for giving bonus drops, double drops are used if triple is false
     *
     * @param blockState target blockstate
     * @param triple     marks the block to give triple drops
     */
    public static void markDropsAsBonus(BlockState blockState, boolean triple) {
        if (triple)
            blockState.setMetadata(MetadataConstants.METADATA_KEY_BONUS_DROPS, new BonusDropMeta(2, mcMMO.p));
        else
            blockState.setMetadata(MetadataConstants.METADATA_KEY_BONUS_DROPS, new BonusDropMeta(1, mcMMO.p));
    }

    /**
     * Set up the state for a block to be seen as unnatural and cleanup any unwanted metadata from the block
     * @param block target block
     */
    public static void setUnnaturalBlock(@NotNull Block block) {
        mcMMO.getPlaceStore().setTrue(block);

        // Failsafe against lingering metadata
        if(block.hasMetadata(MetadataConstants.METADATA_KEY_BONUS_DROPS))
            block.removeMetadata(MetadataConstants.METADATA_KEY_BONUS_DROPS, mcMMO.p);
    }

    /**
     * Cleans up some block metadata when a block breaks and the metadata is no longer needed
     * This also sets the blocks coords to false in our chunk store
     * @param block target block
     */
    public static void cleanupBlockMetadata(Block block) {
        if(block.hasMetadata(MetadataConstants.METADATA_KEY_REPLANT)) {
            block.removeMetadata(MetadataConstants.METADATA_KEY_REPLANT, mcMMO.p);
        }

        mcMMO.getPlaceStore().setFalse(block);
    }

    /**
     * Marks a block to drop extra copies of items
     * @param blockState target blockstate
     * @param amount amount of extra items to drop
     */
    public static void markDropsAsBonus(BlockState blockState, int amount) {
            blockState.setMetadata(MetadataConstants.METADATA_KEY_BONUS_DROPS, new BonusDropMeta(amount, mcMMO.p));
    }

    /**
     * Checks if a player successfully passed the double drop check
     *
     * @param blockState the blockstate
     * @return true if the player succeeded in the check
     */
    public static boolean checkDoubleDrops(Player player, BlockState blockState, PrimarySkillType skillType, SubSkillType subSkillType) {
        if (mcMMO.p.getGeneralConfig().getDoubleDropsEnabled(skillType, blockState.getType()) && Permissions.isSubSkillEnabled(player, subSkillType)) {
            return RandomChanceUtil.checkRandomChanceExecutionSuccess(new RandomChanceSkill(player, subSkillType, true));
        }

        return false;
    }

    /**
     * Checks to see if a given block awards XP.
     *
     * @param blockState The {@link BlockState} of the block to check
     * @return true if the block awards XP, false otherwise
     */
    public static boolean shouldBeWatched(BlockState blockState) {
        return affectedByGigaDrillBreaker(blockState) || affectedByGreenTerra(blockState) || affectedBySuperBreaker(blockState) || hasWoodcuttingXP(blockState)
                || mcMMO.p.getGeneralConfig().getDoubleDropsEnabled(PrimarySkillType.MINING, blockState.getType())
                || mcMMO.p.getGeneralConfig().getDoubleDropsEnabled(PrimarySkillType.EXCAVATION, blockState.getType())
                || mcMMO.p.getGeneralConfig().getDoubleDropsEnabled(PrimarySkillType.WOODCUTTING, blockState.getType())
                || mcMMO.p.getGeneralConfig().getDoubleDropsEnabled(PrimarySkillType.SMELTING, blockState.getType());
    }

    /**
     * Check if a given block should allow for the activation of abilities
     *
     * @param blockState The {@link BlockState} of the block to check
     * @return true if the block should allow ability activation, false
     * otherwise
     */
    public static boolean canActivateAbilities(BlockState blockState) {
        return !mcMMO.getMaterialMapStore().isAbilityActivationBlackListed(blockState.getType());
    }

    /**
     * Check if a given block should allow for the activation of tools
     * Activating a tool is step 1 of a 2 step process for super ability activation
     *
     * @param blockState The {@link BlockState} of the block to check
     * @return true if the block should allow ability activation, false
     * otherwise
     */
    public static boolean canActivateTools(BlockState blockState) {
        return !mcMMO.getMaterialMapStore().isToolActivationBlackListed(blockState.getType())
                && blockState.getType() != Repair.anvilMaterial
                && blockState.getType() != Salvage.anvilMaterial;
    }

    /**
     * Check if a given block is an ore
     *
     * @param blockState The {@link BlockState} of the block to check
     * @return true if the block is an ore, false otherwise
     */
    public static boolean isOre(BlockState blockState) {
        return MaterialUtils.isOre(blockState.getType());
    }

    /**
     * Determine if a given block can be made mossy
     *
     * @param blockState The {@link BlockState} of the block to check
     * @return true if the block can be made mossy, false otherwise
     */
    public static boolean canMakeMossy(BlockState blockState) {
        return mcMMO.getMaterialMapStore().isMossyWhiteListed(blockState.getType());
    }

    /**
     * Determine if a given block should be affected by Green Terra
     *
     * @param blockState The {@link BlockState} of the block to check
     * @return true if the block should affected by Green Terra, false otherwise
     */
    public static boolean affectedByGreenTerra(BlockState blockState) {
        if (ExperienceConfig.getInstance().doesBlockGiveSkillXP(PrimarySkillType.HERBALISM, blockState.getBlockData())) {
            return true;
        }

        return mcMMO.getModManager().isCustomHerbalismBlock(blockState);
    }

    /**
     * Determine if a given block should be affected by Super Breaker
     *
     * @param blockState The {@link BlockState} of the block to check
     * @return true if the block should affected by Super Breaker, false
     * otherwise
     */
    public static boolean affectedBySuperBreaker(BlockState blockState) {
        if(mcMMO.getMaterialMapStore().isIntendedToolPickaxe(blockState.getType()))
            return true;

        if (ExperienceConfig.getInstance().doesBlockGiveSkillXP(PrimarySkillType.MINING, blockState.getBlockData()))
            return true;

        return mcMMO.getModManager().isCustomMiningBlock(blockState);
    }

    /**
     * Determine if a given block should be affected by Giga Drill Breaker
     *
     * @param blockState The {@link BlockState} of the block to check
     * @return true if the block should affected by Giga Drill Breaker, false
     * otherwise
     */
    public static boolean affectedByGigaDrillBreaker(@NotNull BlockState blockState) {
        if (ExperienceConfig.getInstance().doesBlockGiveSkillXP(PrimarySkillType.EXCAVATION, blockState.getBlockData()))
            return true;
        return mcMMO.getModManager().isCustomExcavationBlock(blockState);
    }

    /**
     * Check if a given block is a log
     *
     * @param blockState The {@link BlockState} of the block to check
     * @return true if the block is a log, false otherwise
     */
    public static boolean hasWoodcuttingXP(@NotNull BlockState blockState) {
        return ExperienceConfig.getInstance().doesBlockGiveSkillXP(PrimarySkillType.WOODCUTTING, blockState.getBlockData());
    }

    /**
     * Check if a given block is a leaf
     *
     * @param blockState The {@link BlockState} of the block to check
     * @return true if the block is a leaf, false otherwise
     */
    public static boolean isNonWoodPartOfTree(@NotNull BlockState blockState) {
        return mcMMO.getMaterialMapStore().isTreeFellerDestructible(blockState.getType());
    }

    public static boolean isNonWoodPartOfTree(@NotNull Material material) {
        return mcMMO.getMaterialMapStore().isTreeFellerDestructible(material);
    }

    /**
     * Determine if a given block should be affected by Flux Mining
     *
     * @param blockState The {@link BlockState} of the block to check
     * @return true if the block should affected by Flux Mining, false otherwise
     */
    public static boolean affectedByFluxMining(BlockState blockState) {
        switch (blockState.getType()) {
            case IRON_ORE:
            case GOLD_ORE:
                return true;

            default:
                return false;
        }
    }

    /**
     * Determine if a given block can activate Herbalism abilities
     *
     * @param blockState The {@link BlockState} of the block to check
     * @return true if the block can be activate Herbalism abilities, false
     * otherwise
     */
    public static boolean canActivateHerbalism(BlockState blockState) {
        return mcMMO.getMaterialMapStore().isHerbalismAbilityWhiteListed(blockState.getType());
    }

    /**
     * Determine if a given block should be affected by Block Cracker
     *
     * @param blockState The {@link BlockState} of the block to check
     * @return true if the block should affected by Block Cracker, false
     * otherwise
     */
    public static boolean affectedByBlockCracker(BlockState blockState) {
        return mcMMO.getMaterialMapStore().isBlockCrackerWhiteListed(blockState.getType());
    }

    /**
     * Determine if a given block can be made into Mycelium
     *
     * @param blockState The {@link BlockState} of the block to check
     * @return true if the block can be made into Mycelium, false otherwise
     */
    public static boolean canMakeShroomy(BlockState blockState) {
        return mcMMO.getMaterialMapStore().isShroomyWhiteListed(blockState.getType());
    }

    /**
     * Determine if a given block is an mcMMO anvil
     *
     * @param blockState The {@link BlockState} of the block to check
     * @return true if the block is an mcMMO anvil, false otherwise
     */
    public static boolean isMcMMOAnvil(BlockState blockState) {
        Material type = blockState.getType();

        return type == Repair.anvilMaterial || type == Salvage.anvilMaterial;
    }

    public static boolean isPistonPiece(BlockState blockState) {
        Material type = blockState.getType();

        return type == Material.MOVING_PISTON || type == Material.AIR;
    }

    /**
     * Get a HashSet containing every transparent block
     *
     * @return HashSet with the IDs of every transparent block
     */
    public static HashSet<Material> getTransparentBlocks() {
        HashSet<Material> transparentBlocks = new HashSet<>();

        for (Material material : Material.values()) {
            if (material.isTransparent()) {
                transparentBlocks.add(material);
            }
        }

        return transparentBlocks;
    }

    public static boolean isFullyGrown(BlockState blockState) {
        BlockData data = blockState.getBlockData();
        if (data.getMaterial() == Material.CACTUS || data.getMaterial() == Material.SUGAR_CANE) {
            return true;
        }
        if (data instanceof Ageable ageable) {
            return ageable.getAge() == ageable.getMaximumAge();
        }
        return true;
    }

    public static boolean isPartOfTree(Block block) {
        return hasWoodcuttingXP(block.getState()) || isNonWoodPartOfTree(block.getType());
    }

    /**
     * Checks to see if a Block is within the world bounds
     * Prevent processing blocks from other plugins (or perhaps odd spigot anomalies) from sending blocks that can't exist within the world
     * @param block
     * @return
     */
    public static boolean isWithinWorldBounds(@NotNull Block block) {
        World world = block.getWorld();

        //World min height = inclusive | World max height = exclusive
        return block.getY() >= world.getMinHeight() && block.getY() < world.getMaxHeight();
    }

}
