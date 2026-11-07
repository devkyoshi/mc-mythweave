package com.mythweave.command;

import com.mythweave.entity.base.MythlingEntity;
import com.mythweave.network.OpenJournalPayload;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * The journal command for everyone, plus testing/admin commands for operators only
 * (same permission as /kill):
 *
 * <pre>
 * /mythweave journal
 * /mythweave kindle get &lt;mythling&gt;
 * /mythweave kindle add &lt;mythlings&gt; &lt;amount&gt;
 * </pre>
 *
 * Example: {@code /mythweave kindle add @e[type=mythweave:cindercub,distance=..5] 50}
 */
public final class MythweaveCommands {
	private MythweaveCommands() {
	}

	/** Builds the command tree; called by {@link com.mythweave.event.ModEvents} when commands register. */
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(
				Commands.literal("mythweave")
						.then(Commands.literal("journal")
								.executes(MythweaveCommands::journal))
						.then(Commands.literal("kindle")
								.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
								.then(Commands.literal("get")
										.then(Commands.argument("target", EntityArgument.entity())
												.executes(MythweaveCommands::get)))
								.then(Commands.literal("add")
										.then(Commands.argument("targets", EntityArgument.entities())
												.then(addAmount())))));
	}

	private static int journal(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		if (!ServerPlayNetworking.canSend(player, OpenJournalPayload.TYPE)) {
			context.getSource().sendFailure(Component.translatable("commands.mythweave.journal.client_only"));
			return 0;
		}
		ServerPlayNetworking.send(player, OpenJournalPayload.INSTANCE);
		return 1;
	}

	private static ArgumentBuilder<CommandSourceStack, ?> addAmount() {
		return Commands.argument("amount", IntegerArgumentType.integer(1, 1000))
				.executes(MythweaveCommands::add);
	}

	private static int get(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		Entity entity = EntityArgument.getEntity(context, "target");
		if (!(entity instanceof MythlingEntity mythling)) {
			context.getSource().sendFailure(Component.translatable("commands.mythweave.not_a_mythling"));
			return 0;
		}
		context.getSource().sendSuccess(() -> mythling.kindle().status(mythling), false);
		return mythling.kindle().points();
	}

	private static int add(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		int amount = IntegerArgumentType.getInteger(context, "amount");
		int changed = 0;
		for (Entity entity : EntityArgument.getEntities(context, "targets")) {
			if (entity instanceof MythlingEntity mythling && mythling.isTame()) {
				mythling.kindle().add(mythling, amount);
				context.getSource().sendSuccess(() -> mythling.kindle().status(mythling), true);
				changed++;
			}
		}
		if (changed == 0) {
			context.getSource().sendFailure(Component.translatable("commands.mythweave.no_bonded_mythling"));
		}
		return changed;
	}
}
