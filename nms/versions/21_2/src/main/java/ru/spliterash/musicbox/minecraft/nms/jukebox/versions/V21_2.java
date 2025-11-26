package ru.spliterash.musicbox.minecraft.nms.jukebox.versions;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Jukebox;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import ru.spliterash.musicbox.minecraft.nms.jukebox.IJukebox;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 1.21.2+ 向け。NMSアクセスが壊れた場合でも、バニラ音を鳴らさずにディスクを保持するためのフォールバックを実装する。
 */
public class V21_2 implements IJukebox {
    private static final ConcurrentMap<Location, ItemStack> FALLBACK_STORE = new ConcurrentHashMap<>();

    private final Object tileEntity;
    private final Method setSongWithoutPlay;
    private final Method getItem;
    private final boolean useFallbackStore;
    private final Jukebox bukkitJukebox;
    private final Location location;

    public V21_2(Jukebox jukebox) {
        Object tmpTile = null;
        Method tmpSet = null;
        Method tmpGet = null;
        boolean tmpFallback = false;
        this.bukkitJukebox = jukebox;
        this.location = jukebox.getLocation();
        try {
            Class<?> craftClass = tryLoad("org.bukkit.craftbukkit.block.CraftJukebox")
                    .orElseGet(() -> {
                        String version = getServerVersion();
                        try {
                            return load("org.bukkit.craftbukkit." + version + ".block.CraftJukebox");
                        } catch (ClassNotFoundException e) {
                            throw new RuntimeException(e);
                        }
                    });

            Object craft = craftClass.cast(jukebox);

            // CraftJukebox や親クラスから getTileEntity/tileEntity を探す
            Method getTile = findMethodInHierarchy(craftClass, "getTileEntity");
            Object tile;
            if (getTile != null) {
                getTile.setAccessible(true);
                tile = getTile.invoke(craft);
            } else {
                Field field = findFieldInHierarchy(craftClass, "tileEntity");
                field.setAccessible(true);
                tile = field.get(craft);
            }

            tmpTile = tile;
            Class<?> tileClass = tmpTile.getClass();
            Class<?> nmsItemClass = load("net.minecraft.world.item.ItemStack");
            tmpSet = tileClass.getMethod("setSongItemWithoutPlaying", nmsItemClass, int.class);
            tmpGet = tileClass.getMethod("getItem", int.class);
        } catch (Exception e) {
            // NMS経路が完全に壊れている場合は独自ストアで管理し、バニラ音を止める
            stopVanilla(bukkitJukebox);
            bukkitJukebox.setRecord(null);
            bukkitJukebox.update();
            tmpFallback = true;
        }

        this.tileEntity = tmpTile;
        this.setSongWithoutPlay = tmpSet;
        this.getItem = tmpGet;
        this.useFallbackStore = tmpFallback;
    }

    @Override
    public void setJukebox(ItemStack item) {
        if (useFallbackStore) {
            // バニラ音を鳴らさないため、実ジュークボックスには挿さないでメモリに保持
            stopVanilla(bukkitJukebox);
            if (location != null) {
                if (item == null) {
                    FALLBACK_STORE.remove(location);
                } else {
                    FALLBACK_STORE.put(location, item.clone());
                }
            }
            return;
        }
        try {
            Object nmsItem = CraftItemStack.asNMSCopy(item);
            setSongWithoutPlay.invoke(tileEntity, nmsItem, 0);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ItemStack getJukebox() {
        if (useFallbackStore) {
            ItemStack stored = FALLBACK_STORE.get(location);
            return stored == null ? null : stored.clone();
        }
        try {
            Object nmsItem = getItem.invoke(tileEntity, 0);
            if (nmsItem == null)
                return null;

            Method isEmpty = nmsItem.getClass().getMethod("isEmpty");
            boolean empty = (boolean) isEmpty.invoke(nmsItem);
            if (empty)
                return null;

            return CraftItemStack.asBukkitCopy((net.minecraft.world.item.ItemStack) nmsItem);
        } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getServerVersion() {
        String pkg = Bukkit.getServer().getClass().getPackage().getName();
        return pkg.substring(pkg.lastIndexOf('.') + 1);
    }

    private static Optional<Class<?>> tryLoad(String cls) {
        try {
            return Optional.of(Class.forName(cls));
        } catch (ClassNotFoundException e) {
            return Optional.empty();
        }
    }

    private static Class<?> load(String cls) throws ClassNotFoundException {
        return Class.forName(cls);
    }

    private static Method findMethodInHierarchy(Class<?> clazz, String name) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredMethod(name);
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static Field findFieldInHierarchy(Class<?> clazz, String name) throws NoSuchFieldException {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    private void stopVanilla(Jukebox jukebox) {
        try {
            Method stop = jukebox.getClass().getMethod("stopPlaying");
            stop.invoke(jukebox);
        } catch (Exception ignore) {
            try {
                Method setPlaying = jukebox.getClass().getMethod("setPlaying", boolean.class);
                setPlaying.invoke(jukebox, false);
            } catch (Exception ignored) {
                // 最終手段で空にする
                jukebox.setRecord(null);
                jukebox.update();
            }
        }
    }
}
