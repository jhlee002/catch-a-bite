package com.deliveryapp.catchabite.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.deliveryapp.catchabite.dto.DeliveryApiResponseDTO;
import com.deliveryapp.catchabite.dto.DeliveryAssignRequestDTO;
import com.deliveryapp.catchabite.service.OrderDeliveryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// 배달원 액션은 delivererId를 Body로 받지 않고, 로그인(Principal)에서 꺼냄
@RestController
@RequestMapping("/api/v1/rider/deliveries")
@RequiredArgsConstructor
public class DeliveryController {

    private final OrderDeliveryService deliveryService;

    // 배정(매장주인) - 사용하지 않음
    // "배달원을 지정(assign)"하는 요청이라 delivererId가 필요하므로 기존처럼 Body 유지
    @PostMapping("/{deliveryId}/assign")
    public ResponseEntity<DeliveryApiResponseDTO<Void>> assign(
            @PathVariable("deliveryId") Long deliveryId,
            @RequestBody @Valid DeliveryAssignRequestDTO req
            // (@AuthenticationPrincipal AuthUser user 받아서 "매장주인 권한" 검증 가능)
    ) {
        deliveryService.assignDeliverer(deliveryId, req.getDelivererId());
        return ResponseEntity.ok(DeliveryApiResponseDTO.success("배달원이 배정되었습니다."));
    }  
    // 수락(배달원) - 단계 축소로 더 이상 사용하지 않음
    @PostMapping("/{deliveryId}/accept")
    public ResponseEntity<DeliveryApiResponseDTO<Void>> accept(
            @PathVariable("deliveryId") Long deliveryId,
            Authentication authentication
    ) {
        return ResponseEntity.badRequest()
                .body(DeliveryApiResponseDTO.fail("accept 단계는 사용하지 않습니다. pickup-complete를 사용하세요."));
    }


    // 매장에서 픽업완료(배달원)
    @PostMapping("/{deliveryId}/pickup-complete")
    public ResponseEntity<DeliveryApiResponseDTO<Void>> pickupComplete(
            @PathVariable("deliveryId") Long deliveryId,
            Authentication authentication
    ) {
        // 임시로 추가한 코드 디버깅용(인증 오류 401 확인용)
        System.out.println("pickup authentication = " + authentication);
        System.out.println("pickup principal = " + (authentication !=  null ? authentication.getPrincipal() : null));

        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(DeliveryApiResponseDTO.fail("인증 정보가 없습니다."));
        }

        String principal = authentication.getPrincipal().toString();
        deliveryService.pickupCompleteByPrincipal(deliveryId, principal);

        return ResponseEntity.ok(DeliveryApiResponseDTO.success("픽업이 완료되었습니다."));
    }

    // 배달시작(배달원)
    @PostMapping("/{deliveryId}/start")
    public ResponseEntity<DeliveryApiResponseDTO<Void>> start(
            @PathVariable("deliveryId") Long deliveryId,
            Authentication authentication
    ) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(DeliveryApiResponseDTO.fail("인증 정보가 없습니다."));
        }

        String principal = authentication.getPrincipal().toString();
        deliveryService.startDeliveryByPrincipal(deliveryId, principal);

        return ResponseEntity.ok(DeliveryApiResponseDTO.success("배달이 시작되었습니다."));
    }

    @PostMapping("/{deliveryId}/complete")
    public ResponseEntity<DeliveryApiResponseDTO<Void>> complete(
            @PathVariable("deliveryId") Long deliveryId,
            Authentication authentication
    ) {
        // 임시로 추가한 코드 디버깅용(인증 오류 401 확인용)
        System.out.println("complete authentication = " + authentication);
        System.out.println("complete principal = " + (authentication != null ? authentication.getPrincipal() : null));

        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(DeliveryApiResponseDTO.fail("인증 정보가 없습니다."));
        }

        String principal = authentication.getPrincipal().toString();
        deliveryService.completeDeliveryByPrincipal(deliveryId, principal);

        return ResponseEntity.ok(DeliveryApiResponseDTO.success("배달이 완료되었습니다."));
    }

    // 배달 재오픈(관리자/시스템)
    @PostMapping("/{deliveryId}/reopen")
    public ResponseEntity<DeliveryApiResponseDTO<Void>> reopenDelivery(
            @PathVariable Long deliveryId
    ) {
        deliveryService.reopenDelivery(deliveryId);
        return ResponseEntity.ok(DeliveryApiResponseDTO.success("배달이 재오픈되었습니다."));
    }
}
