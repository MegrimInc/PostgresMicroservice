package edu.help.microservice.service;

import edu.help.microservice.dto.LeaderboardRankResponse;
import edu.help.microservice.entity.Customer;
import edu.help.microservice.entity.Leaderboard;
import edu.help.microservice.repository.CustomerRepository;
import edu.help.microservice.repository.LeaderboardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LeaderboardService {

    private final LeaderboardRepository leaderboardRepository;
    private final CustomerRepository customerRepository;

    @Transactional(readOnly = true)
    public LeaderboardRankResponse getRank(int merchantId, int customerId) {
        List<Leaderboard> leaderboard = leaderboardRepository.findByMerchantIdOrderByTotalDesc(merchantId);
        List<Customer> allCustomers = customerRepository.findAll();

        Customer customer = allCustomers.stream()
                .filter(c -> c.getCustomerId().equals(customerId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        String customerFullName = formatFullName(customer.getFirstName(), customer.getLastName());

        // 🔎 Look for this customer on leaderboard
        Optional<Leaderboard> theirEntryOpt = leaderboard.stream()
                .filter(e -> e.getCustomerId().equals(customerId))
                .findFirst();

        if (theirEntryOpt.isPresent()) {
            Leaderboard entry = theirEntryOpt.get();

            String rivalFullName = findCustomerById(entry.getRivalId()).getFirstName();

            return LeaderboardRankResponse.builder()
                    .rank(entry.getRank())
                    .difference(entry.getDifference())
                    .rivalFullName(rivalFullName)
                    .customerFullName(customerFullName)
                    .build();
        }

        // 🟠 Edge Case 1: Leaderboard is EMPTY
        if (leaderboard.isEmpty()) {
            long countBefore = allCustomers.stream()
                    .filter(c -> c.getCustomerId() < customerId)
                    .count();
            int rank = (int) countBefore + 1;

            Customer rivalCustomer = allCustomers.stream()
                    .min(Comparator.comparingInt(Customer::getCustomerId))
                    .orElse(customer);

            String rivalFullName = formatFullName(rivalCustomer.getFirstName(), rivalCustomer.getLastName());

            return LeaderboardRankResponse.builder()
                    .rank(rank)
                    .difference(0.01)
                    .rivalFullName(rivalFullName)
                    .customerFullName(customerFullName)
                    .build();
        }

        // 🟠 Edge Case 2: Customer NOT on leaderboard but board is populated
        int onBoardCount = leaderboard.size();

        long lowerUnrankedCount = allCustomers.stream()
                .filter(c -> c.getCustomerId() < customerId)
                .filter(c -> leaderboard.stream().noneMatch(lb -> lb.getCustomerId().equals(c.getCustomerId())))
                .count();

        int rank = onBoardCount + (int) lowerUnrankedCount + 1;

        Leaderboard lowestEntry = leaderboard.get(leaderboard.size() - 1);
        double difference = lowestEntry.getTotal();

        Customer rival = findCustomerById(lowestEntry.getCustomerId());
        String rivalFullName = formatFullName(rival.getFirstName(), rival.getLastName());

        return LeaderboardRankResponse.builder()
                .rank(rank)
                .difference(difference)
                .rivalFullName(rivalFullName)
                .customerFullName(customerFullName)
                .build();
    }

    @Transactional
    public void addToLeaderboard(int merchantId, int customerId, double amount) {
        // 1. Update or insert this customer's total
        Optional<Leaderboard> leaderboardOpt = leaderboardRepository
                .findByMerchantIdOrderByTotalDesc(merchantId)
                .stream()
                .filter(l -> l.getCustomerId().equals(customerId))
                .findFirst();

        if (leaderboardOpt.isPresent()) {
            Leaderboard leaderboard = leaderboardOpt.get();
            leaderboard.setTotal(leaderboard.getTotal() + amount);
            leaderboardRepository.save(leaderboard);
        } else {
            Leaderboard newLeaderboard = Leaderboard.builder()
                    .merchantId(merchantId)
                    .customerId(customerId)
                    .total(amount)
                    .rank(0) // dummy placeholder
                    .rivalId(0) // dummy placeholder
                    .difference(0.0) // dummy placeholder
                    .build();
            leaderboardRepository.save(newLeaderboard);
        }

        // 2. Recalculate all leaderboard entries for this merchant
        List<Leaderboard> allEntries = leaderboardRepository.findByMerchantIdOrderByTotalDesc(merchantId);

        // 3. Sort with tiebreaker
        allEntries.sort(
                Comparator.comparingDouble(Leaderboard::getTotal).reversed()
                        .thenComparingInt(Leaderboard::getCustomerId));

        // 4. Determine fallback rival
        Customer lowestIdCustomer = customerRepository.findAll()
                .stream()
                .min(Comparator.comparingInt(Customer::getCustomerId))
                .orElse(null);

        // 5. Assign rank, rival, difference
        for (int i = 0; i < allEntries.size(); i++) {
            Leaderboard entry = allEntries.get(i);
            entry.setRank(i + 1);

            double difference;
            int rivalId;

            if (i == 0) {
                // First place
                if (allEntries.size() > 1) {
                    difference = entry.getTotal() - allEntries.get(1).getTotal();
                    rivalId = allEntries.get(1).getCustomerId();
                } else {
                    difference = -entry.getTotal();
                    rivalId = lowestIdCustomer != null ? lowestIdCustomer.getCustomerId() : entry.getCustomerId();
                }
            } else {
                difference = allEntries.get(i - 1).getTotal() - entry.getTotal();
                rivalId = allEntries.get(i - 1).getCustomerId();
            }

            entry.setDifference(difference);
            entry.setRivalId(rivalId);
        }

        // 6. Save everything
        leaderboardRepository.saveAll(allEntries);
    }

    private Customer findCustomerById(int customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
    }

    private String formatFullName(String firstName, String lastName) {
        return capitalize(firstName) + " " + capitalize(lastName);
    }

    private String capitalize(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        return input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
    }
}