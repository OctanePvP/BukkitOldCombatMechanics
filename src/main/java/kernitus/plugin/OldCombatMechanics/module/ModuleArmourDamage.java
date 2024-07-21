package kernitus.plugin.OldCombatMechanics.module;

import kernitus.plugin.OldCombatMechanics.OCMMain;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ModuleArmourDamage extends OCMModule{


    private int cooldown;
    private Map<Material, Integer> customDamages;
    private OCMMain plugin;

    /**
     * Creates a new module.
     *
     * @param plugin     the plugin instance
     */
    public ModuleArmourDamage(OCMMain plugin) {
        super(plugin, "armour-durability-damage");
        this.plugin = plugin;
        customDamages = new HashMap<>();
        lastHits = new HashMap<>();
        reload();

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            customDamages.clear();
        },20*60*15, 20*60*15);
    }

    @Override
    public void reload() {
        cooldown = module().getInt("cooldown");

        customDamages.clear();
        lastHits.clear();
        ConfigurationSection section = module().getConfigurationSection("custom-damages");
        if (section != null)
            for (String key : section.getKeys(false)) {
                Material material = Material.getMaterial(key);
                if (material == null) continue;
                customDamages.put(material, section.getInt(key));
            }
    }

    private Map<Integer, Long> lastHits;
    @EventHandler (priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onArmorDamage(PlayerItemDamageEvent e) {
        debug("§eItemDamageEvent");

        int hash = Objects.hash(e.getPlayer().getUniqueId(), e.getItem().getI18NDisplayName(), e.getItem().getType());
        long lastHitTick = lastHits.getOrDefault(hash, 0L);
        long tick = plugin.getServer().getCurrentTick();

        debug("§elast hit tick: " + lastHitTick);
        debug("§ecurrent tick: " + tick);

        if (tick - lastHitTick < cooldown) {
            e.setCancelled(true);
            debug("§ehasnt been long enough - protected");
            return;
        }
        // do damage
        lastHits.put(hash, tick);
        debug("Doing damage, tick: " + tick);
        if (e.getDamage() == 0) return;


//        int level = e.getItem().getEnchantmentLevel(Enchantment.DURABILITY);
//        boolean hit = Math.random() < (1.0 / (level + 1));
//
//        debug("§eunbreaking level: " + level);
//        debug("§eunbreaking protected: " + !hit);
//
//
//        if (!hit) {
//            e.setCancelled(true);
//            return;
//        }

        EntityDamageEvent lastDamageEvent = e.getPlayer().getLastDamageCause();
        if (lastDamageEvent != null) {
            if (lastDamageEvent instanceof EntityDamageByEntityEvent entityDamageByEntityEvent) {
                Entity causingEntity = entityDamageByEntityEvent.getDamager();
                if (causingEntity instanceof Player livingEntity) {
                    ItemStack tool = livingEntity.getInventory().getItemInMainHand();
                    Integer damage = customDamages.get(tool.getType());
                    if (damage != null) {
                        debug("§eset damage: " + damage);
                        e.setDamage(damage);
                    } else
                        debug("§eNo custom damage");
                }
            }
        }
    }
}
