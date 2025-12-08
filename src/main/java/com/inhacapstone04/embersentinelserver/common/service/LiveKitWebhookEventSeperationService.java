package com.inhacapstone04.embersentinelserver.common.service;

import com.inhacapstone04.embersentinelserver.common.exception.CustomException;
import com.inhacapstone04.embersentinelserver.common.exception.ErrorCode;
import com.inhacapstone04.embersentinelserver.fire_event.service.FireEventWebhookService;
import com.inhacapstone04.embersentinelserver.media.service.MediaRecordCommandService;
import io.livekit.server.WebhookReceiver;
import livekit.LivekitEgress;
import livekit.LivekitModels;
import livekit.LivekitWebhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LiveKitWebhookEventSeperationService {

    private final WebhookReceiver webhookReceiver;
    private final MediaRecordCommandService mediaRecordCommandService;
    private final FireEventWebhookService fireEventWebhookService;

    public void handleWebhookEvent(String body, String authHeader) {
        try {
            // [LOG] 1. 웹훅 수신 시작 로그
            log.info(">>> [Webhook Start] Received raw body length: {}", body.length());

            // 1. 서명 검증
            LivekitWebhook.WebhookEvent event = webhookReceiver.receive(body, authHeader);
            String eventType = event.getEvent();

            // [LOG] 2. 검증 성공 및 이벤트 타입 확인
            log.info(">>> [Webhook Verified] Event Type: {}", eventType);

            // 2. 이벤트 타입별 로직 분기
            switch (eventType) {
                case "track_published":
                    log.info("--- Handling Track Published ---");
                    handleTrackPublished(event);
                    break;
                case "participant_left":
                case "participant_disconnected":
                    log.info("--- Handling Participant Disconnected ({}) ---", eventType);
                    handleParticipantDisconnected(event);
                    break;
                case "egress_ended":
                    log.info("--- Handling Egress Ended ---");
                    handleEgressEnded(event);
                    break;
                default:
                    log.info("--- Ignored Event: {} ---", eventType);
                    break;
            }
        } catch (Exception e) {
            log.error("!!! Webhook handling failed !!!", e);
            throw new CustomException(ErrorCode.LIVEKIT_SERVER_ERROR, "Webhook validation or processing failed");
        }
    }

    private void handleTrackPublished(LivekitWebhook.WebhookEvent event) {
        if (!event.hasRoom() || !event.hasParticipant() || !event.hasTrack()) {
            log.warn("Track Published event is missing room, participant, or track info.");
            return;
        }

        if (event.getTrack().getType() != LivekitModels.TrackType.VIDEO) {
            return;
        }

        fireEventWebhookService.processTrackPublished(
                event.getRoom().getName(), event.getParticipant().getIdentity());
    }

    private void handleParticipantDisconnected(LivekitWebhook.WebhookEvent event) {
        if (event.hasParticipant()) {
            String metadata = event.getParticipant().getMetadata();
            String roomName = event.getRoom().getName();
            log.info("Participant Disconnected - Room: {}, Metadata: {}", roomName, metadata);

            if (metadata != null && !metadata.isEmpty()) {
                fireEventWebhookService.processParticipantDisconnected(metadata, roomName);
                log.info("Processed Participant Disconnected logic successfully.");
            } else {
                log.warn("Participant Disconnected but Metadata is empty.");
            }
        }
    }

    // [핵심 수정] 상세 로그가 추가된 메서드
    private void handleEgressEnded(LivekitWebhook.WebhookEvent event) {
        // 1. EgressInfo 존재 여부 확인
        if (!event.hasEgressInfo()) {
            log.error(">>> [Egress Error] 'egress_ended' event received but NO EgressInfo found in payload.");
            return;
        }

        LivekitEgress.EgressInfo egressInfo = event.getEgressInfo();
        String roomName = egressInfo.getRoomName();
        String egressId = egressInfo.getEgressId();
        LivekitEgress.EgressStatus status = egressInfo.getStatus();

        // 2. 기본 정보 로그
        log.info(">>> [Egress Info] ID: {}, Room: {}, Status: {}", egressId, roomName, status);

        // 3. 상태 체크 (EGRESS_COMPLETE 여부)
        if (status == LivekitEgress.EgressStatus.EGRESS_COMPLETE) {
            log.info(">>> [Egress Success] Status confirmed as COMPLETE.");

            // 4. 파일 정보 확인
            if (egressInfo.hasFile()) {
                LivekitEgress.FileInfo fileInfo = egressInfo.getFile();

                // Filename(Object Key)을 사용함
                String s3Key = fileInfo.getFilename();

                // 로그 확인용 (location도 로그엔 남겨두면 좋음)
                String location = fileInfo.getLocation();

                log.info(">>> [Egress File Found] Filename: {}, Location: {}", s3Key, location);

                // Service 호출
                try {
                    mediaRecordCommandService.saveRecording(roomName, s3Key);
                    log.info(">>> [Logic Success] mediaRecordCommandService.saveRecording called successfully.");
                } catch (Exception e) {
                    log.error(">>> [Logic Error] Failed to save recording info to DB", e);
                }
            } else {
                // 파일 정보가 없는 경우 (스트림 전용이거나 오류 등)
                log.warn(">>> [Egress Warning] Status is COMPLETE but 'hasFile()' is false. Check if outputType was 'file'.");
            }
        } else {
            // 실패하거나 중단된 경우
            log.warn(">>> [Egress Failed/Stopped] Egress did not complete successfully. Status: {}, Error: {}", status, egressInfo.getError());
        }
    }
}
