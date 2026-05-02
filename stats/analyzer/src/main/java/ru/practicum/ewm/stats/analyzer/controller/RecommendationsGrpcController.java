package ru.practicum.ewm.stats.analyzer.controller;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.ewm.stats.analyzer.service.RecommendationService;
import ru.practicum.ewm.stats.proto.analyzer.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.proto.analyzer.RecommendedEventProto;
import ru.practicum.ewm.stats.proto.analyzer.RecommendationsControllerGrpc;
import ru.practicum.ewm.stats.proto.analyzer.SimilarEventsRequestProto;
import ru.practicum.ewm.stats.proto.analyzer.UserPredictionsRequestProto;

import java.util.List;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class RecommendationsGrpcController
    extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {

    private final RecommendationService recommendationService;

    @Override
    public void getRecommendationsForUser(UserPredictionsRequestProto request,
                                          StreamObserver<RecommendedEventProto> responseObserver) {
        log.debug("GetRecommendationsForUser userId={}, maxResults={}", request.getUserId(), request.getMaxResults());
        List<long[]> recs = recommendationService.getRecommendationsForUser(
            request.getUserId(), request.getMaxResults());
        for (long[] pair : recs) {
            responseObserver.onNext(RecommendedEventProto.newBuilder()
                .setEventId(pair[0])
                .setScore(Double.longBitsToDouble(pair[1]))
                .build());
        }
        responseObserver.onCompleted();
    }

    @Override
    public void getSimilarEvents(SimilarEventsRequestProto request,
                                 StreamObserver<RecommendedEventProto> responseObserver) {
        log.debug("GetSimilarEvents eventId={}, userId={}", request.getEventId(), request.getUserId());
        List<long[]> similar = recommendationService.getSimilarEvents(
            request.getEventId(), request.getUserId(), request.getMaxResults());
        for (long[] pair : similar) {
            responseObserver.onNext(RecommendedEventProto.newBuilder()
                .setEventId(pair[0])
                .setScore(Double.longBitsToDouble(pair[1]))
                .build());
        }
        responseObserver.onCompleted();
    }

    @Override
    public void getInteractionsCount(InteractionsCountRequestProto request,
                                     StreamObserver<RecommendedEventProto> responseObserver) {
        log.debug("GetInteractionsCount for {} events", request.getEventIdCount());
        List<long[]> counts = recommendationService.getInteractionsCount(request.getEventIdList());
        for (long[] pair : counts) {
            responseObserver.onNext(RecommendedEventProto.newBuilder()
                .setEventId(pair[0])
                .setScore(Double.longBitsToDouble(pair[1]))
                .build());
        }
        responseObserver.onCompleted();
    }
}
