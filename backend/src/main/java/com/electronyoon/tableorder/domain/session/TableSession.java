package com.electronyoon.tableorder.domain.session;

import com.electronyoon.tableorder.domain.storetable.StoreTable;
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
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "table_session")
@Getter
@Setter
@NoArgsConstructor
public class TableSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id", nullable = false)
    private StoreTable table;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SessionStatus status;

    @Column(name = "opened_at", nullable = false)
    private OffsetDateTime openedAt;

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    public static TableSession open(StoreTable table) {
        TableSession session = new TableSession();
        session.setTable(table);
        session.setStatus(SessionStatus.OPEN);
        session.setOpenedAt(OffsetDateTime.now());
        return session;
    }

    public void close() {
        this.status = SessionStatus.CLOSED;
        this.closedAt = OffsetDateTime.now();
    }
}
