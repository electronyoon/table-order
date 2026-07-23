package com.electronyoon.tableorder.domain.order;

import com.electronyoon.tableorder.domain.menu.Menu;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "order_item")
@Getter
@Setter
@NoArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id", nullable = false)
    private Menu menu;

    /** 주문 시점 스냅샷 (메뉴 가격 변경과 무관) */
    @Column(name = "menu_name", nullable = false)
    private String menuName;

    @Column(name = "unit_price", nullable = false)
    private int unitPrice;

    @Column(nullable = false)
    private int quantity;

    @Column
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private OrderItemStatus status;

    public static OrderItem fromMenu(Menu menu, int quantity, String note) {
        OrderItem item = new OrderItem();
        item.setMenu(menu);
        item.setMenuName(menu.getName());
        item.setUnitPrice(menu.getPrice());
        item.setQuantity(quantity);
        item.setNote(note);
        item.setStatus(OrderItemStatus.ACTIVE);
        return item;
    }

    public void cancel() {
        this.status = OrderItemStatus.CANCELED;
    }
}
