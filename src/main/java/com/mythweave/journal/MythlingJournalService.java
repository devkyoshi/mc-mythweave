package com.mythweave.journal;

import com.mythweave.entity.base.MythlingEntity;
import com.mythweave.evolution.EvolutionLine;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Server-side keeper of every player's journal.
 *
 * <p>Discovery is authoritative and per-player: getting near a Mythling unlocks its form.
 * Tamed companions are tracked as small snapshots, so the journal can list them even while
 * their chunk is unloaded. The whole journal lives in a player attachment, which Fabric
 * persists and syncs only to that player.
 */
public final class MythlingJournalService {
	/** How close a player must get to a wild Mythling to discover it. */
	private static final double DISCOVERY_RADIUS = 24.0;
	/** How often the server scans for discoveries and refreshes tamed snapshots. */
	private static final int SCAN_INTERVAL_TICKS = 20;
	/** Journal order: stage, then name, then form id, so the list is stable between refreshes. */
	private static final Comparator<TamedMythling> TAMED_ORDER = Comparator
			.comparingInt((TamedMythling mythling) -> MythlingCatalog.stageOf(mythling.formId()))
			.thenComparing(TamedMythling::name)
			.thenComparing(TamedMythling::formId);

	private MythlingJournalService() {
	}

	public static JournalData data(ServerPlayer player) {
		return player.getAttachedOrCreate(JournalAttachments.JOURNAL);
	}

	/** Called once a second from {@link JournalEvents}. */
	public static void tick(MinecraftServer server) {
		if (server.getTickCount() % SCAN_INTERVAL_TICKS != 0) {
			return;
		}
		if (server.getPlayerList().getPlayers().isEmpty()) {
			return;
		}

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			discoverNearby(player);
		}
		refreshTamed(server);
	}

	private static void discoverNearby(ServerPlayer player) {
		ServerLevel level = player.level();
		double radiusSqr = DISCOVERY_RADIUS * DISCOVERY_RADIUS;
		for (MythlingEntity mythling : level.getEntitiesOfClass(MythlingEntity.class,
				player.getBoundingBox().inflate(DISCOVERY_RADIUS))) {
			// The AABB is a cube, so check the real distance to keep the promise of 24 blocks.
			if (player.distanceToSqr(mythling) <= radiusSqr) {
				markDiscovered(player, mythling.formId());
			}
		}
	}

	/** Unlocks a form and tells the player, once. */
	public static void markDiscovered(ServerPlayer player, String formId) {
		JournalData data = data(player);
		if (data.isDiscovered(formId)) {
			return;
		}
		player.setAttached(JournalAttachments.JOURNAL, data.withDiscovered(formId));
		player.sendOverlayMessage(Component.translatable("message.mythweave.journal.discovered",
				EvolutionLine.formName(formId)));
	}

	/** Called right after a wild Mythling bonds with a player. */
	public static void onBonded(MythlingEntity mythling, Player player) {
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return;
		}
		markDiscovered(serverPlayer, mythling.formId());
		upsertTamed(serverPlayer, snapshot(mythling));
	}

	/** Called when an old form is replaced by its evolved form. */
	public static void onEvolved(MythlingEntity oldForm, MythlingEntity newForm) {
		if (!(newForm.getOwner() instanceof ServerPlayer owner)) {
			return;
		}
		markDiscovered(owner, newForm.formId());
		JournalData data = data(owner);
		List<TamedMythling> tamed = new ArrayList<>(data.tamed());
		tamed.removeIf(mythling -> mythling.id().equals(oldForm.getUUID()));
		tamed.removeIf(mythling -> mythling.id().equals(newForm.getUUID()));
		tamed.add(snapshot(newForm));
		setTamed(owner, data, tamed);
	}

	/** Called when a bonded Mythling dies; its journal entry is removed. */
	public static void onDeath(MythlingEntity mythling) {
		if (!(mythling.getOwner() instanceof ServerPlayer owner)) {
			return;
		}
		JournalData data = data(owner);
		List<TamedMythling> tamed = data.tamed().stream()
				.filter(entry -> !entry.id().equals(mythling.getUUID()))
				.toList();
		if (tamed.size() != data.tamed().size()) {
			setTamed(owner, data, tamed);
		}
	}

	/** Rebuilds each online player's tamed list from the entities that are currently loaded. */
	private static void refreshTamed(MinecraftServer server) {
		Map<UUID, List<TamedMythling>> loadedByOwner = new HashMap<>();
		for (ServerLevel level : server.getAllLevels()) {
			for (Entity entity : level.getAllEntities()) {
				if (entity instanceof MythlingEntity mythling
						&& mythling.isAlive()
						&& mythling.isTame()
						&& mythling.getOwner() instanceof ServerPlayer owner) {
					loadedByOwner.computeIfAbsent(owner.getUUID(), ignored -> new ArrayList<>())
							.add(snapshot(mythling));
				}
			}
		}

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			JournalData data = data(player);
			Map<UUID, TamedMythling> merged = new LinkedHashMap<>();
			for (TamedMythling mythling : data.tamed()) {
				merged.put(mythling.id(), mythling);
			}
			for (TamedMythling mythling : loadedByOwner.getOrDefault(player.getUUID(), List.of())) {
				merged.put(mythling.id(), mythling);
			}
			List<TamedMythling> tamed = new ArrayList<>(merged.values());
			tamed.sort(TAMED_ORDER);
			JournalData updated = data.withTamed(tamed);
			if (!updated.equals(data)) {
				player.setAttached(JournalAttachments.JOURNAL, updated);
			}
		}
	}

	private static void upsertTamed(ServerPlayer player, TamedMythling mythling) {
		JournalData data = data(player);
		List<TamedMythling> tamed = new ArrayList<>(data.tamed());
		tamed.removeIf(existing -> existing.id().equals(mythling.id()));
		tamed.add(mythling);
		setTamed(player, data, tamed);
	}

	private static void setTamed(ServerPlayer player, JournalData data, List<TamedMythling> tamed) {
		List<TamedMythling> sorted = new ArrayList<>(tamed);
		sorted.sort(TAMED_ORDER);
		JournalData updated = data.withTamed(sorted);
		if (!updated.equals(data)) {
			player.setAttached(JournalAttachments.JOURNAL, updated);
		}
	}

	private static TamedMythling snapshot(MythlingEntity mythling) {
		return new TamedMythling(
				mythling.getUUID(),
				mythling.formId(),
				mythling.hasCustomName() && mythling.getCustomName() != null
						? mythling.getCustomName().getString()
						: "",
				mythling.kindle().points(),
				mythling.kindle().combat(),
				mythling.kindle().exploration(),
				mythling.isOrderedToSit(),
				mythling.isEvolving(),
				mythling.getHealth(),
				mythling.getMaxHealth()
		);
	}
}
