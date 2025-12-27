package com.eyevector;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;

import java.util.concurrent.CompletableFuture;

public class EyeVectorCommand {
	public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
		dispatcher.register(ClientCommandManager.literal("eyevector")
			.then(ClientCommandManager.literal("mode")
				.then(ClientCommandManager.argument("count", IntegerArgumentType.integer(2, 3))
					.suggests((context, builder) -> {
						builder.suggest(2, Text.translatable("eyevector.suggest.mode.2"));
						builder.suggest(3, Text.translatable("eyevector.suggest.mode.3"));
						return builder.buildFuture();
					})
					.executes(context -> {
						int count = IntegerArgumentType.getInteger(context, "count");
						EyeVectorClient.setMeasurementMode(count);
						context.getSource().sendFeedback(
							Text.translatable("eyevector.mode.set", count)
						);
						return 1;
					})
				)
			)
			.then(ClientCommandManager.literal("distance")
				.then(ClientCommandManager.argument("blocks", IntegerArgumentType.integer(16, 500))
					.suggests((context, builder) -> {
						builder.suggest(16, Text.translatable("eyevector.suggest.distance.16"));
						builder.suggest(30, Text.translatable("eyevector.suggest.distance.30"));
						builder.suggest(50, Text.translatable("eyevector.suggest.distance.50"));
						builder.suggest(100, Text.translatable("eyevector.suggest.distance.100"));
						builder.suggest(200, Text.translatable("eyevector.suggest.distance.200"));
						return builder.buildFuture();
					})
					.executes(context -> {
						int distance = IntegerArgumentType.getInteger(context, "blocks");
						EyeVectorClient.setMinDistance(distance);
						context.getSource().sendFeedback(
							Text.translatable("eyevector.distance.set", distance)
						);
						return 1;
					})
				)
			)
			.then(ClientCommandManager.literal("precision")
				.then(ClientCommandManager.literal("low")
					.executes(context -> {
						EyeVectorClient.setPrecision(EyeVectorClient.Precision.LOW);
						context.getSource().sendFeedback(
							Text.translatable("eyevector.precision.low")
						);
						return 1;
					})
				)
				.then(ClientCommandManager.literal("medium")
					.executes(context -> {
						EyeVectorClient.setPrecision(EyeVectorClient.Precision.MEDIUM);
						context.getSource().sendFeedback(
							Text.translatable("eyevector.precision.medium")
						);
						return 1;
					})
				)
				.then(ClientCommandManager.literal("high")
					.executes(context -> {
						EyeVectorClient.setPrecision(EyeVectorClient.Precision.HIGH);
						context.getSource().sendFeedback(
							Text.translatable("eyevector.precision.high")
						);
						return 1;
					})
				)
			)
			.then(ClientCommandManager.literal("reset")
				.executes(context -> {
					EyeVectorClient.reset();
					context.getSource().sendFeedback(
						Text.translatable("eyevector.reset")
					);
					return 1;
				})
			)
			.then(ClientCommandManager.literal("status")
				.executes(context -> {
					int mode = EyeVectorClient.getMeasurementMode();
					int recorded = EyeVectorClient.getRecordedCount();
					int minDist = EyeVectorClient.getMinDistance();
					String precisionKey = EyeVectorClient.getPrecision().getTranslationKey();

					context.getSource().sendFeedback(
						Text.translatable("eyevector.status.header")
					);
					context.getSource().sendFeedback(
						Text.translatable("eyevector.status.mode", mode, recorded, mode)
					);
					context.getSource().sendFeedback(
						Text.translatable("eyevector.status.config", minDist, Text.translatable(precisionKey))
					);
					return 1;
				})
			)
			.executes(context -> {
				context.getSource().sendFeedback(
					Text.translatable("eyevector.help.header")
				);
				context.getSource().sendFeedback(
					Text.translatable("eyevector.help.mode")
				);
				context.getSource().sendFeedback(
					Text.translatable("eyevector.help.distance")
				);
				context.getSource().sendFeedback(
					Text.translatable("eyevector.help.precision")
				);
				context.getSource().sendFeedback(
					Text.translatable("eyevector.help.reset")
				);
				context.getSource().sendFeedback(
					Text.translatable("eyevector.help.status")
				);
				return 1;
			})
		);
	}
}
