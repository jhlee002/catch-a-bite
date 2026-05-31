package com.deliveryapp.catchabite.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.deliveryapp.catchabite.domain.enumtype.DeliveryStatus;
import com.deliveryapp.catchabite.dto.DeliveryApiResponseDTO;
import com.deliveryapp.catchabite.dto.OrderDeliveryDTO;
import com.deliveryapp.catchabite.entity.Deliverer;
import com.deliveryapp.catchabite.repository.DelivererRepository;
import com.deliveryapp.catchabite.service.OrderDeliveryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/rider/deliveries")
@RequiredArgsConstructor
public class DelivererDeliveryController {

    private final OrderDeliveryService deliveryService;
    private final DelivererRepository delivererRepository;

    /**
     * 세션 기반 principal이 "RIDER:email" 형태로 저장되어 있는 현재 프로젝트 구조에서,
     * Authentication으로부터 delivererId를 복원한다.
     */
    private Long resolveDelivererId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationCredentialsNotFoundException("로그인이 필요합니다.");
        }

        // UsernamePasswordAuthenticationToken(String principal, ...)이면 getName() == principal.toString()
        String name = authentication.getName(); // 예: "RIDER:rider@test.com"
        if (name == null || !name.startsWith("RIDER:")) {
            throw new AccessDeniedException("라이더 계정만 접근 가능합니다.");
        }

        String email = name.substring("RIDER:".length()).trim();
        if (email.isEmpty()) {
            throw new AuthenticationCredentialsNotFoundException("로그인이 필요합니다.");
        }

        Deliverer deliverer = delivererRepository.findByDelivererEmail(email)
            .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("라이더 정보를 찾을 수 없습니다."));

        return deliverer.getDelivererId();
    }

    // 내 배달 단건 조회 (배달원)
    @GetMapping("/{deliveryId}")
    public ResponseEntity<DeliveryApiResponseDTO<OrderDeliveryDTO>> getDelivery(
            @PathVariable("deliveryId") Long deliveryId,
            Authentication authentication
    ) {
        Long delivererId = resolveDelivererId(authentication);

        return ResponseEntity.ok(
            DeliveryApiResponseDTO.success(
                "조회 성공",
                deliveryService.getDeliveryForDeliverer(deliveryId, delivererId)
            )
        );
    }

    // 내 배달 목록 (배달원)
    @GetMapping
    public ResponseEntity<DeliveryApiResponseDTO<List<OrderDeliveryDTO>>> getMyDeliveries(
            Authentication authentication
    ) {
        Long delivererId = resolveDelivererId(authentication);

        return ResponseEntity.ok(
            DeliveryApiResponseDTO.success(
                "조회 성공",
                deliveryService.getDeliveriesByDeliverer(delivererId)
            )
        );
    }

    // 상태별 내 배달 조회 (배달원)
    @GetMapping("/status")
    public ResponseEntity<DeliveryApiResponseDTO<List<OrderDeliveryDTO>>> getByStatus(
            @RequestParam("orderDeliveryStatus") DeliveryStatus orderDeliveryStatus,
            Authentication authentication
    ) {
        Long delivererId = resolveDelivererId(authentication);

        return ResponseEntity.ok(
            DeliveryApiResponseDTO.success(
                "조회 성공",
                deliveryService.getDeliveriesByDelivererInStatus(delivererId, orderDeliveryStatus)
            )
        );
    }
}