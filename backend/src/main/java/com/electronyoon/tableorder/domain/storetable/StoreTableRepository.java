package com.electronyoon.tableorder.domain.storetable;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreTableRepository extends JpaRepository<StoreTable, Long> {

    Optional<StoreTable> findByQrToken(String qrToken);
}
