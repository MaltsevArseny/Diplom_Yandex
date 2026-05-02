package ru.practicum.ewm.stats.analyzer.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class EventSimilarityId implements Serializable {
    private Long eventA;
    private Long eventB;
}
