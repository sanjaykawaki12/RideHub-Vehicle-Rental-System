package com.rental.service;

import com.rental.dto.ShopRequest;
import com.rental.entity.Shop;
import java.util.List;

public interface ShopService {
    Shop createShop(Long ownerId, ShopRequest request);
    Shop updateShop(Long ownerId, Long shopId, ShopRequest request);
    List<Shop> getShopsByOwner(Long ownerId);
    List<Shop> getAllShops();
    List<Shop> getNearbyShops(Double lat, Double lng, Double radiusKm);
    Shop getShopOrThrow(Long shopId);
    void assertOwnership(Long ownerId, Shop shop);
}
