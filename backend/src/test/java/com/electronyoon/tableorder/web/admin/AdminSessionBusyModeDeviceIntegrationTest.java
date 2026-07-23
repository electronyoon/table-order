package com.electronyoon.tableorder.web.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.electronyoon.tableorder.TestcontainersConfiguration;
import com.electronyoon.tableorder.domain.session.TableSession;
import com.electronyoon.tableorder.domain.session.TableSessionRepository;
import com.electronyoon.tableorder.domain.storetable.StoreTable;
import com.electronyoon.tableorder.domain.storetable.StoreTableRepository;
import com.electronyoon.tableorder.web.dto.BusyModeRequest;
import com.electronyoon.tableorder.web.dto.DeviceDto;
import com.electronyoon.tableorder.web.dto.RegisterDeviceRequest;
import com.electronyoon.tableorder.web.dto.TableSessionDto;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Import(TestcontainersConfiguration.class)
class AdminSessionBusyModeDeviceIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private StoreTableRepository storeTableRepository;
    @Autowired
    private TableSessionRepository tableSessionRepository;
    @Value("${app.admin-token}")
    private String adminToken;

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        return headers;
    }

    @Test
    void closeSessionSetsStatusClosedAndIsIdempotent() {
        StoreTable table = new StoreTable();
        table.setLabel("정산테스트");
        table.setQrToken("qr-" + UUID.randomUUID());
        table = storeTableRepository.saveAndFlush(table);

        TableSession session = tableSessionRepository.saveAndFlush(TableSession.open(table));

        ResponseEntity<TableSessionDto> first = restTemplate.exchange(
                "/admin/sessions/" + session.getId() + "/close", HttpMethod.POST,
                new HttpEntity<>(authHeaders()), TableSessionDto.class);
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(first.getBody().status()).isEqualTo("CLOSED");

        ResponseEntity<TableSessionDto> second = restTemplate.exchange(
                "/admin/sessions/" + session.getId() + "/close", HttpMethod.POST,
                new HttpEntity<>(authHeaders()), TableSessionDto.class);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(second.getBody().status()).isEqualTo("CLOSED");
    }

    @Test
    void closeSessionWithUnknownIdReturns404() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/admin/sessions/999999/close", HttpMethod.POST, new HttpEntity<>(authHeaders()), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void setBusyModeReturns200() {
        BusyModeRequest request = new BusyModeRequest(true);
        ResponseEntity<Void> response = restTemplate.exchange(
                "/admin/busy-mode", HttpMethod.POST, new HttpEntity<>(request, authHeaders()), Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void registerDeviceReturns201() {
        RegisterDeviceRequest request = new RegisterDeviceRequest("주방 태블릿", com.electronyoon.tableorder.domain.device.DeviceRole.PRIMARY, "fcm-token-abc");
        ResponseEntity<DeviceDto> response = restTemplate.exchange(
                "/admin/devices", HttpMethod.POST, new HttpEntity<>(request, authHeaders()), DeviceDto.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().role()).isEqualTo("PRIMARY");
        assertThat(response.getBody().id()).isNotNull();
    }
}
