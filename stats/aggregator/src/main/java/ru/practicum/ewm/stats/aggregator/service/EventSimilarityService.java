package ru.practicum.ewm.stats.aggregator.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class EventSimilarityService {

    private static final Map<ActionTypeAvro, Double> ACTION_WEIGHTS = Map.of(
        ActionTypeAvro.VIEW, 0.4,
        ActionTypeAvro.REGISTER, 0.8,
        ActionTypeAvro.LIKE, 1.0
    );

    // eventId -> (userId -> maxWeight)
    private final Map<Long, Map<Long, Double>> eventUserWeights = new HashMap<>();

    // sum of weights per event
    private final Map<Long, Double> eventWeightSums = new HashMap<>();

    // min-weight sums per ordered pair: (min(eA,eB), max(eA,eB)) -> sum
    private final Map<Long, Map<Long, Double>> minWeightSums = new HashMap<>();

    public List<EventSimilarityAvro> processAction(UserActionAvro action) {
        long userId = action.getUserId();
        long eventId = action.getEventId();
        double newWeight = ACTION_WEIGHTS.getOrDefault(action.getActionType(), 0.4);

        double oldWeight = eventUserWeights
            .computeIfAbsent(eventId, e -> new HashMap<>())
            .getOrDefault(userId, 0.0);

        if (newWeight <= oldWeight) {
            return List.of();
        }

        eventUserWeights.get(eventId).put(userId, newWeight);

        double weightDelta = newWeight - oldWeight;
        eventWeightSums.merge(eventId, weightDelta, Double::sum);

        List<EventSimilarityAvro> results = new ArrayList<>();

        for (Long otherEventId : eventUserWeights.keySet()) {
            if (otherEventId.equals(eventId)) continue;

            Double userWeightForOther = eventUserWeights.get(otherEventId).get(userId);
            if (userWeightForOther == null) continue;

            double oldMin = getMinWeightSum(eventId, otherEventId);
            double newMin = oldMin + (Math.min(newWeight, userWeightForOther) - Math.min(oldWeight, userWeightForOther));
            putMinWeightSum(eventId, otherEventId, newMin);

            double sA = eventWeightSums.getOrDefault(eventId, 0.0);
            double sB = eventWeightSums.getOrDefault(otherEventId, 0.0);

            if (sA <= 0 || sB <= 0) continue;

            double similarity = newMin / (Math.sqrt(sA) * Math.sqrt(sB));

            long eA = Math.min(eventId, otherEventId);
            long eB = Math.max(eventId, otherEventId);

            results.add(EventSimilarityAvro.newBuilder()
                .setEventA(eA)
                .setEventB(eB)
                .setScore(similarity)
                .setTimestamp(action.getTimestamp())
                .build());
        }

        return results;
    }

    private double getMinWeightSum(long e1, long e2) {
        long first = Math.min(e1, e2);
        long second = Math.max(e1, e2);
        return minWeightSums
            .computeIfAbsent(first, k -> new HashMap<>())
            .getOrDefault(second, 0.0);
    }

    private void putMinWeightSum(long e1, long e2, double value) {
        long first = Math.min(e1, e2);
        long second = Math.max(e1, e2);
        minWeightSums
            .computeIfAbsent(first, k -> new HashMap<>())
            .put(second, value);
    }
}
