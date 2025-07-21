package edu.help.microservice.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import edu.help.microservice.entity.Merchant;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, Integer> {

    /**
     * Retrieves a Merchant by its primary key (merchant_id).
     *
     * @param merchantId the ID of the merchant
     * @return the Merchant entity, or null if not found
     */
    Merchant getMerchantsByMerchantId(Integer merchantId);

    /**
     * Retrieves a Merchant by its associated Stripe account ID.
     *
     * @param accountId the Stripe account ID associated with the merchant
     * @return the Merchant entity, or null if not found
     */
    Merchant findByAccountId(String accountId);

    @Modifying
    @Transactional
    @Query("UPDATE Merchant m SET m.image = :url WHERE m.merchantId = :id")
    void updateImageById(@Param("id") Integer id, @Param("url") String url);

    @Query("SELECT m.image FROM Merchant m WHERE m.merchantId = :id")
    String findImageById(@Param("id") Integer id);

}