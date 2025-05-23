package edu.help.microservice.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import edu.help.microservice.dto.MerchantDTO;
import edu.help.microservice.dto.InventoryResponse;
import edu.help.microservice.entity.Merchant;
import edu.help.microservice.entity.Category;
import edu.help.microservice.entity.Item;
import edu.help.microservice.repository.MerchantRepository;
import edu.help.microservice.repository.CategoryRepository;
import edu.help.microservice.repository.ItemRepository;
import edu.help.microservice.util.DTOConverter;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class MerchantService {

    private final MerchantRepository merchantRepository;
    private final ItemRepository itemRepository;
    private final CategoryRepository categoryRepository;

    /**
     * Returns a list of MerchantDTOs for merchants that are currently open
     * and have a verified Stripe account.
     * 
     * Filters:
     * - Only includes merchants where isOpen = true
     * - Only includes merchants with stripeVerificationStatus = "verified"
     * 
     * @return List of eligible MerchantDTOs
     */
    public List<MerchantDTO> findAllMerchants() {
        List<Merchant> merchants = merchantRepository.findAll().stream()
                .filter(m -> Boolean.TRUE.equals(m.getIsOpen()))
                .filter(m -> "verified".equalsIgnoreCase(m.getStripeVerificationStatus()))
                .collect(Collectors.toList());

        return merchants.stream()
                .map(DTOConverter::convertToMerchantDTO)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves the Stripe Account ID for the given Merchant ID.
     * 
     * @param merchantId The ID of the merchant.
     * @return The account ID string (e.g., "acct_1QJSzEPP544TBp2T"), or null if not
     *         set or merchant doesn't exist.
     */
    public String getAccountId(Integer merchantId) {
        return merchantRepository.findById(merchantId)
                .map(Merchant::getAccountId)
                .orElse(null);
    }

    /**
     * Finds a Merchant by its ID.
     * 
     * @param id The merchant's ID.
     * @return The Merchant if found, otherwise null.
     */
    public Merchant findMerchantById(Integer id) {
        Optional<Merchant> merchant = merchantRepository.findById(id);
        return merchant.orElse(null);
    }

    /**
     * Retrieves inventory for a given Merchant ID (excluding certain fields).
     */
    public InventoryResponse getInventoryByMerchantId(Integer merchantId) {
        List<Item> items = itemRepository.findByMerchantId(merchantId);
        List<Category> categories = categoryRepository.findByMerchantId(merchantId);

        return InventoryResponse.builder()
                .items(items)
                .categories(categories)
                .build();
    }

    /**
     * Deletes a Merchant from the database.
     */
    public void delete(Merchant merchant) {
        merchantRepository.delete(merchant);
    }

    /**
     * Saves (creates or updates) a Merchant object.
     * 
     * @param merchant The Merchant to save.
     * @return The saved Merchant object.
     */
    public Merchant save(Merchant merchant) {
        return merchantRepository.save(merchant);
    }
}