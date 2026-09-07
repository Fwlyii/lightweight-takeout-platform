package com.tju.elm_bk.service;

import com.tju.elm_bk.dto.MerchantInteractionUpdateDTO;
import com.tju.elm_bk.entity.MerchantInteraction;
import com.tju.elm_bk.exception.APIException;
import com.tju.elm_bk.mapper.BusinessMapper;
import com.tju.elm_bk.mapper.MerchantInteractionMapper;
import com.tju.elm_bk.result.ResultCodeEnum;
import com.tju.elm_bk.vo.BusinessSearchVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('USER')")
public class MerchantInteractionService {
    private final MerchantInteractionMapper interactions;
    private final BusinessMapper businesses;
    private final BusinessService storefront;
    private final CurrentUserService currentUser;
    private final AccountWriteLock writeLock;

    public List<BusinessSearchVO> collections() {
        var ids = new HashSet<>(interactions.collectionIds(currentUser.requireUserId()));
        if (ids.isEmpty()) return List.of();
        // Shared source of live ratings, sales and tags; no second formula or cached rating.
        return storefront.getBusinessesBySearch(null, false, false).stream().filter(b -> ids.contains(b.getId())).toList();
    }
    public MerchantInteraction status(Long merchantId) {
        ensureVisible(merchantId);
        return currentOrEmpty(currentUser.requireUserId(), merchantId);
    }
    @Transactional
    public MerchantInteraction update(MerchantInteractionUpdateDTO request) {
        if (request.getCollected() == null && request.getLiked() == null) {
            throw new APIException(ResultCodeEnum.PARAM_VERIFIED_FAILED);
        }
        Long userId = writeLock.acquire();
        var interaction = currentOrEmpty(userId, request.getMerchantId());
        if (interaction.getId() == null || Boolean.TRUE.equals(request.getCollected()) || Boolean.TRUE.equals(request.getLiked())) {
            ensureVisible(request.getMerchantId());
        }
        if (request.getCollected() != null) interaction.setCollected(request.getCollected());
        if (request.getLiked() != null) interaction.setLiked(request.getLiked());
        if (interaction.getId() == null) interactions.insert(interaction);
        else interactions.update(interaction);
        return interactions.selectByUserAndMerchant(userId, request.getMerchantId());
    }
    private MerchantInteraction currentOrEmpty(Long userId, Long merchantId) {
        var result = interactions.selectByUserAndMerchant(userId, merchantId);
        if (result == null) {
            result = new MerchantInteraction();
            result.setUserId(userId); result.setMerchantId(merchantId);
            result.setCollected(false); result.setLiked(false);
        }
        return result;
    }
    private void ensureVisible(Long id) {
        var business = businesses.selectBusinessById(id);
        if (business == null || !Integer.valueOf(1).equals(business.getStatus())) throw new APIException(ResultCodeEnum.BUSINESS_MISSED);
    }
}
