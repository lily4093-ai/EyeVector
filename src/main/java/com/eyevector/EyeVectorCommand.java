package com.eyevector;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.network.chat.Component;

public class EyeVectorCommand {
	public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandBuildContext registryAccess) {
		dispatcher.register(ClientCommands.literal("eyevector")
			.then(ClientCommands.literal("mode")
				.then(ClientCommands.argument("count", IntegerArgumentType.integer(2, 3))
					.suggests((context, builder) -> {
						builder.suggest(2, Component.translatable("eyevector.suggest.mode.2"));
						builder.suggest(3, Component.translatable("eyevector.suggest.mode.3"));
						return builder.buildFuture();
					})
					.executes(context -> {
						int count = IntegerArgumentType.getInteger(context, "count");
						EyeVectorClient.setMeasurementMode(count);
						context.getSource().sendFeedback(
							Component.translatable("eyevector.mode.set", count)
						);
						return 1;
					})
				)
			)
			.then(ClientCommands.literal("distance")
				.then(ClientCommands.argument("blocks", IntegerArgumentType.integer(16, 500))
					.suggests((context, builder) -> {
						builder.suggest(16, Component.translatable("eyevector.suggest.distance.16"));
						builder.suggest(30, Component.translatable("eyevector.suggest.distance.30"));
						builder.suggest(50, Component.translatable("eyevector.suggest.distance.50"));
						builder.suggest(100, Component.translatable("eyevector.suggest.distance.100"));
						builder.suggest(200, Component.translatable("eyevector.suggest.distance.200"));
						return builder.buildFuture();
					})
					.executes(context -> {
						int distance = IntegerArgumentType.getInteger(context, "blocks");
						EyeVectorClient.setMinDistance(distance);
						context.getSource().sendFeedback(
							Component.translatable("eyevector.distance.set", distance)
						);
						return 1;
					})
				)
			)
			.then(ClientCommands.literal("precision")
				.then(ClientCommands.literal("low")
					.executes(context -> {
						EyeVectorClient.setPrecision(EyeVectorClient.Precision.LOW);
						context.getSource().sendFeedback(
							Component.translatable("eyevector.precision.low")
						);
						return 1;
					})
				)
				.then(ClientCommands.literal("medium")
					.executes(context -> {
						EyeVectorClient.setPrecision(EyeVectorClient.Precision.MEDIUM);
						context.getSource().sendFeedback(
							Component.translatable("eyevector.precision.medium")
						);
						return 1;
					})
				)
				.then(ClientCommands.literal("high")
					.executes(context -> {
						EyeVectorClient.setPrecision(EyeVectorClient.Precision.HIGH);
						context.getSource().sendFeedback(
							Component.translatable("eyevector.precision.high")
						);
						return 1;
					})
				)
			)
			.then(ClientCommands.literal("reset")
				.executes(context -> {
					EyeVectorClient.reset();
					context.getSource().sendFeedback(
						Component.translatable("eyevector.reset")
					);
					return 1;
				})
			)
			.then(ClientCommands.literal("status")
				.executes(context -> {
					int mode = EyeVectorClient.getMeasurementMode();
					int recorded = EyeVectorClient.getRecordedCount();
					int minDist = EyeVectorClient.getMinDistance();
					String precisionKey = EyeVectorClient.getPrecision().getTranslationKey();

					context.getSource().sendFeedback(
						Component.translatable("eyevector.status.header")
					);
					context.getSource().sendFeedback(
						Component.translatable("eyevector.status.mode", mode, recorded, mode)
					);
					context.getSource().sendFeedback(
						Component.translatable("eyevector.status.config", minDist, Component.translatable(precisionKey))
					);
					return 1;
				})
			)
			.executes(context -> {
				context.getSource().sendFeedback(
					Component.translatable("eyevector.help.header")
				);
				context.getSource().sendFeedback(
					Component.translatable("eyevector.help.mode")
				);
				context.getSource().sendFeedback(
					Component.translatable("eyevector.help.distance")
				);
				context.getSource().sendFeedback(
					Component.translatable("eyevector.help.precision")
				);
				context.getSource().sendFeedback(
					Component.translatable("eyevector.help.reset")
				);
				context.getSource().sendFeedback(
					Component.translatable("eyevector.help.status")
				);
				return 1;
			})
		);
	}
}
