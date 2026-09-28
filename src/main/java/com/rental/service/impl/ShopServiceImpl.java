package com.rental.service.impl;

import com.rental.dto.ShopRequest;
import com.rental.entity.Owner;
import com.rental.entity.Shop;
import com.rental.exception.ApiException;
import com.rental.repository.OwnerRepository;
import com.rental.repository.ShopRepository;
import com.rental.service.ShopService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ShopServiceImpl implements ShopService {

    private final ShopRepository shopRepository;
    private final OwnerRepository ownerRepository;

    public ShopServiceImpl(ShopRepository shopRepository, OwnerRepository ownerRepository) {
        this.shopRepository = shopRepository;
        this.ownerRepository = ownerRepository;
    }

    @Override
    public Shop createShop(Long ownerId, ShopRequest request) {
        Owner owner = ownerRepository.findById(ownerId)
                .orElseThrow(() -> new ApiException("Owner not found", HttpStatus.NOT_FOUND));

        Shop shop = Shop.builder()
                .owner(owner)
                .shopName(request.getShopName())
                .address(request.getAddress())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .contactPhone(request.getContactPhone())
                .build();
        return shopRepository.save(shop);
    }

    @Override
    public Shop updateShop(Long ownerId, Long shopId, ShopRequest request) {
        Shop shop = getShopOrThrow(shopId);
        assertOwnership(ownerId, shop);

        shop.setShopName(request.getShopName());
        shop.setAddress(request.getAddress());
        shop.setLatitude(request.getLatitude());
        shop.setLongitude(request.getLongitude());
        shop.setContactPhone(request.getContactPhone());
        return shopRepository.save(shop);
    }

    @Override
    public List<Shop> getShopsByOwner(Long ownerId) {
        return shopRepository.findByOwnerId(ownerId);
    }

    @Override
    public List<Shop> getAllShops() {
        return shopRepository.findAll();
    }

    @Override
    public List<Shop> getNearbyShops(Double lat, Double lng, Double radiusKm) {
        double radius = radiusKm != null ? radiusKm : 10.0;
        return shopRepository.findNearbyShops(lat, lng, radius);
    }

    @Override
    public Shop getShopOrThrow(Long shopId) {
        return shopRepository.findById(shopId)
                .orElseThrow(() -> new ApiException("Shop not found", HttpStatus.NOT_FOUND));
    }

    @Override
    public void assertOwnership(Long ownerId, Shop shop) {
        if (!shop.getOwner().getId().equals(ownerId)) {
            throw new ApiException("You do not have access to this shop", HttpStatus.FORBIDDEN);
        }
    }
}
