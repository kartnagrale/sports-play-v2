package com.neml.badminton.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "app_screens", indexes = {
        @Index(name = "idx_app_screen_order", columnList = "section_name, display_order")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AppScreen {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(nullable = false, length = 80)
    private String label;

    @Column(nullable = false, unique = true, length = 160)
    private String path;

    @Column(nullable = false, length = 64)
    private String icon;

    @Column(name = "section_name", nullable = false, length = 64)
    private String section;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(nullable = false)
    private Boolean active;
}
