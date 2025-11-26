package ru.spliterash.musicbox.customPlayers.objects.jukebox;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Jukebox;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import ru.spliterash.musicbox.Lang;
import ru.spliterash.musicbox.MusicBox;
import ru.spliterash.musicbox.customPlayers.abstracts.AbstractBlockPlayer;
import ru.spliterash.musicbox.customPlayers.interfaces.IPlayList;
import ru.spliterash.musicbox.customPlayers.models.MusicBoxSongPlayerModel;
import ru.spliterash.musicbox.minecraft.nms.jukebox.JukeboxFactory;
import ru.spliterash.musicbox.minecraft.nms.jukebox.IJukebox;
import ru.spliterash.musicbox.song.MusicBoxSong;
import ru.spliterash.musicbox.song.MusicBoxSongManager;
import ru.spliterash.musicbox.utils.BukkitUtils;
import ru.spliterash.musicbox.utils.SignUtils;

import java.util.Collection;

@Getter
public class JukeboxPlayer extends AbstractBlockPlayer {
    private Location infoSign;

    private JukeboxPlayer(IPlayList list, int range, Jukebox box) {
        super(list, box.getLocation(), range);
        SignUtils
                .findSign(box.getLocation())
                .ifPresent(s -> {
                    infoSign = s.getLocation();
                    SignUtils.setPlayListInfo(infoSign, list);
                });
    }

    public static void onJukeboxClick(Jukebox jukebox, ItemStack clickedItem, PlayerInteractEvent e) {
        JukeboxPlayer sp = AbstractBlockPlayer.findByLocation(jukebox.getLocation());
        IJukebox handler = JukeboxFactory.getJukebox(jukebox);
        if (clickedItem == null) {
            // 手に何も持っていない=停止/取り出し。現在のディスクを返却し、再生を止める。
            ejectAndStop(jukebox, handler, true);
            if (sp != null) {
                // 再生を確実に止めてからプレイヤーを破棄
                sp.getMusicBoxModel().getMusicBoxSongPlayer().getApiPlayer().destroy();
                sp.destroy();
            }
            e.setCancelled(true);
            return;
        }
        ItemStack current = handler.getJukebox();
        MusicBoxSong song = MusicBoxSongManager.findByItem(clickedItem).orElse(null);
        if (song == null) {
            return;
        }
        if (sp != null) {
            sp.destroy();
        }
        if (JukeboxFactory.jukeboxAvailable()) {
            e.setCancelled(true);
            e.getPlayer().getInventory().setItemInMainHand(null);
            // 以前のディスクが入っていた場合は返却する
            if (current != null && current.getType() != Material.AIR) {
                jukebox.getWorld().dropItem(jukebox.getLocation().add(0.5, 0.5, 0.5), current);
            }
            handler.setJukebox(clickedItem);
            createNew(jukebox);
        } else {
            e.getPlayer().sendMessage(Lang.JUKEBOX_NOT_SUPPORTED.toString());
        }
    }

    private static void createNew(Jukebox jukebox) {
        try {
            JukeboxPlaylistImpl playlist = new JukeboxPlaylistImpl(jukebox.getLocation());
            new JukeboxPlayer(playlist, MusicBox.getInstance().getConfigObject().getJukeboxRadius(), jukebox);
        } catch (JukeboxPlaylistInitException e) {
            // NOTHING
        }
    }

    /**
     * Вызывается когда игрок кликает с зажатым шифтом
     */
    public static void onSneakingClick(Jukebox jukebox, Player player) {
        JukeboxPlayer songPlayer = AbstractBlockPlayer.findByLocation(jukebox.getLocation());
        if (songPlayer != null) {
            songPlayer.getControl().open(player);
        }
    }

    @Override
    public void playTick(Player player, int tick) {
        super.playTick(player, tick);
        spawnBeatParticle(tick);
    }

    private void spawnBeatParticle(int tick) {
        var noteOpt = song
                .getLayerHashMap()
                .values()
                .stream()
                .map(layer -> layer.getNote(tick))
                .filter(java.util.Objects::nonNull)
                .findFirst();
        if (noteOpt.isEmpty()) {
            return;
        }
        float color = (noteOpt.get().getKey() % 24) / 24.0f;
        getLocation().getWorld().spawnParticle(
                org.bukkit.Particle.NOTE,
                getTargetLocation().clone().add(0.0, 1.2, 0.0),
                1,
                0.0,
                0.0,
                0.0,
                color
        );
    }

public static void onRedstone(Jukebox box, Block source, int power) {
        // バニラディスクが入っている場合はバニラ挙動に任せる
        ItemStack record = box.getRecord();
        if (record != null && record.getType() != Material.AIR
                && !MusicBoxSongManager.findByItem(record).isPresent()) {
            return;
        }

        if (power > 0) {
            JukeboxPlayer player = AbstractBlockPlayer.findByLocation(box.getLocation());
            if (player != null) {
                player.getMusicBoxModel().startNext();
            } else if (JukeboxFactory.jukeboxAvailable())
                createNew(box);
        }
    }

    @Override
    protected void every100MillisAsync() {
        BukkitUtils.runSyncTask(() -> {
            Block b = getTargetLocation().getBlock();
            if (!(b.getState() instanceof Jukebox))
                destroy();
        });
    }

    @Override
    protected JukeboxPlayer runNextSong(IPlayList list) {
        @NotNull BlockState state = getTargetLocation().getBlock().getState();
        if (state instanceof Jukebox) {
            return new JukeboxPlayer(list, getRange(), (Jukebox) state);
        } else
            return null;
    }

    @Override
    protected void songEnd() {
        // NOTHING
    }

    private static void ejectAndStop(Jukebox jukebox, IJukebox handler, boolean drop) {
        ItemStack current = null;
        try {
            current = handler.getJukebox();
        } catch (Throwable ignored) {
            current = jukebox.getRecord(); // フォールバック
        }

        if (drop && current != null && current.getType() != Material.AIR) {
            jukebox.getWorld().dropItem(jukebox.getLocation().add(0.5, 0.5, 0.5), current);
        }
        try {
            handler.setJukebox(null); // NMS / フォールバックストアを空にする
        } catch (Throwable ignored) {
            // ignore
        }
        try {
            jukebox.stopPlaying();
        } catch (Throwable ignored) {
            try {
                jukebox.setPlaying((Material) null);
            } catch (Throwable ignored2) {
                // ignore
            }
        }
        jukebox.setRecord(null);
        jukebox.update(true);
    }

    public void ejectRecord(boolean drop) {
        BlockState state = getTargetLocation().getBlock().getState();
        if (state instanceof Jukebox) {
            IJukebox handler = JukeboxFactory.getJukebox((Jukebox) state);
            ejectAndStop((Jukebox) state, handler, drop);
        }
    }

    public static void ejectAllRecords(boolean drop) {
        Collection<AbstractBlockPlayer> all = AbstractBlockPlayer.getAll();
        for (AbstractBlockPlayer bp : all) {
            if (bp instanceof JukeboxPlayer) {
                ((JukeboxPlayer) bp).ejectRecord(drop);
            }
        }
    }
}
