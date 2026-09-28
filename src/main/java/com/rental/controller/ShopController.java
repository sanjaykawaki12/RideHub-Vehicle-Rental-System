package com.rental.controller;

import com.rental.dto.ApiResponse;
import com.rental.dto.ShopRequest;
import com.rental.entity.Shop;
import com.rental.security.AuthPrincipal;
import com.rental.service.ShopService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ShopController {

    private final ShopService shopService;

    public ShopController(ShopService shopService) {
        this.shopService = shopService;
    }

    // Public: user page — nearby shops using device geolocation (lat/lng)
    @GetMapping("/shops/nearby")
    public ResponseEntity<ApiResponse<List<Shop>>> nearby(
            @RequestParam Double lat,
            @RequestParam Double lng,
            @RequestParam(required = false) Double radiusKm) {
        List<Shop> shops = shopService.getNearbyShops(lat, lng, radiusKm);
        return ResponseEntity.ok(ApiResponse.ok("Nearby shops fetched", shops));
    }

    // Public: list all shops when location is unavailable
    @GetMapping("/shops/all")
    public ResponseEntity<ApiResponse<List<Shop>>> allShops() {
        return ResponseEntity.ok(ApiResponse.ok("Shops fetched", shopService.getAllShops()));
    }

    // Public: fetch one shop for the customer shop page
    @GetMapping("/shops/{id}")
    public ResponseEntity<ApiResponse<Shop>> getShop(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Shop fetched", shopService.getShopOrThrow(id)));
    }

    // Owner: create their shop (one owner -> access only to their own shop)
    @PostMapping("/owner/shops")
    public ResponseEntity<ApiResponse<Shop>> createShop(@AuthenticationPrincipal AuthPrincipal principal,
                                                          @Valid @RequestBody ShopRequest request) {
        Shop shop = shopService.createShop(principal.id(), request);
        return ResponseEntity.ok(ApiResponse.ok("Shop created", shop));
    }

    @PutMapping("/owner/shops/{shopId}")
    public ResponseEntity<ApiResponse<Shop>> updateShop(@AuthenticationPrincipal AuthPrincipal principal,
                                                          @PathVariable Long shopId,
                                                          @Valid @RequestBody ShopRequest request) {
        Shop shop = shopService.updateShop(principal.id(), shopId, request);
        return ResponseEntity.ok(ApiResponse.ok("Shop updated", shop));
    }

    @GetMapping("/owner/shops")
    public ResponseEntity<ApiResponse<List<Shop>>> myShops(@AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok("Shops fetched", shopService.getShopsByOwner(principal.id())));
    }
}
