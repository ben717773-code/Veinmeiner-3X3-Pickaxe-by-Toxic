package de.example.hammervein;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class HammerVeinPlugin extends JavaPlugin {

    private NamespacedKey hammerKey;
    private final Set<UUID> veinDisabled = new HashSet<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        hammerKey = new NamespacedKey(this, "hammer");

        getServer().getPluginManager().registerEvents(new MiningListener(this), this);
        registerRecipe();
    }

    private void registerRecipe() {
        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(this, "hammer_pickaxe"), createHammer());
        recipe.shape("DDD", " N ", " S ");
        recipe.setIngredient('D', Material.DIAMOND_BLOCK);
        recipe.setIngredient('N', Material.NETHERITE_PICKAXE);
        recipe.setIngredient('S', Material.NETHERITE_INGOT);
        getServer().addRecipe(recipe);
    }

    public ItemStack createHammer() {
        ItemStack item = new ItemStack(Material.NETHERITE_PICKAXE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("3x3 Spitzhacke", NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Baut einen 3x3-Bereich ab.", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("Schleichen = normales Abbauen / Veinminer", NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false)));
        meta.getPersistentDataContainer().set(hammerKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isHammer(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(hammerKey, PersistentDataType.BYTE);
    }

    public boolean isVeinEnabled(UUID id) {
        return !veinDisabled.contains(id);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Nur Spieler koennen diesen Befehl nutzen.");
            return true;
        }
        switch (command.getName().toLowerCase()) {
            case "hammer" -> {
                player.getInventory().addItem(createHammer());
                player.sendMessage(Component.text("Du hast die 3x3 Spitzhacke erhalten.", NamedTextColor.GREEN));
            }
            case "veinminer" -> {
                if (veinDisabled.remove(player.getUniqueId())) {
                    player.sendMessage(Component.text("Veinminer: AN", NamedTextColor.GREEN));
                } else {
                    veinDisabled.add(player.getUniqueId());
                    player.sendMessage(Component.text("Veinminer: AUS", NamedTextColor.RED));
                }
            }
        }
        return true;
    }
}
