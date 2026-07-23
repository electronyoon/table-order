package com.electronyoon.tableorder.domain.menu;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "menu")
@Getter
@Setter
@NoArgsConstructor
public class Menu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private MenuCategory category;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int price;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "is_self_service", nullable = false)
    private boolean selfService;

    /** 품절 여부가 아니라 "품절된 영업일"을 저장한다. 오늘 영업일과 같으면 품절. */
    @Column(name = "sold_out_date")
    private LocalDate soldOutDate;

    public void markSoldOut(LocalDate businessDay) {
        this.soldOutDate = businessDay;
    }

    public void restore() {
        this.soldOutDate = null;
    }
}
