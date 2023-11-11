package ua.terra;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ListUtil {
    public static <T> List<List<T>> divideList(List<T> originalList, int numChunks) {
        if (originalList.isEmpty()) {
            return List.of(List.of());
        }

        int chunkSize = Math.max(1, originalList.size() / numChunks);

        return IntStream.range(0, originalList.size())
                .boxed()
                .collect(Collectors.groupingBy(index -> index / chunkSize))
                .values()
                .stream()
                .map(indices -> indices.stream().map(originalList::get).collect(Collectors.toList()))
                .collect(Collectors.toList());
    }


}
