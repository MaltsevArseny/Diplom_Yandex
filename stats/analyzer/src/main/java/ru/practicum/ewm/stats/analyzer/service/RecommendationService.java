package ru.practicum.ewm.stats.analyzer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.analyzer.model.EventSimilarity;
import ru.practicum.ewm.stats.analyzer.model.UserAction;
import ru.practicum.ewm.stats.analyzer.repository.EventSimilarityRepository;
import ru.practicum.ewm.stats.analyzer.repository.UserActionRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private static final int RECENT_INTERACTIONS_LIMIT = 10;
    private static final int K_NEIGHBORS = 5;

    private final UserActionRepository userActionRepository;
    private final EventSimilarityRepository eventSimilarityRepository;

    public List<long[]> getRecommendationsForUser(long userId, int maxResults) {
        List<UserAction> recentActions = userActionRepository
            .findAllByUserIdOrderByTimestampDesc(userId, PageRequest.of(0, RECENT_INTERACTIONS_LIMIT));

        if (recentActions.isEmpty()) return List.of();

        Set<Long> interactedEvents = recentActions.stream()
            .map(UserAction::getEventId)
            .collect(Collectors.toSet());

        // Collect candidates: events similar to interacted ones, not yet seen
        Map<Long, Double> candidateScores = new java.util.HashMap<>();
        for (UserAction action : recentActions) {
            List<EventSimilarity> similarities = eventSimilarityRepository.findByEventId(action.getEventId());
            for (EventSimilarity sim : similarities) {
                long otherEvent = sim.getEventA().equals(action.getEventId()) ? sim.getEventB() : sim.getEventA();
                if (!interactedEvents.contains(otherEvent)) {
                    candidateScores.merge(otherEvent, sim.getScore(), Double::max);
                }
            }
        }

        if (candidateScores.isEmpty()) return List.of();

        // Take top N candidates by similarity
        List<Long> topCandidates = candidateScores.entrySet().stream()
            .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
            .limit(maxResults * 2L)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        // Calculate predicted score for each candidate
        List<long[]> result = new ArrayList<>();
        for (Long candidateId : topCandidates) {
            double predicted = predictScore(userId, candidateId, interactedEvents);
            result.add(new long[]{candidateId, Double.doubleToLongBits(predicted)});
        }

        result.sort(Comparator.comparingDouble(a -> -Double.longBitsToDouble(a[1])));
        return result.stream().limit(maxResults).collect(Collectors.toList());
    }

    public List<long[]> getSimilarEvents(long eventId, long userId, int maxResults) {
        Set<Long> interactedEvents = userActionRepository.findAllByUserId(userId).stream()
            .map(UserAction::getEventId)
            .collect(Collectors.toSet());

        List<EventSimilarity> similarities = eventSimilarityRepository.findByEventId(eventId);

        return similarities.stream()
            .map(sim -> {
                long other = sim.getEventA().equals(eventId) ? sim.getEventB() : sim.getEventA();
                return new long[]{other, Double.doubleToLongBits(sim.getScore())};
            })
            .filter(pair -> !interactedEvents.contains(pair[0]))
            .sorted(Comparator.comparingDouble(a -> -Double.longBitsToDouble(a[1])))
            .limit(maxResults)
            .collect(Collectors.toList());
    }

    public List<long[]> getInteractionsCount(List<Long> eventIds) {
        List<Object[]> rows = userActionRepository.sumWeightsByEventIds(eventIds);
        return rows.stream()
            .map(row -> new long[]{(Long) row[0], Double.doubleToLongBits(((Number) row[1]).doubleValue())})
            .collect(Collectors.toList());
    }

    private double predictScore(long userId, long candidateId, Set<Long> interactedEvents) {
        List<EventSimilarity> allSim = eventSimilarityRepository.findByEventId(candidateId);

        List<EventSimilarity> neighbors = allSim.stream()
            .filter(sim -> {
                long other = sim.getEventA().equals(candidateId) ? sim.getEventB() : sim.getEventA();
                return interactedEvents.contains(other);
            })
            .sorted(Comparator.comparingDouble(EventSimilarity::getScore).reversed())
            .limit(K_NEIGHBORS)
            .collect(Collectors.toList());

        if (neighbors.isEmpty()) return 0.0;

        List<Long> neighborIds = neighbors.stream()
            .map(sim -> sim.getEventA().equals(candidateId) ? sim.getEventB() : sim.getEventA())
            .collect(Collectors.toList());

        Map<Long, Double> userWeights = userActionRepository
            .findByUserIdAndEventIdIn(userId, neighborIds).stream()
            .collect(Collectors.toMap(UserAction::getEventId, UserAction::getWeight));

        double weightedSum = 0.0;
        double simSum = 0.0;
        for (EventSimilarity sim : neighbors) {
            long neighborId = sim.getEventA().equals(candidateId) ? sim.getEventB() : sim.getEventA();
            Double userWeight = userWeights.get(neighborId);
            if (userWeight != null) {
                weightedSum += sim.getScore() * userWeight;
                simSum += sim.getScore();
            }
        }

        return simSum > 0 ? weightedSum / simSum : 0.0;
    }
}
