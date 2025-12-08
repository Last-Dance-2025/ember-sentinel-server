package com.inhacapstone04.embersentinelserver.media.entity;

import com.inhacapstone04.embersentinelserver.common.entity.CreatedAtEntity;
import com.inhacapstone04.embersentinelserver.fire_event.entity.FireEvent;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Table(name = "media_record")
@Entity
@Getter
@Setter
public class MediaRecord extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "s3_bucket_path")
    private String s3BucketPath;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fire_event_id", nullable = false, unique = true)
    private FireEvent fireEvent;
}
