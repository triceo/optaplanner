/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.optaplanner.examples.common.experimental.impl;

import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiFunction;

import org.apache.commons.lang3.ObjectUtils;
import org.optaplanner.examples.common.experimental.api.ConsecutiveIntervalInfo;
import org.optaplanner.examples.common.experimental.api.IntervalBreak;
import org.optaplanner.examples.common.experimental.api.IntervalCluster;

public class ConsecutiveIntervalInfoImpl<Interval_, Point_ extends Comparable<Point_>, Difference_ extends Comparable<Difference_>>
        implements
        ConsecutiveIntervalInfo<Interval_, Point_, Difference_> {
    private final NavigableMap<IntervalSplitPoint<Interval_, Point_>, IntervalClusterImpl<Interval_, Point_, Difference_>> clusterStartSplitPointToCluster;
    private final NavigableSet<IntervalSplitPoint<Interval_, Point_>> splitPointSet;
    private final NavigableMap<IntervalSplitPoint<Interval_, Point_>, IntervalBreakImpl<Interval_, Point_, Difference_>> clusterStartSplitPointToNextBreak;
    private final Iterable<IntervalCluster<Interval_, Point_, Difference_>> intervalClusterIterable;
    private final BiFunction<Point_, Point_, Difference_> differenceFunction;
    private final Iterable<IntervalBreak<Interval_, Point_, Difference_>> breaksIterable;

    public ConsecutiveIntervalInfoImpl(TreeSet<IntervalSplitPoint<Interval_, Point_>> splitPointSet,
            BiFunction<Point_, Point_, Difference_> differenceFunction) {
        clusterStartSplitPointToCluster = new TreeMap<>();
        clusterStartSplitPointToNextBreak = new TreeMap<>();
        intervalClusterIterable = new MapValuesIterable<>(clusterStartSplitPointToCluster);
        breaksIterable = new MapValuesIterable<>(clusterStartSplitPointToNextBreak);
        this.splitPointSet = splitPointSet;
        this.differenceFunction = differenceFunction;
    }

    protected void addInterval(Interval<Interval_, Point_> interval) {
        NavigableMap<IntervalSplitPoint<Interval_, Point_>, IntervalClusterImpl<Interval_, Point_, Difference_>> intersectedIntervalClusterMap =
                clusterStartSplitPointToCluster.subMap(
                        ObjectUtils.defaultIfNull(clusterStartSplitPointToCluster.floorKey(interval.getStartSplitPoint()),
                                interval.getStartSplitPoint()),
                        true, interval.getEndSplitPoint(), true);

        // Case: the interval cluster before this interval does not intersect this interval
        if (!intersectedIntervalClusterMap.isEmpty()
                && intersectedIntervalClusterMap.firstEntry().getValue().getEndSplitPoint()
                        .isBefore(interval.getStartSplitPoint())) {
            // Get the tail map after the first cluster
            intersectedIntervalClusterMap = intersectedIntervalClusterMap.subMap(intersectedIntervalClusterMap.firstKey(),
                    false, intersectedIntervalClusterMap.lastKey(), true);
        }

        if (intersectedIntervalClusterMap.isEmpty()) {
            // Interval does not intersect anything
            // Ex:
            //     -----
            //----       -----
            createNewIntervalCluster(interval);
            return;
        }

        // Interval intersect at least one cluster
        // Ex:
        //      -----------------
        //  ------  ------  ---   ----
        IntervalClusterImpl<Interval_, Point_, Difference_> firstIntersectedIntervalCluster =
                intersectedIntervalClusterMap.firstEntry().getValue();
        IntervalSplitPoint<Interval_, Point_> oldStart = firstIntersectedIntervalCluster.getStartSplitPoint();
        firstIntersectedIntervalCluster.addInterval(interval);

        // Merge all the intersected interval clusters into the first intersected
        // interval cluster
        intersectedIntervalClusterMap.tailMap(oldStart, false).values()
                .forEach(firstIntersectedIntervalCluster::mergeIntervalCluster);

        // Remove all the intersected interval clusters after the first intersected
        // one, since they are now merged in the first
        intersectedIntervalClusterMap.tailMap(oldStart, false).clear();
        removeSpannedBreaksAndUpdateIntersectedBreaks(interval, firstIntersectedIntervalCluster);

        // If the first intersected interval cluster start after the interval,
        // we need to make the interval start point the key for this interval
        // cluster in the map
        if (oldStart.isAfter(firstIntersectedIntervalCluster.getStartSplitPoint())) {
            clusterStartSplitPointToCluster.remove(oldStart);
            clusterStartSplitPointToCluster.put(firstIntersectedIntervalCluster.getStartSplitPoint(),
                    firstIntersectedIntervalCluster);
        }
    }

    private void createNewIntervalCluster(Interval<Interval_, Point_> interval) {
        // Interval does not intersect anything
        // Ex:
        //     -----
        //----       -----
        IntervalSplitPoint<Interval_, Point_> start = splitPointSet.floor(interval.getStartSplitPoint());
        IntervalClusterImpl<Interval_, Point_, Difference_> newCluster =
                new IntervalClusterImpl<>(splitPointSet, differenceFunction, start);
        clusterStartSplitPointToCluster.put(start, newCluster);

        // If there a cluster after this interval, add a new break
        // between this interval and the next cluster
        Map.Entry<IntervalSplitPoint<Interval_, Point_>, IntervalClusterImpl<Interval_, Point_, Difference_>> nextClusterEntry =
                clusterStartSplitPointToCluster.higherEntry(start);
        if (nextClusterEntry != null) {
            IntervalClusterImpl<Interval_, Point_, Difference_> nextCluster = nextClusterEntry.getValue();
            Difference_ difference = differenceFunction.apply(newCluster.getEnd(), nextCluster.getStart());
            IntervalBreakImpl<Interval_, Point_, Difference_> newBreak =
                    new IntervalBreakImpl<>(newCluster, nextCluster, difference);
            clusterStartSplitPointToNextBreak.put(start, newBreak);
        }

        // If there a cluster before this interval, add a new break
        // between this interval and the previous cluster
        // (this will replace the old break, if there was one)
        Map.Entry<IntervalSplitPoint<Interval_, Point_>, IntervalClusterImpl<Interval_, Point_, Difference_>> previousClusterEntry =
                clusterStartSplitPointToCluster.lowerEntry(start);
        if (previousClusterEntry != null) {
            IntervalClusterImpl<Interval_, Point_, Difference_> previousCluster = previousClusterEntry.getValue();
            Difference_ difference = differenceFunction.apply(previousCluster.getEnd(), newCluster.getStart());
            IntervalBreakImpl<Interval_, Point_, Difference_> newBreak =
                    new IntervalBreakImpl<>(previousCluster, newCluster, difference);
            clusterStartSplitPointToNextBreak.put(previousClusterEntry.getKey(), newBreak);
        }
    }

    private void removeSpannedBreaksAndUpdateIntersectedBreaks(Interval<Interval_, Point_> interval,
            IntervalClusterImpl<Interval_, Point_, Difference_> intervalCluster) {
        NavigableMap<IntervalSplitPoint<Interval_, Point_>, IntervalBreakImpl<Interval_, Point_, Difference_>> intersectedIntervalBreakMap =
                clusterStartSplitPointToNextBreak.subMap(
                        ObjectUtils.defaultIfNull(clusterStartSplitPointToNextBreak.floorKey(interval.getStartSplitPoint()),
                                interval.getStartSplitPoint()),
                        true, interval.getEndSplitPoint(), true);

        if (intersectedIntervalBreakMap.isEmpty()) {
            return;
        }

        IntervalClusterImpl<Interval_, Point_, Difference_> clusterBeforeFirstIntersectedBreak =
                (IntervalClusterImpl<Interval_, Point_, Difference_>) (intersectedIntervalBreakMap.firstEntry().getValue()
                        .getPreviousIntervalCluster());
        IntervalClusterImpl<Interval_, Point_, Difference_> clusterAfterFinalIntersectedBreak =
                (IntervalClusterImpl<Interval_, Point_, Difference_>) (intersectedIntervalBreakMap.lastEntry().getValue()
                        .getNextIntervalCluster());

        // All breaks that are not the first or last intersected breaks will
        // be removed (as interval span them)
        if (!interval.getStartSplitPoint()
                .isAfter(clusterBeforeFirstIntersectedBreak.getEndSplitPoint())) {
            if (!interval.getEndSplitPoint().isBefore(clusterAfterFinalIntersectedBreak.getStartSplitPoint())) {
                // Case: interval spans all breaks
                // Ex:
                //   -----------
                //----  ------ -----
                intersectedIntervalBreakMap.clear();
            } else {
                // Case: interval span first break, but does not span the final break
                // Ex:
                //   -----------
                //----  ------   -----
                IntervalBreakImpl<Interval_, Point_, Difference_> finalBreak =
                        intersectedIntervalBreakMap.lastEntry().getValue();
                finalBreak.setPreviousCluster(intervalCluster);
                finalBreak.setLength(
                        differenceFunction.apply(finalBreak.getPreviousIntervalClusterEnd(),
                                finalBreak.getNextIntervalClusterStart()));
                intersectedIntervalBreakMap.clear();
                clusterStartSplitPointToNextBreak.put(intervalCluster.getStartSplitPoint(), finalBreak);
            }
        } else if (!interval.getEndSplitPoint().isBefore(clusterAfterFinalIntersectedBreak.getStartSplitPoint())) {
            // Case: interval span final break, but does not span the first break
            // Ex:
            //     -----------
            //----   -----   -----
            Map.Entry<IntervalSplitPoint<Interval_, Point_>, IntervalBreakImpl<Interval_, Point_, Difference_>> previousBreakEntry =
                    intersectedIntervalBreakMap.firstEntry();
            IntervalBreakImpl<Interval_, Point_, Difference_> previousBreak = previousBreakEntry.getValue();
            previousBreak.setNextCluster(intervalCluster);
            previousBreak.setLength(
                    differenceFunction.apply(previousBreak.getPreviousIntervalClusterEnd(), intervalCluster.getStart()));
            intersectedIntervalBreakMap.clear();
            clusterStartSplitPointToNextBreak
                    .put(((IntervalClusterImpl<Interval_, Point_, Difference_>) (previousBreak
                            .getPreviousIntervalCluster())).getStartSplitPoint(), previousBreak);
        } else {
            // Case: interval does not span either the first or final break
            // Ex:
            //     ---------
            //----  ------   -----
            IntervalBreakImpl<Interval_, Point_, Difference_> finalBreak =
                    intersectedIntervalBreakMap.lastEntry().getValue();
            finalBreak.setLength(
                    differenceFunction.apply(finalBreak.getPreviousIntervalClusterEnd(),
                            finalBreak.getNextIntervalClusterStart()));

            Map.Entry<IntervalSplitPoint<Interval_, Point_>, IntervalBreakImpl<Interval_, Point_, Difference_>> previousBreakEntry =
                    intersectedIntervalBreakMap.firstEntry();
            IntervalBreakImpl<Interval_, Point_, Difference_> previousBreak = previousBreakEntry.getValue();
            previousBreak.setNextCluster(intervalCluster);
            previousBreak.setLength(
                    differenceFunction.apply(previousBreak.getPreviousIntervalClusterEnd(), intervalCluster.getStart()));

            intersectedIntervalBreakMap.clear();
            clusterStartSplitPointToNextBreak.put(previousBreakEntry.getKey(), previousBreak);
            clusterStartSplitPointToNextBreak.put(intervalCluster.getStartSplitPoint(), finalBreak);
        }
    }

    protected void removeInterval(Interval<Interval_, Point_> interval) {
        Map.Entry<IntervalSplitPoint<Interval_, Point_>, IntervalClusterImpl<Interval_, Point_, Difference_>> intervalClusterEntry =
                clusterStartSplitPointToCluster.floorEntry(interval.getStartSplitPoint());
        IntervalClusterImpl<Interval_, Point_, Difference_> intervalCluster = intervalClusterEntry.getValue();
        clusterStartSplitPointToCluster.remove(intervalClusterEntry.getKey());
        Map.Entry<IntervalSplitPoint<Interval_, Point_>, IntervalBreakImpl<Interval_, Point_, Difference_>> previousBreakEntry =
                clusterStartSplitPointToNextBreak.lowerEntry(intervalClusterEntry.getKey());
        Map.Entry<IntervalSplitPoint<Interval_, Point_>, IntervalClusterImpl<Interval_, Point_, Difference_>> nextIntervalClusterEntry =
                clusterStartSplitPointToCluster.higherEntry(intervalClusterEntry.getKey());
        clusterStartSplitPointToNextBreak.remove(intervalClusterEntry.getKey());

        IntervalClusterImpl<Interval_, Point_, Difference_> previousIntervalCluster = null;
        IntervalBreakImpl<Interval_, Point_, Difference_> previousBreak = null;
        if (previousBreakEntry != null) {
            previousBreak = previousBreakEntry.getValue();
            previousIntervalCluster =
                    (IntervalClusterImpl<Interval_, Point_, Difference_>) previousBreak.getPreviousIntervalCluster();
        }
        for (IntervalClusterImpl<Interval_, Point_, Difference_> newIntervalCluster : intervalCluster
                .removeInterval(interval)) {
            if (previousBreak != null) {
                previousBreak.setNextCluster(newIntervalCluster);
                previousBreak.setLength(differenceFunction.apply(previousBreak.getPreviousIntervalCluster().getEnd(),
                        newIntervalCluster.getStart()));
                clusterStartSplitPointToNextBreak
                        .put(((IntervalClusterImpl<Interval_, Point_, Difference_>) previousBreak
                                .getPreviousIntervalCluster()).getStartSplitPoint(), previousBreak);
            }
            previousBreak = new IntervalBreakImpl<>(newIntervalCluster, null, null);
            previousIntervalCluster = newIntervalCluster;
            clusterStartSplitPointToCluster.put(newIntervalCluster.getStartSplitPoint(), newIntervalCluster);
        }

        if (nextIntervalClusterEntry != null && previousBreak != null) {
            previousBreak.setNextCluster(nextIntervalClusterEntry.getValue());
            previousBreak.setLength(differenceFunction.apply(previousIntervalCluster.getEnd(),
                    nextIntervalClusterEntry.getValue().getStart()));
            clusterStartSplitPointToNextBreak.put(previousIntervalCluster.getStartSplitPoint(),
                    previousBreak);
        } else if (previousBreakEntry != null && previousBreak == previousBreakEntry.getValue()) {
            // i.e. interval was the last interval in the cluster,
            // (previousBreak == previousBreakEntry.getValue()),
            // and there is no interval cluster after it
            // (previousBreak != null as previousBreakEntry != null,
            // so it must be the case nextIntervalClusterEntry == null)
            clusterStartSplitPointToNextBreak.remove(previousBreakEntry.getKey());
        }
    }

    @Override
    public Iterable<IntervalCluster<Interval_, Point_, Difference_>> getIntervalClusters() {
        return intervalClusterIterable;
    }

    @Override
    public Iterable<IntervalBreak<Interval_, Point_, Difference_>> getBreaks() {
        return breaksIterable;
    }

    @Override
    public String toString() {
        return "ConsecutiveIntervalData{" +
                "intervalClusters=" + intervalClusterIterable +
                ", breaks=" + breaksIterable +
                '}';
    }
}
