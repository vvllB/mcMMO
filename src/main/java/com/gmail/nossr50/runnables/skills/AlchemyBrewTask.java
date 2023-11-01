package com.gmail.nossr50.runnables.skills;

import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.datatypes.skills.SubSkillType;
import com.gmail.nossr50.events.skills.alchemy.McMMOPlayerBrewEvent;
import com.gmail.nossr50.events.skills.alchemy.McMMOPlayerCatalysisEvent;
import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.skills.alchemy.Alchemy;
import com.gmail.nossr50.skills.alchemy.AlchemyPotionBrewer;
import com.gmail.nossr50.util.CancellableRunnable;
import com.gmail.nossr50.util.Misc;
import com.gmail.nossr50.util.Permissions;
import com.gmail.nossr50.util.player.UserManager;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import eu.decentsoftware.holograms.plugin.DecentHologramsPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.BrewingStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.UUID;

import static com.gmail.nossr50.skills.alchemy.AlchemyPotionBrewer.scheduleUpdate;

public class AlchemyBrewTask extends CancellableRunnable {
    private static final double DEFAULT_BREW_SPEED = 1.0;
    private static final int DEFAULT_BREW_TICKS = 400;

    private final BlockState brewingStand;
    private final Location location;
    private double brewSpeed;
    private double brewTimer;
    private final Player player;
    private int fuel;
    private boolean firstRun = true;
    private double totalTime = 0;

    private Hologram inRefining;

    public AlchemyBrewTask(BlockState brewingStand, Player player) {
        this.brewingStand = brewingStand;
        this.location = brewingStand.getLocation();
        this.player = player;

        brewSpeed = DEFAULT_BREW_SPEED;
        brewTimer = DEFAULT_BREW_TICKS;

        if (player != null
                && !Misc.isNPCEntityExcludingVillagers(player)
                && Permissions.isSubSkillEnabled(player, SubSkillType.ALCHEMY_CATALYSIS)
                && UserManager.getPlayer(player) != null) {

            double catalysis = UserManager.getPlayer(player).getAlchemyManager().calculateBrewSpeed(Permissions.lucky(player, PrimarySkillType.ALCHEMY));

            McMMOPlayerCatalysisEvent event = new McMMOPlayerCatalysisEvent(player, catalysis);
            mcMMO.p.getServer().getPluginManager().callEvent(event);

            if (!event.isCancelled()) {
                brewSpeed = catalysis;
            }
        }

        if (Alchemy.brewingStandMap.containsKey(location)) {
            Alchemy.brewingStandMap.get(location).cancel();
        }
        fuel = ((BrewingStand) brewingStand).getFuelLevel();
//        int brewingTime = ((BrewingStand) brewingStand).getBrewingTime();
//        if (brewingTime == 0) // Only decrement on our end if it isn't a vanilla ingredient.
        fuel--;

        Alchemy.brewingStandMap.put(location, this);
        mcMMO.p.getFoliaLib().getImpl().runAtLocationTimer(location, this, 1, 1);

        totalTime = brewTimer;
        DecentHologramsPlugin plugin = (DecentHologramsPlugin) player.getServer().getPluginManager().getPlugin("DecentHolograms");
        if (plugin != null) {
            inRefining = DHAPI.createHologram(UUID.randomUUID().toString(), new Location(location.getWorld(),
                    location.getX() + 0.5,
                    location.getY() + 1.25,
                    location.getZ() + 0.5
            ), Collections.singletonList("§d§l炼制中0.00%"));
        }
    }

    @Override
    public void cancel() {
        if (inRefining != null) {
            inRefining.destroy();
        }
        super.cancel();
    }


    @Override
    public void run() {
        if (player == null || !player.isValid() || brewingStand == null || brewingStand.getType() != Material.BREWING_STAND || !AlchemyPotionBrewer.isValidIngredient(player, ((BrewingStand) brewingStand).getInventory().getContents()[Alchemy.INGREDIENT_SLOT])) {
            if (Alchemy.brewingStandMap.containsKey(location)) {
                Alchemy.brewingStandMap.remove(location);
            }
            this.cancel();
            return;
        }

        if (firstRun) {
            firstRun = false;
            ((BrewingStand) brewingStand).setFuelLevel(fuel);

            ItemStack ingredient = ((BrewingStand) brewingStand).getInventory().getIngredient();
            ItemStack[] contents = ((BrewingStand) brewingStand).getInventory().getContents();
            brewingStand.update();
            ((BrewingStand) brewingStand).getInventory().setIngredient(ingredient);
            ((BrewingStand) brewingStand).getInventory().setContents(contents);
            scheduleUpdate(((BrewingStand) brewingStand).getInventory());
        }

        DecentHologramsPlugin plugin = (DecentHologramsPlugin) player.getServer().getPluginManager().getPlugin("DecentHolograms");
        if (plugin != null) {
            DHAPI.setHologramLines(inRefining, Collections.singletonList(String.format("§d§l炼制中%.2f%%", (1 - brewTimer / totalTime) * 100.0)));
        }
        brewTimer -= brewSpeed;

        // Vanilla potion brewing completes when BrewingTime == 1
        if (brewTimer < Math.max(brewSpeed, 2)) {
            this.cancel();
            finish();
        } else {
            ((BrewingStand) brewingStand).setBrewingTime((int) brewTimer);
        }
    }

    private void finish() {
        McMMOPlayerBrewEvent event = new McMMOPlayerBrewEvent(player, brewingStand);
        mcMMO.p.getServer().getPluginManager().callEvent(event);

        if (!event.isCancelled()) {
            AlchemyPotionBrewer.finishBrewing(brewingStand, player, false);
        }

        Alchemy.brewingStandMap.remove(location);
    }

    public void finishImmediately() {
        this.cancel();

        AlchemyPotionBrewer.finishBrewing(brewingStand, player, true);
        Alchemy.brewingStandMap.remove(location);
    }

    public void cancelBrew() {
        this.cancel();

        ((BrewingStand) brewingStand).setBrewingTime(-1);
        Alchemy.brewingStandMap.remove(location);
    }
}
