import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class Main {

    public static void main(String[] args) {

        List<String> list = new ArrayList<>();
        list.add("Рома");
        list.add("Игорь");
        list.add("Вася");

        Stream<String> stream = list.stream();
        stream.forEach(System.out::println);
        stream.filter(s -> s.contains("Stream API"));
        stream.forEach(System.out::println);
    }
}
