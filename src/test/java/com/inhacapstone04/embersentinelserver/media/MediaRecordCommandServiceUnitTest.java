package com.inhacapstone04.embersentinelserver.media;

import com.inhacapstone04.embersentinelserver.fire_event.entity.FireEvent;
import com.inhacapstone04.embersentinelserver.fire_event.repository.FireEventRepository;
import com.inhacapstone04.embersentinelserver.media.entity.MediaRecord;
import com.inhacapstone04.embersentinelserver.media.repository.MediaRecordRepository;
import com.inhacapstone04.embersentinelserver.media.service.MediaRecordCommandService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaRecordCommandServiceUnitTest {

    @InjectMocks MediaRecordCommandService service;
    @Mock MediaRecordRepository mediaRecordRepository;
    @Mock FireEventRepository fireEventRepository;

    @Test
    void savesFirstRecording() {
        FireEvent fireEvent = new FireEvent();
        when(fireEventRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(fireEvent));

        service.saveRecording("fire_event_101", "recordings/fire_event_101.mp4");

        verify(mediaRecordRepository).save(argThat(record ->
                record.getFireEvent() == fireEvent
                        && "recordings/fire_event_101.mp4".equals(record.getS3BucketPath())));
    }

    @Test
    void ignoresRedeliveredRecording() {
        when(fireEventRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(new FireEvent()));
        when(mediaRecordRepository.existsByFireEvent_Id(101L)).thenReturn(false, true);

        service.saveRecording("fire_event_101", "recordings/fire_event_101.mp4");
        service.saveRecording("fire_event_101", "recordings/fire_event_101.mp4");

        verify(mediaRecordRepository, times(1)).save(any(MediaRecord.class));
    }
}
