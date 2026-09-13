package com.eyevector;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class EyeVectorCommand {
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess) {
		dispatcher.register(Commands.literal("eyevector")
			.then(Commands.literal("mode")
				.then(Commands.argument("count", IntegerArgumentType.integer(2, 3))
					.suggests((context, builder) -> {
						builder.suggest(2, Component.translatable("eyevector.suggest.mode.2"));
						builder.suggest(3, Component.translatable("eyevector.suggest.mode.3"));
						return builder.buildFuture();
					})
					.executes(context -> {
						int count = IntegerArgumentType.getInteger(context, "count");
						EyeVectorMod.setMeasurementMode(count);
						context.getSource().sendSuccess(
							() -> Component.translatable("eyevector.mode.set", count), false
						);
						return 1;
					})
				)
			)
			.then(Commands.literal("distance")
				.then(Commands.argument("blocks", IntegerArgumentType.integer(16, 500))
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
						EyeVectorMod.setMinDistance(distance);
						context.getSource().sendSuccess(
							() -> Component.translatable("eyevector.distance.set", distance), false
						);
						return 1;
					})
				)
			)
			.then(Commands.literal("precision")
				.then(Commands.literal("low")
					.executes(context -> {
						EyeVectorMod.setPrecision(EyeVectorMod.Precision.LOW);
						context.getSource().sendSuccess(
							() -> Component.translatable("eyevector.precision.low"), false
						);
						return 1;
					})
				)
				.then(Commands.literal("medium")
					.executes(context -> {
						EyeVectorMod.setPrecision(EyeVectorMod.Precision.MEDIUM);
						context.getSource().sendSuccess(
							() -> Component.translatable("eyevector.precision.medium"), false
						);
						return 1;
					})
				)
				.then(Commands.literal("high")
					.executes(context -> {
						EyeVectorMod.setPrecision(EyeVectorMod.Precision.HIGH);
						context.getSource().sendSuccess(
							() -> Component.translatable("eyevector.precision.high"), false
						);
						return 1;
					})
				)
			)
			.then(Commands.literal("reset")
				.executes(context -> {
					EyeVectorMod.reset();
					context.getSource().sendSuccess(
						() -> Component.translatable("eyevector.reset"), false
					);
					return 1;
				})
			)
			.then(Commands.literal("status")
				.executes(context -> {
					int mode = EyeVectorMod.getMeasurementMode();
					int recorded = EyeVectorMod.getRecordedCount();
					int minDist = EyeVectorMod.getMinDistance();
					String precisionKey = EyeVectorMod.getPrecision().getTranslationKey();

					context.getSource().sendSuccess(
						() -> Component.translatable("eyevector.status.header"), false
					);
					context.getSource().sendSuccess(
						() -> Component.translatable("eyevector.status.mode", mode, recorded, mode), false
					);
					context.getSource().sendSuccess(
						() -> Component.translatable("eyevector.status.config", minDist, Component.translatable(precisionKey)), false
					);
					return 1;
				})
			)
			.executes(context -> {
				context.getSource().sendSuccess(
					() -> Component.translatable("eyevector.help.header"), false
				);
				context.getSource().sendSuccess(
					() -> Component.translatable("eyevector.help.mode"), false
				);
				context.getSource().sendSuccess(
					() -> Component.translatable("eyevector.help.distance"), false
				);
				context.getSource().sendSuccess(
					() -> Component.translatable("eyevector.help.precision"), false
				);
				context.getSource().sendSuccess(
					() -> Component.translatable("eyevector.help.reset"), false
				);
				context.getSource().sendSuccess(
					() -> Component.translatable("eyevector.help.status"), false
				);
				return 1;
			})
		);
	}
}
