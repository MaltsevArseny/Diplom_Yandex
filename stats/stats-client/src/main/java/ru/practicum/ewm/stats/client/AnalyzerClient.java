package ru.practicum.ewm.stats.client;

import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.proto.analyzer.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.proto.analyzer.RecommendedEventProto;
import ru.practicum.ewm.stats.proto.analyzer.RecommendationsControllerGrpc;
import ru.practicum.ewm.stats.proto.analyzer.SimilarEventsRequestProto;
import ru.practicum.ewm.stats.proto.analyzer.UserPredictionsRequestProto;

import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Slf4j
@Service
public class AnalyzerClient {

    @GrpcClient("analyzer")
    private RecommendationsControllerGrpc.RecommendationsControllerBlockingStub analyzerStub;

    public List<RecommendedEventProto> getRecommendationsForUser(long userId, int maxResults) {
        try {
            UserPredictionsRequestProto request = UserPredictionsRequestProto.newBuilder()
                .setUserId(userId)
                .setMaxResults(maxResults)
                .build();
            return asStream(analyzerStub.getRecommendationsForUser(request)).collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Failed to get recommendations for userId={}: {}", userId, e.getMessage());
            return List.of();
        }
    }

    public List<RecommendedEventProto> getSimilarEvents(long eventId, long userId, int maxResults) {
        try {
            SimilarEventsRequestProto request = SimilarEventsRequestProto.newBuilder()
                .setEventId(eventId)
                .setUserId(userId)
                .setMaxResults(maxResults)
                .build();
            return asStream(analyzerStub.getSimilarEvents(request)).collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Failed to get similar events for eventId={}: {}", eventId, e.getMessage());
            return List.of();
        }
    }

    public List<RecommendedEventProto> getInteractionsCount(List<Long> eventIds) {
        try {
            InteractionsCountRequestProto request = InteractionsCountRequestProto.newBuilder()
                .addAllEventId(eventIds)
                .build();
            return asStream(analyzerStub.getInteractionsCount(request)).collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Failed to get interactions count: {}", e.getMessage());
            return List.of();
        }
    }

    private Stream<RecommendedEventProto> asStream(java.util.Iterator<RecommendedEventProto> iterator) {
        return StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false);
    }
}
