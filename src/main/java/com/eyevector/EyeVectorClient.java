package com.eyevector;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.projectile.EyeOfEnder;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class EyeVectorClient implements ClientModInitializer {
	private static final List<EyeThrowData> throwDataList = new ArrayList<>();
	private static final Set<UUID> trackedEyes = new HashSet<>();
	private static final List<EyeTracker> activeTrackers = new ArrayList<>();
	private static int measurementMode = 2; // 기본값: 2개 측정
	private static int minDistance = 30; // 기본값: 30블록
	private static Precision precision = Precision.MEDIUM; // 기본값: 보통

	public enum Precision {
		LOW(3, "eyevector.precision.low.name"),      // 최소 3개 샘플, 렉 감소
		MEDIUM(5, "eyevector.precision.medium.name"),   // 최소 5개 샘플, 기본값
		HIGH(10, "eyevector.precision.high.name");    // 최소 10개 샘플, 더 정확

		private final int minSamples;
		private final String translationKey;

		Precision(int minSamples, String translationKey) {
			this.minSamples = minSamples;
			this.translationKey = translationKey;
		}

		public int getMinSamples() {
			return minSamples;
		}

		public String getTranslationKey() {
			return translationKey;
		}

		@Override
		public String toString() {
			return translationKey;
		}
	}

	@Override
	public void onInitializeClient() {
		// 명령어 등록
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			EyeVectorCommand.register(dispatcher, registryAccess);
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.level != null && client.player != null) {
				// 활성 추적기 업데이트
				activeTrackers.removeIf(tracker -> {
					EyeOfEnder eye = tracker.getEye(client.level);
					if (eye == null || !eye.isAlive()) {
						// 엔더의 눈이 사라지거나 죽으면 추적 완료
						if (tracker.isValid()) {
							processCompletedTracker(client, tracker);
						}
						return true; // 리스트에서 제거
					}
					tracker.update(eye);
					return false;
				});

				// 엔더의 눈 엔티티 감지
				for (EyeOfEnder eye : client.level.getEntitiesOfClass(
						EyeOfEnder.class,
						client.player.getBoundingBox().inflate(50),
						e -> true)) {

					if (eye.tickCount == 1) { // 방금 생성된 엔더의 눈
						// 플레이어가 던진 엔더의 눈인지 확인
						Vec3 playerPos = client.player.getEyePosition();
						Vec3 eyePos = new Vec3(eye.getX(), eye.getY(), eye.getZ());
						double distance = playerPos.distanceTo(eyePos);

						if (distance < 3.0) { // 플레이어가 던진 것으로 판단
							UUID eyeId = eye.getUUID();
							if (!trackedEyes.contains(eyeId)) {
								trackedEyes.add(eyeId);
								// 새로운 추적기 시작
								activeTrackers.add(new EyeTracker(eye));
							}
						}
					}
				}

				// 오래된 추적 정보 정리
				if (trackedEyes.size() > 100) {
					trackedEyes.clear();
				}
			}
		});
	}

	private void processCompletedTracker(Minecraft client, EyeTracker tracker) {
		Vec3 startPos = tracker.getStartPosition();
		double angle = tracker.getAverageAngle();

		if (throwDataList.isEmpty()) {
			// 첫 번째 던지기
			throwDataList.add(new EyeThrowData(startPos.x, startPos.z, angle));
			client.player.sendSystemMessage(
				Component.translatable("eyevector.recorded.first", throwDataList.size(), measurementMode)
			);
		} else if (throwDataList.size() < measurementMode) {
			// 두 번째 또는 세 번째 던지기
			EyeThrowData newThrow = new EyeThrowData(startPos.x, startPos.z, angle);

			// 이전 위치들과의 거리 확인
			for (int i = 0; i < throwDataList.size(); i++) {
				EyeThrowData prevThrow = throwDataList.get(i);
				double distance = Math.sqrt(
					Math.pow(newThrow.x - prevThrow.x, 2) +
					Math.pow(newThrow.z - prevThrow.z, 2)
				);

				// 설정된 최소 거리 이상 떨어져야 함
				if (distance < minDistance) {
					client.player.sendSystemMessage(
						Component.translatable("eyevector.error.too_close", i + 1, minDistance)
					);
					return; // 기록 유지
				}
			}

			throwDataList.add(newThrow);

			if (throwDataList.size() == measurementMode) {
				// 모든 위치가 기록됨 - 계산 시작
				calculateAndDisplay(client);
			} else {
				client.player.sendSystemMessage(
					Component.translatable("eyevector.recorded.nth", throwDataList.size(), throwDataList.size(), measurementMode)
				);
			}
		}
	}

	private void calculateAndDisplay(Minecraft client) {
		Vec3 strongholdPos;

		if (measurementMode == 2) {
			strongholdPos = calculateWith2Points(throwDataList.get(0), throwDataList.get(1));
		} else {
			strongholdPos = calculateWith3Points(throwDataList.get(0), throwDataList.get(1), throwDataList.get(2));
		}

		if (strongholdPos != null) {
			int roundedX = (int) Math.round(strongholdPos.x);
			int roundedZ = (int) Math.round(strongholdPos.z);
			client.player.sendSystemMessage(
				Component.translatable("eyevector.result.location", roundedX, roundedZ)
			);
			Vec3 playerPos = new Vec3(client.player.getX(), client.player.getY(), client.player.getZ());
			double distance = Math.sqrt(
				Math.pow(strongholdPos.x - playerPos.x, 2) +
				Math.pow(strongholdPos.z - playerPos.z, 2)
			);
			int roundedDistance = (int) Math.round(distance);
			client.player.sendSystemMessage(
				Component.translatable("eyevector.result.distance", roundedDistance)
			);
			// 성공시 초기화
			throwDataList.clear();
		} else {
			client.player.sendSystemMessage(
				Component.translatable("eyevector.error.parallel")
			);
			// 실패해도 기록은 유지 (한 번만 더 던지면 됨)
		}
	}

	private Vec3 calculateWith2Points(EyeThrowData first, EyeThrowData second) {
		// 두 직선의 교점 계산
		double x1 = first.x;
		double z1 = first.z;
		double dx1 = Math.cos(first.angle);
		double dz1 = Math.sin(first.angle);

		double x2 = second.x;
		double z2 = second.z;
		double dx2 = Math.cos(second.angle);
		double dz2 = Math.sin(second.angle);

		// 평행 체크
		double cross = dx1 * dz2 - dz1 * dx2;
		if (Math.abs(cross) < 0.00001) {
			return null; // 거의 완전히 평행
		}

		// 교점 계산
		double t1 = ((x2 - x1) * dz2 - (z2 - z1) * dx2) / cross;

		double intersectX = x1 + t1 * dx1;
		double intersectZ = z1 + t1 * dz1;

		return new Vec3(intersectX, 0, intersectZ);
	}

	private Vec3 calculateWith3Points(EyeThrowData first, EyeThrowData second, EyeThrowData third) {
		// 3개의 직선으로 더 정확한 위치 계산
		// 각 두 직선의 교점을 구하고 평균을 냄
		Vec3 pos12 = calculateWith2Points(first, second);
		Vec3 pos23 = calculateWith2Points(second, third);
		Vec3 pos13 = calculateWith2Points(first, third);

		int validCount = 0;
		double sumX = 0, sumZ = 0;

		if (pos12 != null) {
			sumX += pos12.x;
			sumZ += pos12.z;
			validCount++;
		}
		if (pos23 != null) {
			sumX += pos23.x;
			sumZ += pos23.z;
			validCount++;
		}
		if (pos13 != null) {
			sumX += pos13.x;
			sumZ += pos13.z;
			validCount++;
		}

		if (validCount == 0) {
			return null; // 모든 직선이 평행
		}

		return new Vec3(sumX / validCount, 0, sumZ / validCount);
	}

	// 명령어용 메서드들
	public static void setMeasurementMode(int mode) {
		measurementMode = mode;
		throwDataList.clear(); // 모드 변경시 기록 초기화
	}

	public static int getMeasurementMode() {
		return measurementMode;
	}

	public static void setMinDistance(int distance) {
		minDistance = distance;
	}

	public static int getMinDistance() {
		return minDistance;
	}

	public static void setPrecision(Precision p) {
		precision = p;
	}

	public static Precision getPrecision() {
		return precision;
	}

	public static void reset() {
		throwDataList.clear();
	}

	public static int getRecordedCount() {
		return throwDataList.size();
	}

	private static class EyeThrowData {
		final double x;
		final double z;
		final double angle;

		EyeThrowData(double x, double z, double angle) {
			this.x = x;
			this.z = z;
			this.angle = angle;
		}
	}

	// 엔더의 눈 추적기 - 여러 틱에 걸쳐 정확한 각도 측정
	private static class EyeTracker {
		private final UUID eyeId;
		private final Vec3 startPosition;
		private final List<Double> angles = new ArrayList<>();
		private Vec3 lastPosition;
		private int tickCount = 0;

		EyeTracker(EyeOfEnder eye) {
			this.eyeId = eye.getUUID();
			this.startPosition = new Vec3(eye.getX(), eye.getY(), eye.getZ());
			this.lastPosition = startPosition;
		}

		void update(EyeOfEnder eye) {
			tickCount++;
			Vec3 currentPos = new Vec3(eye.getX(), eye.getY(), eye.getZ());

			// 2틱 이상이고, 충분히 이동했을 때만 각도 기록
			if (tickCount >= 2) {
				double dx = currentPos.x - lastPosition.x;
				double dz = currentPos.z - lastPosition.z;
				double distance = Math.sqrt(dx * dx + dz * dz);

				// 최소 0.1블록 이상 이동했을 때만 각도 계산
				if (distance > 0.1) {
					double angle = Math.atan2(dz, dx);
					angles.add(angle);
				}
			}

			lastPosition = currentPos;
		}

		EyeOfEnder getEye(net.minecraft.world.level.Level world) {
			var entities = world.getEntitiesOfClass(
				EyeOfEnder.class,
				new net.minecraft.world.phys.AABB(-30000000, -30000000, -30000000, 30000000, 30000000, 30000000),
				entity -> entity.getUUID().equals(eyeId)
			);
			return entities.isEmpty() ? null : entities.get(0);
		}

		boolean isValid() {
			// 설정된 정밀도에 따라 필요한 샘플 수가 달라짐
			return angles.size() >= precision.getMinSamples();
		}

		Vec3 getStartPosition() {
			return startPosition;
		}

		double getAverageAngle() {
			if (angles.isEmpty()) {
				return 0;
			}

			// 원형 평균을 사용하여 각도 평균 계산
			// (일반 평균은 180도 근처에서 부정확함)
			double sumSin = 0;
			double sumCos = 0;

			for (double angle : angles) {
				sumSin += Math.sin(angle);
				sumCos += Math.cos(angle);
			}

			return Math.atan2(sumSin / angles.size(), sumCos / angles.size());
		}
	}
}
