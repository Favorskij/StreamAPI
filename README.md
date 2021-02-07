# 1. Stream Api

Stream — это объект для универсальной работы с данными. Мы указываем, какие операции хотим провести, при этом не заботясь о деталях реализации. Например, взять элементы из списка сотрудников, выбрать тех, кто младше 40 лет, отсортировать по фамилии и поместить в новый список. Или чуть сложнее, прочитать все json-файлы, находящиеся в папке books, десериализировать в список объектов книг, обработать элементы всех этих списков, а затем сгруппировать книги по автору.


Данные могут быть получены из источников, коими являются коллекции или методы, поставляющие данные. Например, список файлов, массив строк, метод range () для числовых промежутков и т.д. То есть, стрим использует существующие коллекции для получения новых элементов, это ни в коем случае не новая структура данных.
К данным затем применяются операторы. Например, взять лишь некоторые элементы (`filter`), преобразовать каждый элемент (`map`), посчитать сумму элементов или объединить всё в один объект (`reduce`).

![Stream API](/Stream.png)


Операторы можно разделить на две группы:
     
- Промежуточные (`intermediate`) — обрабатывают поступающие элементы и возвращают стрим. Промежуточных операторов в цепочке обработки элементов может быть много. 

- Терминальные (`terminal`) — обрабатывают элементы и завершают работу стрима, так что терминальный оператор в цепочке может быть только один.
     
# 2. Получение объекта Stream

Пока что хватит теории. Пришло время посмотреть, как создать или получить объект java.util.stream.Stream.
     
- Пустой стрим: `Stream.empty() // Stream <*String>`
- Стрим из List: `list.stream() // Stream <*String>`
- Стрим из Map: `map.entrySet().stream() // Stream<Map.Entry<String, String>>`
- Стрим из массива: `Arrays.stream(array) // Stream<String>`
- Стрим из указанных элементов: `Stream.of("a", "b", "c") // Stream<String>`


А вот и пример:

```java
public class Main {

    public static void main(String[] args) {

        String [] name = {"Рома", "Игорь", "Вася"};

        List<String> list = Arrays.stream(name)
                .filter(s -> s.length() <= 2)
                .collect(Collectors.toList());

        System.out.println(list);

    }

}
```

В данном примере источником служит метод `Arrays.stream`, который из  массива `name` делает стрим. Промежуточный оператор `filter` отбирает только те строки, длина которых не превышает два. Терминальный оператор `collect` собирает полученные элементы в новый список.


И ещё один пример:

```java
public class Main {

    public static void main(String[] args) {

        IntStream.of(120, 410, 85, 32, 314, 12)
                .filter(x -> x < 300)
                .map(x -> x + 11)
                .limit(3)
                .forEach(System.out::print);

    }

}
```

Здесь уже три промежуточных оператора:

- `filter — отбирает элементы, значение которых меньше 300,`
- `map — прибавляет 11 к каждому числу,`
- `limit — ограничивает количество элементов до 3.`


Терминальный оператор `forEach` применяет функцию `print` к каждому приходящему элементу.

[![Смотреть видео](https://img.youtube.com/vi/LKE-Qwx_v9U/hqdefault.jpg)](https://youtu.be/LKE-Qwx_v9U)

На ранних версиях Java этот пример выглядел бы так:

```java
public class Main {

    public static void main(String[] args) {

        int[] arr = {120, 410, 85, 32, 314, 12};
        int count = 0;
        for (int x : arr) {
            if (x >= 300) continue;
            x += 11;
            count++;
            if (count > 3) break;
            System.out.print(x);
        }

    }

}
```


С увеличением числа операторов код в ранних версиях усложнялся бы на порядок, не говоря уже о том, что разбить вычисления на несколько потоков при таком подходе было бы крайне непросто.

# 3. Как работает стрим

У стримов есть некоторые особенности. Во-первых, обработка не начнётся до тех пор, пока не будет вызван терминальный оператор. `list.stream().filter(x -> x > 100)`; не возьмёт ни единого элемента из списка. Во-вторых, стрим после обработки нельзя переиспользовать.

```java
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
```

Код на одинадцатой строке выполнится, а вот на двенадцатой выбросит исключение `java.lang.IllegalStateException:` stream has already been operated upon or closed.

Исходя из первой особенности, делаем вывод, что обработка происходит от терминального оператора к источнику. Это действительно так и это удобно. Мы можем в качестве источника использовать генерируемую бесконечную последовательность, скажем, факториала или чисел Фибоначчи, но обрабатывать лишь некоторую её часть.

[![Смотреть видео](https://img.youtube.com/vi/8YXZlQymitY/hqdefault.jpg)](https://youtu.be/8YXZlQymitY)


Пока мы не присоединили терминальный оператор, доступа к источнику не проводилось. Как только появился терминальный оператор `forEach`, он стал запрашивать элементы у стоящего перед ним оператора `limit`. Тот в свою очередь обращается к `map`, `map` к `filter`, а `filter` уже обращается к источнику. Затем элементы поступают в прямом порядке: источник, `filter`, `map`, `limit` и `forEach`.

Пока какой-либо из операторов не обработает элемент должным образом, новые запрошены не будут.

Как только через оператор `limit` прошло 3 элемента, он переходит в закрытое состояние и больше не будет запрашивать элементы у map. `forEach` запрашивает очередной элемент, но `limit` сообщает, что больше не может поставить элементов, поэтому forEach делает вывод, что элементы закончились и прекращает работу.

Такой подход зовётся pull `iteration`, то есть элементы запрашиваются у источника по мере надобности. К слову, в `RxJava` реализован push `iteration` подход, то есть источник сам уведомляет, что появились элементы и их нужно обработать.


# 4. Параллельные стримы

Стримы бывают последовательными `(sequential)` и параллельными `(parallel)`. Последовательные выполняются только в текущем потоке, а вот параллельные используют общий пул  [ForkJoinPool.commonPool().](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ForkJoinPool.html#commonPool--) При этом элементы разбиваются (если это возможно) на несколько групп и обрабатываются в каждом потоке отдельно. Затем на нужном этапе группы объединяются в одну для предоставления конечного результата.

Чтобы получить параллельный стрим, нужно либо вызвать метод `parallelStream()` вместо `stream()`, либо превратить обычный стрим в параллельный, вызвав промежуточный оператор `parallel`.

```java
public class Main {

    public static void main(String[] args) {

        List<Integer> list = new ArrayList<>();


        list.parallelStream()
                .filter(x -> x > 10)
                .map(x -> x * 2)
                .collect(Collectors.toList());

        IntStream.range(0, 10)
                .parallel()
                .map(x -> x * 10)
                .sum();


        System.out.println(list.size());

    }

}
```

Работа с потоконебезопасными коллекциями, разбиение элементов на части, создание потоков, объединение частей воедино, всё это кроется в реализации `Stream API`. От нас лишь требуется вызвать нужный метод и проследить, чтобы функции в операторах не зависели от каких-либо внешних факторов, иначе есть риск получить неверный результат или ошибку.


Вот так делать нельзя:


```java
public class Main {

    public static void main(String[] args) {

        final List<Integer> ints = new ArrayList<>();
        IntStream.range(0, 1000000)
                .parallel()
                .forEach(i -> ints.add(i));
        System.out.println(ints.size());


        System.out.println(ints.size());

    }

}
```

Это код Шрёдингера. Он может нормально выполниться и показать `1000000`, может выполниться и показать `869877`, а может и упасть с ошибкой `Exception in thread "main" java.lang.ArrayIndexOutOfBoundsException: 332 at java.util.ArrayList.add(ArrayList.java:459).`

Поэтому разработчики настоятельно просят воздержаться от побочных эффектов в лямбдах, то тут, то там говоря в документации о невмешательстве **(non-interference)**.


# 5. Стримы для примитивов

Кроме объектных стримов `Stream<T>`, существуют специальные стримы для примитивных типов:

- `IntStream` для `int,`
- `LongStream` для `long,`
- `DoubleStream` для `double.`

Для `boolean`, `byte`, `short` и `char` специальных стримов не придумали, но вместо них можно использовать `IntStream`, а затем приводить к нужному типу. Для `float` тоже придётся воспользоваться `DoubleStream`.

Примитивные стримы полезны, так как не нужно тратить время на боксинг/анбоксинг, к тому же у них есть ряд специальных операторов, упрощающих жизнь. Их мы рассмотрим очень скоро.

# 6. Операторы Stream API

Дальше приводятся операторы `Stream API` с описанием, демонстрацией и примерами. Можете использовать это как справочник.

# 6.1. Источники

**empty()**

Стрим, как и коллекция, может быть пустым, а значит всем последующем операторам нечего будет обрабатывать.

```java
public class Main {

    public static void main(String[] args) {

        Stream.empty()
                .forEach(System.out::println);

        // Вывода нет

    }

}
```

**of(T value)**

**of(T... values)**

Стрим для одного или нескольких перечисленных элементов. Очень часто вижу, что используют такую конструкцию:

```java
public class Main {

    public static void main(String[] args) {

        Arrays.asList(1, 2, 3).stream()
                .forEach(System.out::println);

    }

}
```

однако она излишня. Вот так проще:

```java
public class Main {

    public static void main(String[] args) {

        Stream.of(1, 2, 3)
                .forEach(System.out::println);

    }

}
```

**ofNullable(T t)**

Появился в Java 9. Возвращает пустой стрим, если в качестве аргумента передан null, в противном случае, возвращает стрим из одного элемента.

```java
public class Main {

    public static void main(String[] args) {

        String str = Math.random() > 0.5 ? "I'm feeling lucky" : null;
        Stream.ofNullable(str)
                .forEach(System.out::println);

    }

}
```

**generate(Supplier s)**

Возвращает стрим с бесконечной последовательностью элементов, генерируемых функцией `Supplier s`.

```java
public class Main {

    public static void main(String[] args) {

        Stream.generate(() -> 6)
                .limit(6)
                .forEach(System.out::println);

        // 6, 6, 6, 6, 6, 6

    }

}
```

[![Смотреть видео](https://img.youtube.com/vi/4S12MsOsLAQ/hqdefault.jpg)](https://youtu.be/4S12MsOsLAQ)

Поскольку стрим бесконечный, нужно его ограничивать или осторожно использовать, дабы не попасть в бесконечный цикл.


**iterate​(T seed, UnaryOperator f)**

Возвращает бесконечный стрим с элементами, которые образуются в результате последовательного применения функции `f` к итерируемому значению. Первым элементом будет `seed`, затем `f(seed)`, затем `f(f(seed))` и так далее.

```java
public class Main {

    public static void main(String[] args) {

        Stream.iterate(2, x -> x + 6)
                .limit(6)
                .forEach(System.out::println);

        // 2, 8, 14, 20, 26, 32

    }

}
```

[![Смотреть видео](https://img.youtube.com/vi/A1NL5KFG7fs/hqdefault.jpg)](https://youtu.be/A1NL5KFG7fs)

```java
public class Main {

    public static void main(String[] args) {

        Stream.iterate(1, x -> x * 2)
                .limit(6)
                .forEach(System.out::println);
        
        // 1, 2, 4, 8, 16, 32

    }

}
```

**iterate​(T seed, Predicate hasNext, UnaryOperator f)**

Появился в Java 9. Всё то же самое, только добавляется ещё один аргумент `hasNext:` если он возвращает `false`, то стрим завершается. Это очень похоже на цикл `for`:

```java
public class Main {

    public static void main(String[] args) {

        // for (i = seed; hasNext(i); i = f(i)) {}

    }

}
```

Таким образом, с помощью `iterate` теперь можно создать конечный стрим.

```java
public class Main {

    public static void main(String[] args) {

        Stream.iterate(2, x -> x < 25, x -> x + 6)
                .forEach(System.out::println);

        // 2, 8, 14, 20

    }

}
```

[![Смотреть видео](https://img.youtube.com/vi/yhqWBraORRQ/hqdefault.jpg)](https://youtu.be/yhqWBraORRQ)


```java
public class Main {

    public static void main(String[] args) {

        Stream.iterate(4, x -> x < 100, x -> x * 4)
                .forEach(System.out::println);

        // 4, 16, 64

    }

}
```

**concat(Stream a, Stream b)**

Объединяет два стрима так, что вначале идут элементы стрима `A`, а по его окончанию последуют элементы стрима `B`.

```java
public class Main {

    public static void main(String[] args) {

        Stream.concat(
                Stream.of(1, 2, 3),
                Stream.of(4, 5, 6))
                .forEach(System.out::println);

        // 1, 2, 3, 4, 5, 6

    }

}
```

[![Смотреть видео](https://img.youtube.com/vi/iUBaDmwUynM/hqdefault.jpg)](https://youtu.be/iUBaDmwUynM)

```java
public class Main {

    public static void main(String[] args) {

        Stream.concat(
                Stream.of(10),
                Stream.of(4, 16))
                .forEach(System.out::println);
        // 10, 4, 16

    }

}
```

**builder()**

Создаёт мутабельный объект для добавления элементов в стрим без использования какого-либо контейнера для этого.

```java
public class Main {

    public static void main(String[] args) {

        Stream.Builder<Integer> streamBuider = Stream.<Integer>builder()
                .add(0)
                .add(1);
        for (int i = 2; i <= 8; i += 2) {
            streamBuider.accept(i);
        }
        streamBuider
                .add(9)
                .add(10)
                .build()
                .forEach(System.out::println);

        // 0, 1, 2, 4, 6, 8, 9, 10

    }

}
```

- `IntStream.range​(int startInclusive, int endExclusive)`
- `LongStream.range​(long startInclusive, long endExclusive)`

Создаёт стрим из числового промежутка `[start..end]`, то есть от `start` (включительно) по `end`.

```java
public class Main {

    public static void main(String[] args) {

        IntStream.range(0, 10)
                .forEach(System.out::println);

        // 0, 1, 2, 3, 4, 5, 6, 7, 8, 9

    }

}
```

```java
public class Main {

    public static void main(String[] args) {

        LongStream.range(-10L, -5L)
                .forEach(System.out::println);

        // -10, -9, -8, -7, -6

    }

}
```

- `IntStream.rangeClosed​(int startInclusive, int endInclusive)`
- `LongStream.range​Closed(long startInclusive, long endInclusive)`

Создаёт стрим из числового промежутка `[start..end]`, то есть от `start` (включительно) по `end` (включительно).

```java
public class Main {

    public static void main(String[] args) {

        IntStream.rangeClosed(0, 5)
                .forEach(System.out::println);

        // 0, 1, 2, 3, 4, 5

    }

}
```

```java
public class Main {

    public static void main(String[] args) {

        LongStream.range(-8L, -5L)
                .forEach(System.out::println);

        // -8, -7, -6, -5

    }

}
```

# 6.2. Промежуточные операторы

**filter​(Predicate predicate)**

Фильтрует стрим, принимая только те элементы, которые удовлетворяют заданному условию.

```java
public class Main {

    public static void main(String[] args) {

        Stream.of(1, 2, 3)
                .filter(x -> x == 10)
                .forEach(System.out::print);
        
        // Вывода нет, так как после фильтрации стрим станет пустым

    }

}
```

```java
public class Main {

    public static void main(String[] args) {

        Stream.of(120, 410, 85, 32, 314, 12)
                .filter(x -> x > 100)
                .forEach(System.out::println);

        // 120, 410, 314

    }

}
```

[![Смотреть видео](https://img.youtube.com/vi/UGNYvJUpIj4/hqdefault.jpg)](https://youtu.be/UGNYvJUpIj4)

```java
public class Main {

    public static void main(String[] args) {

        IntStream.range(2, 9)
                .filter(x -> x % 3 == 0)
                .forEach(System.out::println);

        // 3, 6

    }

}
```

**map​(Function mapper)**

Применяет функцию к каждому элементу и затем возвращает стрим, в котором элементами будут результаты функции. map можно применять для изменения типа элементов.

- `Stream.mapToDouble​(ToDoubleFunction mapper)`
- `Stream.mapToInt​(ToIntFunction mapper)`
- `Stream.mapToLong​(ToLongFunction mapper)`
- `IntStream.mapToObj(IntFunction mapper)`
- `IntStream.mapToLong(IntToLongFunction mapper)`
- `IntStream.mapToDouble(IntToDoubleFunction mapper)`

Специальные операторы для преобразования объектного стрима в примитивный, примитивного в объектный, либо примитивного стрима одного типа в примитивный стрим другого.

```java
public class Main {

    public static void main(String[] args) {

        Stream.of("3", "4", "5")
                .map(Integer::parseInt)
                .map(x -> x + 10)
                .forEach(System.out::println);

        // 13, 14, 15

    }

}
```

```java
public class Main {

    public static void main(String[] args) {

        Stream.of(120, 410, 85, 32, 314, 12)
                .map(x -> x + 11)
                .forEach(System.out::println);

        // 131, 421, 96, 43, 325, 23

    }

}
```

[![Смотреть видео](https://img.youtube.com/vi/pYev_wpi0QM/hqdefault.jpg)](https://youtu.be/pYev_wpi0QM)


```java
public class Main {

    public static void main(String[] args) {

        Stream.of("10", "11", "32")
                .map(x -> Integer.parseInt(x, 16))
                .forEach(System.out::println);

        // 16, 17, 50

    }

}
```

**flatMap​(Function<T, Stream<R>> mapper)**

Один из самых интересных операторов. Работает как map, но с одним отличием — можно преобразовать один элемент в ноль, один или множество других.

- `flatMapToDouble​(Function mapper)`
- `flatMapToInt​(Function mapper)`
- `flatMapToLong​(Function mapper)`

Как и в случае с map, служат для преобразования в примитивный стрим.

Для того, чтобы один элемент преобразовать в ноль элементов, нужно вернуть `null`, либо пустой стрим. Чтобы преобразовать в один элемент, нужно вернуть стрим из одного элемента, например, через `Stream.of(x)`. Для возвращения нескольких элементов, можно любыми способами создать стрим с этими элементами.

```java
public class Main {

    public static void main(String[] args) {

        Stream.of(2, 3, 0, 1, 3)
                .flatMapToInt(x -> IntStream.range(0, x))
                .forEach(System.out::println);

        // 0, 1, 0, 1, 2, 0, 0, 1, 2

    }

}
```

[![Смотреть видео](https://img.youtube.com/vi/WA89tJvIG6c/hqdefault.jpg)](https://youtu.be/WA89tJvIG6c)


```java
public class Main {

    public static void main(String[] args) {

        Stream.of(1, 2, 3, 4, 5, 6)
                .flatMap(x -> {
                    switch (x % 3) {
                        case 0:
                            return Stream.of(x, x * x, x * x * 2);
                        case 1:
                            return Stream.of(x);
                        case 2:
                        default:
                            return Stream.empty();
                    }
                })
                .forEach(System.out::println);

        // 1, 3, 9, 18, 4, 6, 36, 72

    }

}
```

**limit​(long maxSize)**

Ограничивает стрим maxSize элементами.

```java
public class Main {

    public static void main(String[] args) {

        Stream.of(120, 410, 85, 32, 314, 12)
                .limit(4)
                .forEach(System.out::println);

        // 120, 410, 85, 32

    }

}
```

[![Смотреть видео](https://img.youtube.com/vi/nImdufy_9zA/hqdefault.jpg)](https://youtu.be/nImdufy_9zA)

```java
public class Main {

    public static void main(String[] args) {

        Stream.of(120, 410, 85, 32, 314, 12)
                .limit(2).limit(5)
                .forEach(System.out::println);

        // 120, 410

        Stream.of(19)
                .limit(0)
                .forEach(System.out::println);

        // Вывода нет

    }

}
```

**skip​(long n)**

Пропускает n элементов стрима.

```java
public class Main {

    public static void main(String[] args) {

        Stream.of(5, 10)
                .skip(40)
                .forEach(System.out::println);

        // Вывода нет

        Stream.of(120, 410, 85, 32, 314, 12)
                .skip(2)
                .forEach(System.out::println);

        // 85, 32, 314, 12

    }

}
```

[![Смотреть видео](https://img.youtube.com/vi/lbv7bH8yoFY/hqdefault.jpg)](https://youtu.be/lbv7bH8yoFY)

```java
public class Main {

    public static void main(String[] args) {

        IntStream.range(0, 10)
                .limit(5)
                .skip(3)
                .forEach(System.out::println);

        // 3, 4

        IntStream.range(0, 10)
                .skip(5)
                .limit(3)
                .skip(1)
                .forEach(System.out::println);

        // 6, 7
        
    }

}
```

**sorted​()**
**sorted​(Comparator comparator)**

Сортирует элементы стрима. Причём работает этот оператор очень хитро: если стрим уже помечен как отсортированный, то сортировка проводиться не будет, иначе соберёт все элементы, отсортирует их и вернёт новый стрим, помеченный как отсортированный. См. [9.1.](#9.1)

```java
public class Main {

    public static void main(String[] args) {

        IntStream.range(0, 100000000)
                .sorted()
                .limit(3)
                .forEach(System.out::println);

        // 0, 1, 2

        IntStream.concat(
                IntStream.range(0, 100000000),
                IntStream.of(-1, -2))
                .sorted()
                .limit(3)
                .forEach(System.out::println);

        // Exception in thread "main" java.lang.OutOfMemoryError: Java heap space

        Stream.of(120, 410, 85, 32, 314, 12)
                .sorted()
                .forEach(System.out::println);

        // 12, 32, 85, 120, 314, 410

    }

}
```

[![Смотреть видео](https://img.youtube.com/vi/3k8V1P4dyhU/hqdefault.jpg)](https://youtu.be/3k8V1P4dyhU)

```java
public class Main {

    public static void main(String[] args) {

        Stream.of(120, 410, 85, 32, 314, 12)
                .sorted(Comparator.reverseOrder())
                .forEach(System.out::println);

        // 410, 314, 120, 85, 32, 12

    }

}
```

**distinct​()**

Убирает повторяющиеся элементы и возвращаем стрим с уникальными элементами. Как и в случае с sorted, смотрит, состоит ли уже стрим из уникальных элементов и если это не так, отбирает уникальные и помечает стрим как содержащий уникальные элементы.

```java
public class Main {

    public static void main(String[] args) {

        Stream.of(2, 1, 8, 1, 3, 2)
                .distinct()
                .forEach(System.out::println);

        // 2, 1, 8, 3

    }

}
```

[![Смотреть видео](https://img.youtube.com/vi/VCU3EKlEqRY/hqdefault.jpg)](https://youtu.be/VCU3EKlEqRY)

```java
public class Main {

    public static void main(String[] args) {

        IntStream.concat(
                IntStream.range(2, 5),
                IntStream.range(0, 4))
                .distinct()
                .forEach(System.out::println);

        // 2, 3, 4, 0, 1

    }

}
```

**peek​(Consumer action)**

Выполняет действие над каждым элементом стрима и при этом возвращает стрим с элементами исходного стрима. Служит для того, чтобы передать элемент куда-нибудь, не разрывая при этом цепочку операторов (вы же помните, что forEach — терминальный оператор и после него стрим завершается?), либо для отладки.

```java
public class Main {

    public static void main(String[] args) {

        Stream.of(0, 3, 0, 0, 5)
                .peek(x -> System.out.format("before distinct: %d%n", x))
                .distinct()
                .peek(x -> System.out.format("after distinct: %d%n", x))
                .map(x -> x * x)
                .forEach(x -> System.out.format("after map: %d%n", x));

                // before distinct: 0
                // after distinct: 0
                // after map: 0
                // before distinct: 3
                // after distinct: 3
                // after map: 9
                // before distinct: 1
                // after distinct: 1
                // after map: 1
                // before distinct: 5
                // before distinct: 0
                // before distinct: 5
                // after distinct: 5
                // after map: 25

    }

}
```

[![Смотреть видео](https://img.youtube.com/vi/HfXmFdItuqY/hqdefault.jpg)](https://youtu.be/HfXmFdItuqY)

**takeWhile​(Predicate predicate)**

Появился в Java 9. Возвращает элементы до тех пор, пока они удовлетворяют условию, то есть функция-предикат возвращает true. Это как limit, только не с числом, а с условием.

```java
public class Main {

    public static void main(String[] args) {

        Stream.of(1, 2, 3, 4, 2, 5)
                .takeWhile(x -> x < 3)
                .forEach(System.out::println);

        // 1, 2

    }

}
```

[![Смотреть видео](https://img.youtube.com/vi/VUTFCex_J3s/hqdefault.jpg)](https://youtu.be/VUTFCex_J3s)

```java
public class Main {

    public static void main(String[] args) {

        IntStream.range(2, 7)
                .takeWhile(x -> x != 5)
                .forEach(System.out::println);

        // 2, 3, 4

    }

}
```

**dropWhile​(Predicate predicate)**

Появился в Java 9. Пропускает элементы до тех пор, пока они удовлетворяют условию, затем возвращает оставшуюся часть стрима. Если предикат вернул для первого элемента false, то ни единого элемента не будет пропущено. Оператор подобен skip, только работает по условию.

```java
public class Main {

    public static void main(String[] args) {

        Stream.of(1, 2, 3, 4, 2, 5)
                .dropWhile(x -> x >= 3)
                .forEach(System.out::println);

        // 1, 2, 3, 4, 2, 5

        System.out.println("========");

        Stream.of(1, 2, 3, 4, 2, 5)
                .dropWhile(x -> x < 3)
                .forEach(System.out::println);

        // 3, 4, 2, 5

    }

}
```

[![Смотреть видео](https://img.youtube.com/vi/Ij2P4tbEtSM/hqdefault.jpg)](https://youtu.be/Ij2P4tbEtSM)

```java
public class Main {

    public static void main(String[] args) {

        IntStream.range(2, 7)
                .dropWhile(x -> x < 5)
                .forEach(System.out::println);

        // 5, 6

        System.out.println("=======");

        IntStream.of(1, 3, 2, 0, 5, 4)
                .dropWhile(x -> x % 2 == 1)
                .forEach(System.out::println);

        // 2, 0, 5, 6

    }

}
```

**boxed()**

Преобразует примитивный стрим в объектный.

```java
public class Main {

    public static void main(String[] args) {

        DoubleStream.of(0.1, Math.PI)
                .boxed()
                .map(Object::getClass)
                .forEach(System.out::println);

        // class java.lang.Double
        // class java.lang.Double

    }

}
```

**6.3. Терминальные операторы**

**void forEach​(Consumer action)**

Выполняет указанное действие для каждого элемента стрима.

```java
public class Main {

    public static void main(String[] args) {

        Stream.of(120, 410, 85, 32, 314, 12)
                .forEach(x -> System.out.format("%s, ", x));

        // 120, 410, 85, 32, 314, 12

    }

}
```

[![Смотреть видео](https://img.youtube.com/vi/SPqfPxDmMpg/hqdefault.jpg)](https://youtu.be/SPqfPxDmMpg)

**void forEachOrdered​(Consumer action)**

Тоже выполняет указанное действие для каждого элемента стрима, но перед этим добивается правильного порядка вхождения элементов. Используется для параллельных стримов, когда нужно получить правильную последовательность элементов.

```java
public class Main {

    public static void main(String[] args) {

        IntStream.range(0, 100000)
                .parallel()
                .filter(x -> x % 10000 == 0)
                .map(x -> x / 10000)
                .forEach(System.out::println);

        // 5, 6, 7, 3, 4, 8, 0, 9, 1, 2

        System.out.println("=======");

        IntStream.range(0, 100000)
                .parallel()
                .filter(x -> x % 10000 == 0)
                .map(x -> x / 10000)
                .forEachOrdered(System.out::println);

        // 0, 1, 2, 3, 4, 5, 6, 7, 8, 9

    }

}
```

**long count​()**

Возвращает количество элементов стрима.

```java
public class Main {

    public static void main(String[] args) {

        long count = IntStream.range(0, 10)
                .flatMap(x -> IntStream.range(0, x))
                .count();
        System.out.println(count);

        // 45

        System.out.println(
                IntStream.rangeClosed(-3, 6)
                        .count()
        );

        // 10

        System.out.println(
                Stream.of(0, 2, 9, 13, 5, 11)
                        .map(x -> x * 2)
        .filter(x -> x % 2 == 1)
                .count()
        );

        // 0


    }

}
```

**R collect​(Collector collector)**

Один из самых мощных операторов `Stream API`. С его помощью можно собрать все элементы в список, множество или другую коллекцию, сгруппировать элементы по какому-нибудь критерию, объединить всё в строку и т.д.. В классе `java.util.stream.Collectors` очень много методов на все случаи жизни, мы рассмотрим их позже. При желании можно [написать свой коллектор](#collector-implementation), реализовав интерфейс `Collector`.

```java
public class Main {

    public static void main(String[] args) {

        List<Integer> list = Stream.of(1, 2, 3)
                .collect(Collectors.toList());

        System.out.println(list);

        // list: [1, 2, 3]

        String s = Stream.of(1, 2, 3)
                .map(String::valueOf)
                .collect(Collectors.joining("-", "<", ">"));

        System.out.println(s);

        // s: "<1-2-3>"

    }

}
```

**R collect​(Supplier supplier, BiConsumer accumulator, BiConsumer combiner)**

То же, что и collect(collector), только параметры разбиты для удобства. Если нужно быстро сделать какую-то операцию, нет нужды реализовывать интерфейс Collector, достаточно передать три лямбда-выражения.

supplier должен поставлять новые объекты (контейнеры), например new ArrayList(), accumulator добавляет элемент в контейнер, combiner необходим для параллельных стримов и объединяет части стрима воедино.

```java
public class Main {

    public static void main(String[] args) {

        List<String> list = Stream.of("a", "b", "c", "d")
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        System.out.println(list);

        // list: ["a", "b", "c", "d"]

    }

}
```

**Object[] toArray​()**</br>
Возвращает нетипизированный массив с элементами стрима.

`A[] toArray​(IntFunction<A[]> generator)`
Аналогично, только возвращает типизированный массив.

```java
public class Main {

    public static void main(String[] args) {

        String[] elements = Stream.of("a", "b", "c", "d")
                .toArray(String[]::new);

        // elements: ["a", "b", "c", "d"]

        System.out.println(Arrays.toString(elements));

    }

}
```



**T reduce​(T identity, BinaryOperator accumulator)**</br>
**U reduce​(U identity, BiFunction accumulator, BinaryOperator combiner)**</br>
Ещё один полезный оператор. Позволяет преобразовать все элементы стрима в один объект. Например, посчитать сумму всех элементов, либо найти минимальный элемент.

Сперва берётся объект identity и первый элемент стрима, применяется функция accumulator и identity становится её результатом. Затем всё продолжается для остальных элементов.

```java
public class Main {

    public static void main(String[] args) {

        int sum = Stream.of(1, 2, 3, 4, 5)
                .reduce(10, (acc, x) -> acc + x);

        // sum: 25

        System.out.println(sum);

    }

}
```

[![Смотреть видео](https://img.youtube.com/vi/Uq4UbhAnsDM/hqdefault.jpg)](https://youtu.be/Uq4UbhAnsDM)

**Optional reduce​(BinaryOperator accumulator)**</br>
Этот метод отличается тем, что у него нет начального объекта `identity`. В качестве него служит первый элемент стрима. Поскольку стрим может быть пустой и тогда identity объект не присвоится, то результатом функции служит `Optional`, позволяющий обработать и эту ситуацию, вернув `Optional.empty()`.

```java
public class Main {

    public static void main(String[] args) {

        Optional<Integer> result = Stream.<Integer>empty()
                .reduce((acc, x) -> acc + x);
        System.out.println(result.isPresent());

        // false

        Optional<Integer> sum = Stream.of(1, 2, 3, 4, 5)
                .reduce((acc, x) -> acc + x);
        System.out.println(sum.get());

        // 15

    }

}
```

[![Смотреть видео](https://img.youtube.com/vi/bcJtpA-W_Wc/hqdefault.jpg)](https://youtu.be/bcJtpA-W_Wc)

```java
public class Main {

    public static void main(String[] args) {

        int sum = IntStream.of(2, 4, 6, 8)
                .reduce(5, (acc, x) -> acc + x);

        // sum: 25

        System.out.println(sum);

        int product = IntStream.range(0, 10)
                .filter(x -> x++ % 4 == 0)
                .reduce((acc, x) -> acc * x)
                .getAsInt();

        // product:0

        System.out.println(product);

    }

}
```

**Optional min​(Comparator comparator)**</br>
**Optional max​(Comparator comparator)**</br>
Поиск `минимального/максимального` элемента, основываясь на переданном компараторе. Внутри вызывается reduce:

```
reduce((a, b) -> comparator.compare(a, b) <= 0 ? a : b));
reduce((a, b) -> comparator.compare(a, b) >= 0 ? a : b));
```

```java
public class Main {

    public static void main(String[] args){

        int min = Stream.of(20, 11, 45, 78, 13)
                .min(Integer::compare).get();

        // min: 11

        System.out.println(min);

        int max = Stream.of(20, 11, 45, 78, 13)
                .max(Integer::compare).get();

        // max: 78

        System.out.println(max);

    }

}
```

**Optional findAny​()**</br>
Возвращает первый попавшийся элемент стрима. В параллельных стримах это может быть действительно любой элемент, который лежал в разбитой части последовательности.

**Optional findFirst​()**</br>
Гарантированно возвращает первый элемент стрима, даже если стрим параллельный.

Если нужен любой элемент, то для параллельных стримов быстрее будет работать `findAny()`.

```java
public class Main {

    public static void main(String[] args){

        int anySeq = IntStream.range(4, 65536)
                .findAny()
                .getAsInt();

        // anySeq: 4

        System.out.println(anySeq);



        int firstSeq = IntStream.range(4, 65536)
                .findFirst()
                .getAsInt();

        // firstSeq: 4

        System.out.println(firstSeq);



        int anyParallel = IntStream.range(4, 65536)
                .parallel()
                .findAny()
                .getAsInt();

        // anyParallel: 40961

        System.out.println(anyParallel);



        int firstParallel = IntStream.range(4, 65536)
                .parallel()
                .findFirst()
                .getAsInt();

        // firstParallel: 4

        System.out.println(firstParallel);

    }

}
```

**boolean allMatch​(Predicate predicate)**

Возвращает `true`, если все элементы стрима удовлетворяют условию `predicate`. Если встречается какой-либо элемент, для которого результат вызова функции-предиката будет `false`, то оператор перестаёт просматривать элементы и возвращает `false`.

```java
public class Main {

    public static void main(String[] args){

        boolean result = Stream.of(1, 2, 3, 4, 5)
                .allMatch(x -> x <= 7);
        
        // result: true

        System.out.println(result);

    }

}
```

[![Смотреть видео](https://img.youtube.com/vi/195UodMSdls/hqdefault.jpg)](https://youtu.be/195UodMSdls)

```java
public class Main {

    public static void main(String[] args){

        boolean result = Stream.of(1, 2, 3, 4, 5)
                .allMatch(x -> x < 3);

        // result: false

        System.out.println(result);

    }

}
```

[![Смотреть видео](https://img.youtube.com/vi/rJ5pa_OlGQw/hqdefault.jpg)](https://youtu.be/rJ5pa_OlGQw)

```java
public class Main {

    public static void main(String[] args){

        boolean result = Stream.of(120, 410, 85, 32, 314, 12)
                .allMatch(x -> x % 2 == 0);

        // result: false


        System.out.println(result);

    }

}
```

**boolean anyMatch​(Predicate predicate)**

Возвращает `true`, если хотя бы один элемент стрима удовлетворяет условию predicate. Если такой элемент встретился, нет смысла продолжать перебор элементов, поэтому сразу возвращается результат.

```java
public class Main {

    public static void main(String[] args){

        boolean result = Stream.of(1, 2, 3, 4, 5)
                .anyMatch(x -> x == 3);
        

        // result: true


        System.out.println(result);

    }

}
```

[![Смотреть видео](https://img.youtube.com/vi/a0T5H8Ssr84/hqdefault.jpg)](https://youtu.be/a0T5H8Ssr84)

```java
public class Main {

    public static void main(String[] args){

        boolean result = Stream.of(1, 2, 3, 4, 5)
                .anyMatch(x -> x == 8);


        // result: false


        System.out.println(result);

    }

}
```

[![Смотреть видео](https://img.youtube.com/vi/OllxzWu---Q/hqdefault.jpg)](https://youtu.be/OllxzWu---Q)

```java
public class Main {

    public static void main(String[] args){

        boolean result = Stream.of(120, 410, 85, 32, 314, 12)
                .anyMatch(x -> x % 22 == 0);


        // result: false


        System.out.println(result);

    }

}
```

**boolean noneMatch​(Predicate predicate)**

Возвращает `true`, если, пройдя все элементы стрима, ни один не удовлетворил условию `predicate`. Если встречается какой-либо элемент, для которого результат вызова функции-предиката будет true, то оператор перестаёт перебирать элементы и возвращает `false`.

```java
public class Main {

    public static void main(String[] args){

        boolean result = Stream.of(1, 2, 3, 4, 5)
                .noneMatch(x -> x == 9);


        // result: true


        System.out.println(result);

    }

}
```

[![Смотреть видео](https://img.youtube.com/vi/l1choJNBCK0/hqdefault.jpg)](https://youtu.be/l1choJNBCK0)

```java
public class Main {

    public static void main(String[] args){

        boolean result = Stream.of(1, 2, 3, 4, 5)
                .noneMatch(x -> x == 3);


        // result: false


        System.out.println(result);

    }

}
```

[![Смотреть видео](https://img.youtube.com/vi/YWlHDqHUiTQ/hqdefault.jpg)](https://youtu.be/YWlHDqHUiTQ)

```java
public class Main {

    public static void main(String[] args){

        boolean result = Stream.of(120, 410, 86, 32, 314, 12)
                .noneMatch(x -> x % 2 == 1);


        // result: true


        System.out.println(result);

    }

}
```

**OptionalDouble average​()**

Только для примитивных стримов. Возвращает среднее арифметическое всех элементов. Либо `Optional.empty`, если стрим пуст.

```java
public class Main {

    public static void main(String[] args){

        double result = IntStream.range(2, 16)
                .average()
                .getAsDouble();


        // result: 8.5


        System.out.println(result);

    }

}
```

**sum()**

Возвращает сумму элементов примитивного стрима. Для `IntStream` результат будет типа `int`, для `LongStream` — `long`, для `DoubleStream` — `double`.

```java
public class Main {

    public static void main(String[] args){

        long result = LongStream.range(2, 16)
                .sum();


        // result: 119
        
        // 2+3+4+5+6 и так далее до 16


        System.out.println(result);

    }

}
```

**IntSummaryStatistics summaryStatistics()**

Полезный метод примитивных стримов. Позволяет собрать статистику о числовой последовательности стрима, а именно: количество элементов, их сумму, среднее арифметическое, минимальный и максимальный элемент.

```java
public class Main {

    public static void main(String[] args){

        LongSummaryStatistics stats = LongStream.range(2, 16)
                .summaryStatistics();
        System.out.format("  count: %d%n", stats.getCount());
        System.out.format("    sum: %d%n", stats.getSum());
        System.out.format("average: %.1f%n", stats.getAverage());
        System.out.format("    min: %d%n", stats.getMin());
        System.out.format("    max: %d%n", stats.getMax());

        //   count: 14
        //     sum: 119
        // average: 8,5
        //     min: 2
        //     max: 15


        System.out.println(stats);

    }

}
```

**7. Методы Collectors**

**toList​()**

Самый распространённый метод. Собирает элементы в List.

```java
public class Main {

    public static void main(String[] args){

        List<String> phones = new ArrayList<String>();
        Collections.addAll(phones, "iPhone 8", "HTC U12", "Huawei Nexus 6P", "LG G6");

        List<String> filteredPhones = phones.stream()
                .filter(s->s.length()<10)
                .collect(Collectors.toList());

        for(String s : filteredPhones){
            System.out.println(s);
        }

    }

}
```

-----------

**toSet​()**

Собирает элементы в множество.

```java
public class Main {

    public static void main(String[] args){

        List<String> phones = new ArrayList<String>();
        Collections.addAll(phones, "iPhone 8", "HTC U12", "Huawei Nexus 6P", "LG G6");

        Set<String> filteredPhones = phones.stream()
                .filter(s->s.length()<10)
                .collect(Collectors.toSet());

        for(String s : filteredPhones){
            System.out.println(s);
        }

    }

}
```

-----------

**toCollection​(Supplier collectionFactory)**

Собирает элементы в заданную коллекцию. Если нужно конкретно указать, какой `List`, `Set` или другую коллекцию мы хотим использовать, то этот метод поможет.

</br>**Deque**</br>
```java
public class Main {

    public static void main(String[] args){

        Deque<Integer> deque = Stream.of(1, 2, 3, 4, 5)
                .collect(Collectors.toCollection(ArrayDeque::new));

        for(Integer s : deque){
            System.out.println(s);
        }

        // 1, 2, 3, 4, 5
    }

}
```

</br>**Set**</br>
```java
public class Main {

    public static void main(String[] args){

        Set<Integer> set = Stream.of(1, 2, 3, 4, 5)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        for(Integer s : set){
            System.out.println(s);
        }

        // 1, 2, 3, 4, 5
    }

}
```

**toMap​(Function keyMapper, Function valueMapper)**

Собирает элементы в Map. Каждый элемент преобразовывается в ключ и в значение, основываясь на результате функций keyMapper и valueMapper соответственно. Если нужно вернуть тот же элемент, что и пришел, то можно передать `Function.identity()`.

```java
public class Main {

    public static void main(String[] args){

        Map<Integer, Integer> map1 = Stream.of(1, 2, 3, 4, 5)
                .collect(Collectors.toMap(
                        Function.identity(),
                        Function.identity()
                ));

        System.out.println(map1);

        // {1=1, 2=2, 3=3, 4=4, 5=5}


        Map<Integer, String> map2 = Stream.of(1, 2, 3)
                .collect(Collectors.toMap(
                        Function.identity(),
                        i -> String.format("%d * 2 = %d", i, i * 2)
                ));

        System.out.println(map2);

        // {1="1 * 2 = 2", 2="2 * 2 = 4", 3="3 * 2 = 6"}


        Map<Character, String> map3 = Stream.of(50, 54, 55)
                .collect(Collectors.toMap(
                        i -> (char) i.intValue(),
                        i -> String.format("<%d>", i)
                ));

        System.out.println(map3);

        // {'2'="<50>", '6'="<54>", '7'="<55>"}

    }

}
```

**toMap​(Function keyMapper, Function valueMapper, BinaryOperator mergeFunction)**

Аналогичен первой версии метода, только в случае, когда встречается два одинаковых ключа, позволяет объединить значения.

```java
public class Main {

    public static void main(String[] args){

        Map<Integer, String> map4 = Stream.of(50, 55, 69, 20, 19, 52)
                .collect(Collectors.toMap(
                        i -> i % 5,
                        i -> String.format("<%d>", i),
                        (a, b) -> String.join(", ", a, b)
                ));

        System.out.println(map4);

        // {0="<50>, <55>, <20>", 2="<52>", 4="<64>, <19>"}

    }

}
```
В данном случае, для чисел `50`, `55` и `20`, ключ одинаков и равен 0, поэтому значения накапливаются. Для 64 и 19 аналогично.

**toMap​(Function keyMapper, Function valueMapper, BinaryOperator mergeFunction, Supplier mapFactory)**

Всё то же, только позволяет указывать, какой именно класс Map использовать.

```java
public class Main {

    public static void main(String[] args){

        Map<Integer, String> map5 = Stream.of(50, 55, 69, 20, 19, 52)
                .collect(Collectors.toMap(
                        i -> i % 5,
                        i -> String.format("<%d>", i),
                        (a, b) -> String.join(", ", a, b),
                        LinkedHashMap::new
                ));

        System.out.println(map5);

        // {0=<50>, <55>, <20>, 4=<69>, <19>, 2=<52>}

    }

}
```
Отличие этого примера от предыдущего в том, что теперь сохраняется порядок, благодаря `LinkedHashList`.

**toConcurrentMap​(Function keyMapper, Function valueMapper)**</br>
**toConcurrentMap​(Function keyMapper, Function valueMapper, BinaryOperator mergeFunction)**</br>
**toConcurrentMap​(Function keyMapper, Function valueMapper, BinaryOperator mergeFunction, Supplier mapFactory)**
  
Всё то же самое, что и toMap, только работаем с ConcurrentMap.

```java
public class Main {

    public static void main(String[] args){

        ConcurrentMap<Integer, Integer> map1 = Stream.of(1, 2, 3, 4, 5)
                .collect(Collectors.toConcurrentMap(
                        Function.identity(),
                        Function.identity()
                ));

        System.out.println(map1);

        // {1=1, 2=2, 3=3, 4=4, 5=5}


        ConcurrentMap<Integer, String> map2 = Stream.of(1, 2, 3)
                .collect(Collectors.toConcurrentMap(
                        Function.identity(),
                        i -> String.format("%d * 2 = %d", i, i * 2)
                ));

        System.out.println(map2);

        // {1="1 * 2 = 2", 2="2 * 2 = 4", 3="3 * 2 = 6"}


        ConcurrentMap<Character, String> map3 = Stream.of(50, 54, 55)
                .collect(Collectors.toConcurrentMap(
                        i -> (char) i.intValue(),
                        i -> String.format("<%d>", i)
                ));

        System.out.println(map3);

        // {'2'="<50>", '6'="<54>", '7'="<55>"}

    }

}
```

-----------

**collectingAndThen​(Collector downstream, Function finisher)**

Собирает элементы с помощью указанного коллектора, а потом применяет к полученному результату функцию.

```java
public class Main {

    public static void main(String[] args){

        List<Integer> list = Stream.of(1, 2, 3, 4, 5)
                .collect(Collectors.collectingAndThen(
                        Collectors.toList(),
                        Collections::unmodifiableList));

        System.out.println(list.getClass());
        System.out.println();

        // class java.util.Collections$UnmodifiableRandomAccessList


        List<String> list2 = Stream.of("a", "b", "c", "d")
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(Function.identity(), s -> s + s),
                        map -> map.entrySet().stream()))
                .map(Object::toString)
                .collect(Collectors.collectingAndThen(
                        Collectors.toList(),
                        Collections::unmodifiableList));
        
        list2.forEach(System.out::println);

        // a=aa
        // b=bb
        // c=cc
        // d=dd

    }

}
```

**joining​()**</br>
**joining​(CharSequence delimiter)**</br>
**joining​(CharSequence delimiter, CharSequence prefix, CharSequence suffix)**

Собирает элементы, реализующие интерфейс CharSequence, в единую строку. Дополнительно можно указать разделитель, а также префикс и суффикс для всей последовательности.

```java
public class Main {

    public static void main(String[] args){

        String s1 = Stream.of("a", "b", "c", "d")
                .collect(Collectors.joining());

        System.out.println(s1);

        // abcd


        String s2 = Stream.of("a", "b", "c", "d")
                .collect(Collectors.joining("-"));

        System.out.println(s2);

        // a-b-c-d


        String s3 = Stream.of("a", "b", "c", "d")
                .collect(Collectors.joining(" -> ", "[ ", " ]"));

        System.out.println(s3);
        
        // [ a -> b -> c -> d ]

    }

}
```

**summingInt​(ToIntFunction mapper)**</br>
**summingLong​(ToLongFunction mapper)**</br>
**summingDouble​(ToDoubleFunction mapper)**

Коллектор, который преобразовывает объекты в `int/long/double` и подсчитывает сумму.

```java
public class Main {

    public static void main(String[] args){

        Integer sum1 = Stream.of("1", "2", "3", "4")
                .collect(Collectors.summingInt(Integer::parseInt));

        System.out.println(sum1);

        // 10


        Long sum2 = Stream.of("1", "2", "3", "4")
                .collect(Collectors.summingLong(Integer::parseInt));

        System.out.println(sum2);

        // 10


        Double sum3 = Stream.of("1", "2", "3", "4")
                .collect(Collectors.summingDouble(Integer::parseInt));

        System.out.println(sum3);

        // 10.0

    }

}
```

**averagingInt​(ToIntFunction mapper)**</br>
**averagingLong​(ToLongFunction mapper)**</br>
**averagingDouble​(ToDoubleFunction mapper)**

Аналогично, но со средним значением.

```java
public class Main {

    public static void main(String[] args){

        Double average1 = Stream.of("1", "2", "3", "4")
                .collect(Collectors.averagingInt(Integer::parseInt));

        System.out.println(average1);

        // 2.5


        Double average2 = Stream.of("1", "2", "3", "4")
                .collect(Collectors.averagingLong(Integer::parseInt));

        System.out.println(average2);

        // 2.5


        Double average3 = Stream.of("1", "2", "3", "4")
                .collect(Collectors.averagingDouble(Integer::parseInt));

        System.out.println(average3);

        // 2.5


    }

}
```

**summarizingInt​(ToIntFunction mapper)**</br>
**summarizingLong​(ToLongFunction mapper)**</br>
**summarizingDouble​(ToDoubleFunction mapper)**

Аналогично, но с полной статистикой.

```java
public class Main {

    public static void main(String[] args){

        IntSummaryStatistics stats1 = Stream.of("1", "2", "3", "4")
                .collect(Collectors.summarizingInt(Integer::parseInt));

        System.out.println(stats1);

        // IntSummaryStatistics{count=4, sum=10, min=1, average=2,500000, max=4}


        LongSummaryStatistics stats2 = Stream.of("1", "2", "3", "4")
                .collect(Collectors.summarizingLong(Long::parseLong));

        System.out.println(stats2);

        // LongSummaryStatistics{count=4, sum=10, min=1, average=2,500000, max=4}


        DoubleSummaryStatistics stats3 = Stream.of("1.1", "2.34", "3.14", "4.04")
                .collect(Collectors.summarizingDouble(Double::parseDouble));

        System.out.println(stats3);

        // DoubleSummaryStatistics{count=4, sum=10,620000, min=1,100000, average=2,655000, max=4,040000}

    }

}
```

Все эти методы и несколько последующих, зачастую используются в качестве составных коллекторов для группировки или `collectingAndThen`. В том виде, в котором они показаны на примерах используются редко. Я лишь показываю пример, что они возвращают, чтобы было понятнее.

-----------

**counting​()**

Подсчитывает количество элементов.

```java
public class Main {

    public static void main(String[] args){

        Long count = Stream.of("1", "2", "3", "4")
                .collect(Collectors.counting());

        System.out.println(count);

        // 2.5

    }

}
```

**filtering​(Predicate predicate, Collector downstream)**</br>
**mapping​(Function mapper, Collector downstream)**</br>
**flatMapping​(Function downstream)**</br>
**reducing​(BinaryOperator op)**</br>
**reducing​(T identity, BinaryOperator op)**</br>
**reducing​(U identity, Function mapper, BinaryOperator op)**

Специальная группа коллекторов, которая применяет операции `filter`, `map`, `flatMap` и `reduce`. `filtering`​ и `flatMapping​` появились в **Java 9**.

```java
public class Main {

    public static void main(String[] args){

        List<Integer> ints = Stream.of(1, 2, 3, 4, 5, 6)
                .collect(Collectors.filtering(
                        x -> x % 2 == 0,
                        Collectors.toList()));

        System.out.println(ints);

        // 2, 4, 6


        String s1 = Stream.of(1, 2, 3, 4, 5, 6)
                .collect(Collectors.filtering(
                        x -> x % 2 == 0,
                        Collectors.mapping(
                                x -> Integer.toString(x),
                                Collectors.joining("-")
                        )
                ));

        System.out.println(s1);

        // 2-4-6


        String s2 = Stream.of(2, 0, 1, 3, 2)
                .collect(Collectors.flatMapping(
                        x -> IntStream.range(0, x).mapToObj(Integer::toString),
                        Collectors.joining(", ")
                ));

        System.out.println(s2);

        // 0, 1, 0, 0, 1, 2, 0, 1


        int value = Stream.of(1, 2, 3, 4, 5, 6)
                .collect(Collectors.reducing(
                        0, (a, b) -> a + b
                ));

        System.out.println(value);

        // 21


        String s3 = Stream.of(1, 2, 3, 4, 5, 6)
                .collect(Collectors.reducing(
                        "", x -> Integer.toString(x), (a, b) -> a + b
                ));

        System.out.println(s3);

        // 123456

    }

}
```

**minBy​(Comparator comparator)**</br>
**maxBy​(Comparator comparator)**

Поиск `минимального/максимального` элемента, основываясь на заданном компараторе.

```java
public class Main {

    public static void main(String[] args){

        Optional<String> min = Stream.of("ab", "c", "defgh", "ijk", "l")
                .collect(Collectors.minBy(Comparator.comparing(String::length)));

        min.ifPresent(System.out::println);

        // c


        Optional<String> max = Stream.of("ab", "c", "defgh", "ijk", "l")
                .collect(Collectors.maxBy(Comparator.comparing(String::length)));

        max.ifPresent(System.out::println);
        // defgh

    }

}
```

**groupingBy​(Function classifier)**</br>
**groupingBy​(Function classifier, Collector downstream)**</br>
**groupingBy​(Function classifier, Supplier mapFactory, Collector downstream)**

Группирует элементы по критерию, сохраняя результат в `Map`. Вместе с представленными выше агрегирующими коллекторами, позволяет гибко собирать данные. Подробнее о комбинировании в разделе [Примеры](#examples).

**groupingByConcurrent​(Function classifier)**</br>
**groupingByConcurrent​(Function classifier, Collector downstream)**</br>
**groupingByConcurrent​(Function classifier, Supplier mapFactory, Collector downstream)**

Аналогичный набор методов, только сохраняет в ConcurrentMap.

```java
public class Main {

    public static void main(String[] args) {

        Map<Integer, List<String>> map1 = Stream.of(
                "ab", "c", "def", "gh", "ijk", "l", "mnop")
                .collect(Collectors.groupingBy(String::length));

        map1.entrySet().forEach(System.out::println);
        System.out.println();

        // 1=[c, l]
        // 2=[ab, gh]
        // 3=[def, ijk]
        // 4=[mnop]


        Map<Integer, String> map2 = Stream.of(
                "ab", "c", "def", "gh", "ijk", "l", "mnop")
                .collect(Collectors.groupingBy(
                        String::length,
                        Collectors.mapping(
                                String::toUpperCase,
                                Collectors.joining())
                ));

        map2.entrySet().forEach(System.out::println);
        System.out.println();

        // 1=CL
        // 2=ABGH
        // 3=DEFIJK
        // 4=MNOP


        Map<Integer, List<String>> map3 = Stream.of(
                "ab", "c", "def", "gh", "ijk", "l", "mnop")
                .collect(Collectors.groupingBy(
                        String::length,
                        LinkedHashMap::new,
                        Collectors.mapping(
                                String::toUpperCase,
                                Collectors.toList())
                ));

        map3.entrySet().forEach(System.out::println);

        // 2=[AB, GH]
        // 1=[C, L]
        // 3=[DEF, IJK]
        // 4=[MNOP]

    }

}
```

**partitioningBy​(Predicate predicate)**</br>
**partitioningBy​(Predicate predicate, Collector downstream)**

Ещё один интересный метод. Разбивает последовательность элементов по какому-либо критерию. В одну часть попадают все элементы, которые удовлетворяют переданному условию, во вторую — все, которые не удовлетворяют.

```java
public class Main {

    public static void main(String[] args) {

        Map<Boolean, List<String>> map1 = Stream.of(
                "ab", "c", "def", "gh", "ijk", "l", "mnop")
                .collect(Collectors.partitioningBy(s -> s.length() <= 2));

        map1.entrySet().forEach(System.out::println);
        System.out.println();

        // false=[def, ijk, mnop]
        // true=[ab, c, gh, l]


        Map<Boolean, String> map2 = Stream.of(
                "ab", "c", "def", "gh", "ijk", "l", "mnop")
                .collect(Collectors.partitioningBy(
                        s -> s.length() <= 2,
                        Collectors.mapping(
                                String::toUpperCase,
                                Collectors.joining())
                ));

        map2.entrySet().forEach(System.out::println);

        // false=DEFIJKMNOP
        // true=ABCGHL

    }

}
```

**8. Collector**

Интерфейс `java.util.stream.Collector` служит для сбора элементов стрима в некоторый мутабельный контейнер. Он состоит из таких методов:

- **Supplier<A> supplier()** — функция, которая создаёт экземпляры контейнеров.

- **BiConsumer<A,T> accumulator()** — функция, которая кладёт новый элемент в контейнер.

- **BinaryOperator<A> combiner()** — функция, которая объединяет два контейнера в один. В параллельных стримах каждая часть может собираться в отдельный экземпляр контейнера и в итоге необходимо их объединять в один результирующий.

- **Function<A,R> finisher()** — функция, которая преобразовывает весь контейнер в конечный результат. Например, можно обернуть `List` в `Collections.unmodifiableList`.

- **Set<Characteristics> characteristics()** — возвращает характеристики коллектора, чтобы внутренняя реализация знала, с чем имеет дело. Например, можно указать, что коллектор поддерживает многопоточность.

**Характеристики:**

- **CONCURRENT** — коллектор поддерживает многопоточность, а значит отдельные части стрима могут быть успешно положены в контейнер из другого потока.

- **UNORDERED** — коллектор не зависит от порядка поступаемых элементов.

- **IDENTITY_FINISH** — функция `finish()` имеет стандартную реализацию `Function.identity()`, а значит её можно не вызывать.

<a name="collector-implementation">**8.1. Реализация собственного коллектора**</a>

Прежде чем писать свой коллектор, нужно убедиться, что задачу нельзя решить при помощи комбинации стандартных коллекторов.</br></br>

К примеру, если нужно собрать лишь уникальные элементы в список, то можно собрать элементы сначала в `LinkedHashSet`, чтобы сохранился порядок, а потом все элементы добавить в `ArrayList`. Комбинация `collectingAndThen` с `toCollection` и функцией, передающей полученный `Set` в конструктор `ArrayList`, делает то, что задумано:

```java
public class Main {

    public static void main(String[] args) {

        Stream.of(1, 2, 3, 1, 9, 2, 5, 3, 4, 8, 2)
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(LinkedHashSet::new),
                        ArrayList::new)).forEach(System.out::println);

        // 1 2 3 9 5 4 8

    }

}
```

А вот если задача состоит в том, чтобы собрать уникальные элементы в одну часть, а повторяющиеся в другую, например в `Map<Boolean, List>`, то при помощи `partitioningBy` получится не очень красиво:

```java
public class Main {

    public static void main(String[] args) {

        final Set<Integer> elements = new HashSet<>();
        Stream.of(1, 2, 3, 1, 9, 2, 5, 3, 4, 8, 2)
                .collect(Collectors.partitioningBy(elements::add))
                .forEach((isUnique, list) -> System.out.format("%s: %s%n", isUnique ? "unique" : "repetitive", list));

        // repetitive: [1, 2, 3, 2]
        // unique: [1, 2, 3, 9, 5, 4, 8]

    }

}
```

Здесь приходится создавать `Set` и в предикате коллектора его использовать, что нежелательно. Можно превратить лямбду в анонимную функцию, но это ещё хуже:

```java
public class Main {

    public static void main(String[] args) {

        new Predicate<Integer>() {
            final Set<Integer> elements = new HashSet<>();
            @Override
            public boolean test(Integer t) {
                return elements.add(t);
            }
        };

    }

}
```

Для создания своего коллектора есть два пути:</br></br>
1. Создать класс, реализующий интерфейс `Collector`.</br></br>
2. Воспользоваться фабрикой `Collector.of`.</br></br>
Если нужно сделать что-то универсальное, чтобы работало для любых типов, то есть использовать дженерики, то во втором варианте можно просто сделать статическую функцию, а внутри использовать `Collector.of`.</br></br>

Вот полученный коллектор.

```java
public class Main {

    public static <T> Collector<T, ?, Map<Boolean, List<T>>> partitioningByUniqueness() {
        return Collector.<T, Map.Entry<List<T>, Set<T>>, Map<Boolean, List<T>>>of(
                () -> new AbstractMap.SimpleImmutableEntry<>(
                        new ArrayList<T>(), new LinkedHashSet<>()),
                (c, e) -> {
                    if (!c.getValue().add(e)) {
                        c.getKey().add(e);
                    }
                },
                (c1, c2) -> {
                    c1.getKey().addAll(c2.getKey());
                    for (T e : c2.getValue()) {
                        if (!c1.getValue().add(e)) {
                            c1.getKey().add(e);
                        }
                    }
                    return c1;
                },
                c -> {
                    Map<Boolean, List<T>> result = new HashMap<>(2);
                    result.put(Boolean.FALSE, c.getKey());
                    result.put(Boolean.TRUE, new ArrayList<>(c.getValue()));
                    return result;
                });
    }

}
```

Давайте теперь разбираться.</br>

Интерфейс `Collector` объявлен так:</br>

**interface Collector<T, A, R>**</br>

**T** - тип входных элементов.</br>
**A** - тип контейнера, в который будут поступать элементы.</br>
**R** - тип результата.

Сигнатура метода, возвращающего коллектор такова:</br>

**public static <T> Collector<T, ?, Map<Boolean, List<T>>> partitioningByUniqueness()**

Он принимает элементы типа `T`, возвращает `Map<Boolean, List<T>>`, как и `partitioningBy`. Знак вопроса (джокер) в среднем параметре говорит о том, что внутренний тип реализации для публичного `API` не важен. Многие методы класса `Collectors` содержат джокер в качестве типа контейнера.</br>

**return Collector.<T, Map.Entry<List<T>, Set<T>>, Map<Boolean, List<T>>>of**

Вот здесь уже пришлось указать тип контейнера. Так как в **Java** нет класса `Pair` или `Tuple`, то два разных типа можно положить в `Map.Entry`.


```
// supplier
() -> new AbstractMap.SimpleImmutableEntry<>(
        new ArrayList<>(), new LinkedHashSet<>())
```

Контейнером будет `AbstractMap.SimpleImmutableEntry`. В ключе будет список повторяющихся элементов, в значении — множество с уникальными элементами.

```
// accumulator
(c, e) -> {
    if (!c.getValue().add(e)) {
        c.getKey().add(e);
    }
}
```

Здесь всё просто. Если элемент нельзя добавить во множество (по причине того, что там уже есть такой элемент), то добавляем его в список повторяющихся элементов.

```
// combiner
(c1, c2) -> {
    c1.getKey().addAll(c2.getKey());
    for (T e : c2.getValue()) {
        if (!c1.getValue().add(e)) {
            c1.getKey().add(e);
        }
    }
    return c1;
}
```

Нужно объединить два `Map.Entry`. Списки повторяющихся элементов можно объединить вместе, а вот с уникальными элементами так просто не выйдет — нужно пройтись поэлементно и повторить всё то, что делалось в функции-аккумуляторе. Кстати, лямбду-аккумулятор можно присвоить переменной и тогда цикл можно превратить в `c2.getValue().forEach(e -> accumulator.accept(c1, e));`

```
// finisher
c -> {
    Map<Boolean, List<T>> result = new HashMap<>(2);
    result.put(Boolean.FALSE, c.getKey());
    result.put(Boolean.TRUE, new ArrayList<>(c.getValue()));
    return result;
}
```

Наконец, возвращаем необходимый результат. В `map.get(Boolean.TRUE)` будут уникальные, а в `map.get(Boolean.FALSE)` — повторяющиеся элементы.

```
Map<Boolean, List<Integer>> map;
map = Stream.of(1, 2, 3, 1, 9, 2, 5, 3, 4, 8, 2)
    .collect(partitioningByUniqueness());
// {false=[1, 2, 3, 2], true=[1, 2, 3, 9, 5, 4, 8]}
```

</br>Хорошей практикой является создание коллекторов, которые принимают ещё один коллектор и зависят от него. Например, можно будет складывать элементы не только в `List`, но и в любую другую коллекцию `Collectors.toCollection`, либо в строку `Collectors.joining`.

```java
public class Main {

    public static <T, D, A> Collector<T, ?, Map<Boolean, D>> partitioningByUniqueness(
            Collector<? super T, A, D> downstream) {
        class Holder<A, B> {
            final A unique, repetitive;
            final B set;
            Holder(A unique, A repetitive, B set) {
                this.unique = unique;
                this.repetitive = repetitive;
                this.set = set;
            }
        }
        BiConsumer<A, ? super T> downstreamAccumulator = downstream.accumulator();
        BinaryOperator<A> downstreamCombiner = downstream.combiner();
        BiConsumer<Holder<A, Set<T>>, T> accumulator = (t, element) -> {
            A container = t.set.add(element) ? t.unique : t.repetitive;
            downstreamAccumulator.accept(container, element);
        };
        return Collector.<T, Holder<A, Set<T>>, Map<Boolean, D>>of(
                () -> new Holder<>(
                        downstream.supplier().get(),
                        downstream.supplier().get(),
                        new HashSet<>() ),
                accumulator,
                (t1, t2) -> {
                    downstreamCombiner.apply(t1.repetitive, t2.repetitive);
                    t2.set.forEach(e -> accumulator.accept(t1, e));
                    return t1;
                },
                t -> {
                    Map<Boolean, D> result = new HashMap<>(2);
                    result.put(Boolean.FALSE, downstream.finisher().apply(t.repetitive));
                    result.put(Boolean.TRUE, downstream.finisher().apply(t.unique));
                    t.set.clear();
                    return result;
                });
    }

}
```

Алгоритм остался тем же, только теперь уже нельзя во второй контейнер сразу же складывать уникальные элементы, приходится создавать новый `set`. Для удобства также добавлен класс **Holder**, который хранит два контейнера для уникальных и повторяющихся элементов, а также само множество.

Все операции теперь нужно проводить через переданный коллектор, именуемый `downstream`. Именно он сможет поставить контейнер нужного типа `downstream.supplier().get()`, добавить элемент в этот контейнер `downstream.accumulator().accept(container, element)`, объединить контейнеры и создать окончательный результат.

```java
public class Main {

    public static <T, D, A> Collector<T, ?, Map<Boolean, D>> partitioningByUniqueness(
            Collector<? super T, A, D> downstream) {
        class Holder<A, B> {
            final A unique, repetitive;
            final B set;
            Holder(A unique, A repetitive, B set) {
                this.unique = unique;
                this.repetitive = repetitive;
                this.set = set;
            }
        }
        BiConsumer<A, ? super T> downstreamAccumulator = downstream.accumulator();
        BinaryOperator<A> downstreamCombiner = downstream.combiner();
        BiConsumer<Holder<A, Set<T>>, T> accumulator = (t, element) -> {
            A container = t.set.add(element) ? t.unique : t.repetitive;
            downstreamAccumulator.accept(container, element);
        };
        return Collector.<T, Holder<A, Set<T>>, Map<Boolean, D>>of(
                () -> new Holder<>(
                        downstream.supplier().get(),
                        downstream.supplier().get(),
                        new HashSet<>() ),
                accumulator,
                (t1, t2) -> {
                    downstreamCombiner.apply(t1.repetitive, t2.repetitive);
                    t2.set.forEach(e -> accumulator.accept(t1, e));
                    return t1;
                },
                t -> {
                    Map<Boolean, D> result = new HashMap<>(2);
                    result.put(Boolean.FALSE, downstream.finisher().apply(t.repetitive));
                    result.put(Boolean.TRUE, downstream.finisher().apply(t.unique));
                    t.set.clear();
                    return result;
                });
    }

    public static void main(String[] args) {

        Stream.of(1, 2, 3, 1, 9, 2, 5, 3, 4, 8, 2)
                .map(String::valueOf)
                .collect(partitioningByUniqueness(Collectors.joining("-")))
                .forEach((isUnique, str) -> System.out.format("%s: %s%n", isUnique ? "unique" : "repetitive", str));

        // repetitive: 1-2-3-2
        // unique: 1-2-3-9-5-4-8

    }

}
```

Кстати, первую реализацию метода без аргументов можно теперь заменить на:

```
public static <T> Collector<T, ?, Map<Boolean, List<T>>> partitioningByUniqueness() {
    return partitioningByUniqueness(Collectors.toList());
}
```

**9. Spliterator**

Пришло время немного углубиться в работу `Stream API` изнутри. Элементы стримов нужно не только итерировать, но ещё и разделять на части и отдавать другим потокам. За итерацию и разбиение отвечает `Spliterator`. Он даже звучит как `Iterator`, только с приставкой `Split` — разделять.

Методы интерфейса:</br>

- **trySplit** — как следует из названия, пытается разделить элементы на две части. Если это сделать не получается, либо элементов недостаточно для разделения, то вернёт `null`. В остальных случаях возвращает ещё один `Spliterator` с частью данных.

- **tryAdvance​(Consumer action)** — если имеются элементы, для которых можно применить действие, то оно применяется и возвращает `true`, в противном случае возвращается `false`, но действие не выполняется.

- **estimateSize()** — возвращает примерное количество элементов, оставшихся для обработки, либо `Long.MAX_VALUE`, если стрим бесконечный или посчитать количество невозможно.

- **characteristics()** — возвращает характеристики сплитератора.

<a name="9.1">**9.1. Характеристики**</a>

В методе `sorted` и `distinct` было упомянуто, что если стрим помечен как отсортированный или содержащий уникальные элементы, то соответствующие операции проводиться не будут. Вот характеристики сплитератора и влияют на это.</br>

- **DISTINCT** — все элементы уникальны. Сплитераторы всех реализаций `Set` содержат эту характеристику.

- **SORTED** — все элементы отсортированы.

- **ORDERED** — порядок имеет значение. Сплитераторы большинства коллекций содержат эту характеристику, а `HashSet`, к примеру, нет.

- **SIZED** — количество элементов точно известно.

- **SUBSIZED** — количество элементов каждой разбитой части точно известно.

- **NONNULL** — в элементах не встречается `null`. Некоторые коллекции из `java.util.concurrent`, в которые нельзя положить `null`, содержат эту характеристику.

- **IMMUTABLE** — источник является иммутабельным и в него нельзя больше добавить элементов, либо удалить их.

- **CONCURRENT** — источник лоялен к любым изменениям.

Разумеется, характеристики могут быть изменены при выполнении цепочки операторов. Например, после `sorted` добавляется характеристика `SORTED`, после `filter` теряется `SIZED` и т.д.

**9.2. Жизненный цикл сплитератора**

Чтобы понять когда и как сплитератор вызывает тот или иной метод, давайте создадим обёртку, которая логирует все вызовы. Чтобы из сплитератора создать стрим, используется класс `StreamSupport`.

```java
public final class SpliteratorWrapper<T> implements Spliterator<T> {

    public static void main(String[] args) {
        final boolean parallel = true;
        Map<String, Spliterator<Integer>> input = new LinkedHashMap<>();
        input.put("List",
                Arrays.asList(0, 1, 2, 3).spliterator());
        input.put("Set",
                IntStream.range(0, 4)
                        .boxed()
                        .collect(Collectors.toSet())
                        .spliterator());
        input.put("Arrays.spliterator",
                Arrays.spliterator(new int[] {0, 1, 2, 3}));
        input.put("Stream.of",
                Stream.of(0, 1, 2, 3).spliterator());
        input.put("Stream.of + distinct",
                Stream.of(0, 1, 2, 3).distinct().spliterator());
        input.put("Stream.of + distinct + map",
                Stream.of(0, 1, 2, 3).distinct().map(x -> x + 1).spliterator());

        input.forEach((name, s) -> {
            counter = 1;
            System.out.println(name);
            final List<Row> rows = new CopyOnWriteArrayList<>();
            Spliterator<Integer> spliterator = new SpliteratorWrapper<>(s, rows);
            long count = StreamSupport.stream(spliterator, parallel)
                    .count();
            rows.stream()
                    .sorted(Comparator.comparing(Row::time))
                    .forEach(System.out::println);
            System.out.print("count: ");
            System.out.println(count);
            System.out.println();
        });
    }

    private static final Map<Integer, String> NAMES;
    static {
        NAMES = new HashMap<>();
        NAMES.put(Spliterator.ORDERED, "ORDERED");
        NAMES.put(Spliterator.SORTED, "SORTED");
        NAMES.put(Spliterator.DISTINCT, "DISTINCT");
        NAMES.put(Spliterator.SIZED, "SIZED");
        NAMES.put(Spliterator.SUBSIZED, "SUBSIZED");
        NAMES.put(Spliterator.NONNULL, "NONNULL");
        NAMES.put(Spliterator.IMMUTABLE, "IMMUTABLE");
        NAMES.put(Spliterator.CONCURRENT, "CONCURRENT");
    }

    private static int counter;

    public static class Row {
        String tag;
        String threadName;
        String methodName;
        String info;
        long time;

        public long time() {
            return time;
        }

        @Override
        public String toString() {
            return String.format("[%s][%s] %s%s", threadName, tag, methodName, info);
        }
    }

    private final String tag;
    private final Spliterator<T> spliterator;
    private final List<Row> rows;

    public SpliteratorWrapper(Spliterator<T> spliterator, List<Row> rows) {
        this(spliterator, rows, "main");
    }

    public SpliteratorWrapper(Spliterator<T> spliterator, List<Row> rows, String tag) {
        this.tag = tag;
        this.spliterator = spliterator;
        this.rows = rows;
    }

    private Row createRow(String methodName) {
        final Row row = new Row();
        row.threadName = Thread.currentThread().getName();
        row.methodName = methodName;
        row.tag = this.tag;
        return row;
    }

    @Override
    public boolean tryAdvance(Consumer<? super T> action) {
        final Row row = createRow("tryAdvance");
        boolean result = spliterator.tryAdvance(action);
        row.info = ": " + result;
        rows.add(row);
        return result;
    }

    @Override
    public Spliterator<T> trySplit() {
        final Row row = createRow("trySplit");
        Spliterator<T> result = spliterator.trySplit();
        if (result == null) {
            row.info = ": null";
        } else {
            row.info = String.format("%n  %s%n", result.toString());
            row.info += String.format("  size: %d%n", result.estimateSize());
            row.info += String.format("  characteristics: %s", getCharacteristics(result.characteristics()));
            result = new SpliteratorWrapper<>(result, rows, "split-" + (counter++));
        }
        rows.add(row);
        return result;
    }

    @Override
    public long estimateSize() {
        final Row row = createRow("estimateSize");
        long result = spliterator.estimateSize();
        row.info = ": " + result;
        rows.add(row);
        return result;
    }

    @Override
    public int characteristics() {
        final Row row = createRow("characteristics");
        final int result = spliterator.characteristics();
        row.info = ": " + getCharacteristics(result);
        rows.add(row);
        return result;
    }

    private static String getCharacteristics(final int result) {
        return NAMES.entrySet().stream()
                .filter(e -> (result & ((int)e.getKey())) != 0)
                .map(Map.Entry::getValue)
                .collect(Collectors.joining(" | "));
    }
    
    
    //  List
    //  [main][main] characteristics: ORDERED | SIZED | SUBSIZED
    //  [main][main] characteristics: ORDERED | SIZED | SUBSIZED
    //  [main][main] estimateSize: 4
    //  count: 4
    //
    //  Set
    //  [main][main] characteristics: SIZED | DISTINCT
    //  [main][main] characteristics: SIZED | DISTINCT
    //  [main][main] estimateSize: 4
    //  count: 4
    //
    //  Arrays.spliterator
    //  [main][main] characteristics: ORDERED | SIZED | SUBSIZED | IMMUTABLE
    //  [main][main] characteristics: ORDERED | SIZED | SUBSIZED | IMMUTABLE
    //  [main][main] estimateSize: 4
    //  count: 4
    //
    //  Stream.of
    //  [main][main] characteristics: ORDERED | SIZED | SUBSIZED | IMMUTABLE
    //  [main][main] characteristics: ORDERED | SIZED | SUBSIZED | IMMUTABLE
    //  [main][main] estimateSize: 4
    //  count: 4
    //
    //  Stream.of + distinct
    //  [main][main] characteristics: ORDERED | DISTINCT
    //  [main][main] estimateSize: 4
    //  [main][main] trySplit: null
    //  [main][main] characteristics: ORDERED | DISTINCT
    //  [main][main] tryAdvance: true
    //  [main][main] tryAdvance: true
    //  [main][main] tryAdvance: true
    //  [main][main] tryAdvance: true
    //  [main][main] tryAdvance: false
    //  count: 4
    //
    //  Stream.of + distinct + map
    //  [main][main] characteristics: ORDERED
    //  [main][main] estimateSize: 4
    //  [main][main] trySplit: null
    //  [main][main] characteristics: ORDERED
    //  [main][main] tryAdvance: true
    //  [main][main] tryAdvance: true
    //  [main][main] tryAdvance: true
    //  [main][main] tryAdvance: true
    //  [main][main] tryAdvance: false
    //  count: 4
}
```

```java
public class Main {

    public static void main(String[] args) {

        long count = StreamSupport.stream(
                Arrays.asList(0, 1, 2, 3).spliterator(), true)
                .count();
        
        // 4

        System.out.println(count);
    }
}
```

![Spliterator](/list-spliterator.png)

На рисунке показан один из возможных вариантов работы сплитератора. `characteristics` везде возвращает `ORDERED | SIZED | SUBSIZED`, так как в `List` порядок имеет значение, количество элементов и всех разбитых кусков также известно. `trySplit` делит последовательность пополам, но не обязательно каждая часть будет отправлена новому потоку. В параллельном стриме новый поток может и не создаться, т.к. всё успевает обработаться в главном потоке. В данном же случае, новый поток успевал обработать части до того, как это делал главный поток.

```java
public class Main {

    public static void main(String[] args) {

        Spliterator<Integer> s = IntStream.range(0, 4)
                .boxed()
                .collect(Collectors.toSet())
                .spliterator();
        long count = StreamSupport.stream(s, true).count();

        System.out.println(count);
        
        // 4
    }
}
```

Здесь у сплитератора характеристикой будет `SIZED | DISTINCT`, а вот у каждой части характеристика `SIZED` теряется, остаётся только `DISTINCT`, потому что нельзя поделить множество так, чтобы размер каждой части был известен.</br>

В случае с `Set` было три вызова `trySplit`, первый якобы делил элементы поровну, после двух других каждая из частей возврашала `estimateSize`: **1**, однако во всех, кроме одной попытка вызвать `tryAdvance` не увенчалась успехом — возвращался `false`. А вот на одном из частей, который для `estimateSize` также возвращал **1**, было **4** успешных вызова `tryAdvance`. Это и подтверждает тот факт, что `estimateSize` не обязательно должен возвращать действительное число элементов.

```
Arrays.spliterator(new int[] {0, 1, 2, 3});
Stream.of(0, 1, 2, 3).spliterator();
```

Ситуация аналогична работе `List`, только характеристики возвращали `ORDERED | SIZED | SUBSIZED | IMMUTABLE`.</br></br>

```
Stream.of(0, 1, 2, 3).distinct().spliterator();
```

Здесь `trySplit` возвращал `null`, а значит поделить последовательно не представлялось возможным. Иерархия вызовов:

```
[main] characteristics: ORDERED | DISTINCT
[main] estimateSize: 4
[main] trySplit: null
[main] characteristics: ORDERED | DISTINCT
[main] tryAdvance: true
[main] tryAdvance: true
[main] tryAdvance: true
[main] tryAdvance: true
[main] tryAdvance: false
count: 4
```

```
Stream.of(0, 1, 2, 3)
    .distinct()
    .map(x -> x + 1)
    .spliterator();
```

Всё, как и выше, только теперь после применения оператора map, флаг `DISTINCT` исчез.

**9.3. Реализация сплитератора**

Для правильной реализации сплитератора нужно продумать, как сделать разбиение и обозначить характеристики стрима. Давайте напишем сплитератор, генерирующий последовательность чисел **Фибоначчи**.

Для упрощения задачи нам будет известно максимальное количество элементов для генерирования. А значит мы можем разделять последовательность пополам, а потом быстро просчитывать нужные числа по новому индексу.

Осталось определиться с характеристиками. Мы уже решили, что размер последовательности нам будет известен, а значит будет известен и размер каждой разбитой части. Порядок будет важен, так что без флага `ORDERED` не обойтись. Последовательность Фибоначчи также отсортирована — каждый последующий элемент всегда не меньше предыдущего.

А вот с флагом `DISTINCT`, кажется, промах. **0 1 1 2 3**, две единицы повторяются, а значит не видать нам этого флага?

На самом деле ничто нам не мешает просчитывать флаги автоматически. Если часть последовательности не будет затрагивать начальные индексы, то этот флаг можно выставить.

```
int distinct = (index >= 2) ? DISTINCT : 0;
return ORDERED | distinct | SIZED | SUBSIZED | IMMUTABLE | NONNULL;
```

Полная реализация класса:

```java
public class FibonacciSpliterator implements Spliterator<BigInteger> {

    private final int fence;
    private int index;
    private BigInteger a, b;

    public FibonacciSpliterator(int fence) {
        this(0, fence);
    }

    protected FibonacciSpliterator(int start, int fence) {
        this.index = start;
        this.fence = fence;
        recalculateNumbers(start);
    }

    private void recalculateNumbers(int start) {
        a = fastFibonacciDoubling(start);
        b = fastFibonacciDoubling(start + 1);
    }

    @Override
    public boolean tryAdvance(Consumer<? super BigInteger> action) {
        if (index >= fence) {
            return false;
        }
        action.accept(a);
        BigInteger c = a.add(b);
        a = b;
        b = c;
        index++;
        return true;
    }

    @Override
    public FibonacciSpliterator trySplit() {
        int lo = index;
        int mid = (lo + fence) >>> 1;
        if (lo >= mid) {
            return null;
        }
        index = mid;
        recalculateNumbers(mid);
        return new FibonacciSpliterator(lo, mid);
    }

    @Override
    public long estimateSize() {
        return fence - index;
    }

    @Override
    public int characteristics() {
        int distinct = (index >= 2) ? DISTINCT : 0;
        return ORDERED | distinct | SIZED | SUBSIZED | IMMUTABLE | NONNULL;
    }

    /*
     * https://www.nayuki.io/page/fast-fibonacci-algorithms
     */
    public static BigInteger fastFibonacciDoubling(int n) {
        BigInteger a = BigInteger.ZERO;
        BigInteger b = BigInteger.ONE;
        for (int bit = Integer.highestOneBit(n); bit != 0; bit >>>= 1) {
            BigInteger d = a.multiply(b.shiftLeft(1).subtract(a));
            BigInteger e = a.multiply(a).add(b.multiply(b));
            a = d;
            b = e;
            if ((n & bit) != 0) {
                BigInteger c = a.add(b);
                a = b;
                b = c;
            }
        }
        return a;
    }
}
```

Вот как разбиваются теперь элементы параллельного стрима:

```
StreamSupport.stream(new FibonacciSpliterator(7), true)
    .count();
```

![FibonacciSpliterator](/fibonaccispliterator.png)

```
StreamSupport.stream(new FibonacciSpliterator(500), true)
    .count();
```

![FibonacciSpliterator500](/fibonaccispliterator500.png)

**10. Другие способы создания источников**

Стрим из сплитератора — это самый эффективный способ создания стрима, но кроме него есть и другие способы.

**10.1. Стрим из итератора**

Благодаря классу `Spliterators`, можно преобразовать любой итератор в сплитератор. Вот пример создания стрима из итератора, генерирующего бесконечную последовательность чисел Фибоначчи.

```java
public class FibonacciIterator implements Iterator<BigInteger> {

    private BigInteger a = BigInteger.ZERO;
    private BigInteger b = BigInteger.ONE;

    @Override
    public boolean hasNext() {
        return true;
    }

    @Override
    public BigInteger next() {
        BigInteger result = a;
        a = b;
        b = result.add(b);
        return result;
    }

    public static void main(String[] args) {
        
        StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(
                        new FibonacciIterator(),
                        Spliterator.ORDERED | Spliterator.SORTED),
                false /* is parallel*/)
                .limit(10)
                
                .forEach(System.out::println);
        
        // 0, 1, 1, 2, 3, 5, 8, 13, 21, 34

    }
}
```

**10.2. Stream.iterate + map**

Можно воспользоваться двумя операторами: `iterate` + `map`, чтобы создать всё тот же стрим из чисел Фибоначчи.

```java
public class Main {

    public static void main(String[] args) {

        Stream.iterate(
                new BigInteger[] { BigInteger.ZERO, BigInteger.ONE },
                t -> new BigInteger[] { t[1], t[0].add(t[1]) })
                .map(t -> t[0])
                .limit(10)
                .forEach(System.out::println);

        // 0, 1, 1, 2, 3, 5, 8, 13, 21, 34

    }
}
```

Для удобства можно обернуть всё в метод и вызывать</br>
`fibonacciStream().limit(10).forEach(...)`.

**10.3. IntStream.range + map**

Ещё один гибкий и удобный способ создать стрим. Если у вас есть какие-то данные, которые можно получить по индексу, то можно создать числовой промежуток при помощи оператора range, затем поэлементно с помощью него обращаться к данным через `map/mapToObj`.

```java
IntStream.range(0, 200)
    .mapToObj(i -> fibonacci(i))
    .forEach(System.out::println);
 
JSONArray arr = ...
IntStream.range(0, arr.length())
    .mapToObj(JSONArray::getJSONObject)
    .map(obj -> ...)
    .forEach(System.out::println);
```

<a name="examples">**11. Примеры**</a>

Прежде чем перейти к более приближенным к жизни примерам, стоит сказать, что если код уже написан без стримов и работает хорошо, не нужно сломя голову всё переписывать. Также бывает ситуации, когда красиво реализовать задачу с использованием `Stream API` не получается, в таком случае смиритесь и не тяните стримы за уши.

Дан массив аргументов. Нужно получить `Map`, где каждому ключу будет соответствовать своё значение.

```java
public class Main {

    public static void main(String[] args) {

        String[] arguments = {"-i", "in.txt", "--limit", "40", "-d", "1", "-o", "out.txt"};
        Map<String, String> argsMap = new LinkedHashMap<>(arguments.length / 2);
        for (int i = 0; i < arguments.length; i += 2) {
            argsMap.put(arguments[i], arguments[i + 1]);
        }
        argsMap.forEach((key, value) -> System.out.format("%s: %s%n", key, value));
        
        // -i: in.txt
        // --limit: 40
        // -d: 1
        // -o: out.txt


    }
}
```

Быстро и понятно. А вот для обратной задачи — сконвертировать Map с аргументами в массив строк, стримы помогут.

```java
public class Main {

    public static void main(String[] args) {

        String[] arguments = {"-i", "in.txt", "--limit", "40", "-d", "1", "-o", "out.txt"};
        Map<String, String> argsMap = new LinkedHashMap<>(arguments.length / 2);
        for (int i = 0; i < arguments.length; i += 2) {
            argsMap.put(arguments[i], arguments[i + 1]);
        }
        argsMap.forEach((key, value) -> System.out.format("%s: %s%n", key, value));

        // -i: in.txt
        // --limit: 40
        // -d: 1
        // -o: out.txt


        System.out.println();
        
        String[] args1 = argsMap.entrySet().stream()
                .flatMap(e -> Stream.of(e.getKey(), e.getValue()))
                .toArray(String[]::new);
        System.out.println(String.join(" ", args1));

        // -i in.txt --limit 40 -d 1 -o out.txt

    }
}
```

Дан список студентов.

```java
List<Student> students = Arrays.asList(
        new Student("Alex", Speciality.Physics, 1),
        new Student("Rika", Speciality.Biology, 4),
        new Student("Julia", Speciality.Biology, 2),
        new Student("Steve", Speciality.History, 4),
        new Student("Mike", Speciality.Finance, 1),
        new Student("Hinata", Speciality.Biology, 2),
        new Student("Richard", Speciality.History, 1),
        new Student("Kate", Speciality.Psychology, 2),
        new Student("Sergey", Speciality.ComputerScience, 4),
        new Student("Maximilian", Speciality.ComputerScience, 3),
        new Student("Tim", Speciality.ComputerScience, 5),
        new Student("Ann", Speciality.Psychology, 1)
        );
 
enum Speciality {
    Biology, ComputerScience, Economics, Finance,
    History, Philosophy, Physics, Psychology
}
```

У класса `Student` реализованы все геттеры и сеттеры, toString и equals+hashCode.

Нужно сгруппировать всех студентов по курсу.

```java
students.stream()
        .collect(Collectors.groupingBy(Student::getYear))
        .entrySet().forEach(System.out::println);
// 1=[Alex: Physics 1, Mike: Finance 1, Richard: History 1, Ann: Psychology 1]
// 2=[Julia: Biology 2, Hinata: Biology 2, Kate: Psychology 2]
// 3=[Maximilian: ComputerScience 3]
// 4=[Rika: Biology 4, Steve: History 4, Sergey: ComputerScience 4]
// 5=[Tim: ComputerScience 5]
```

Вывести в алфавитном порядке список специальностей, на которых учатся перечисленные в списке студенты.

```java
students.stream()
        .map(Student::getSpeciality)
        .distinct()
        .sorted(Comparator.comparing(Enum::name))
        .forEach(System.out::println);
// Biology
// ComputerScience
// Finance
// History
// Physics
// Psychology
```

Вывести количество учащихся на каждой из специальностей.

```java
students.stream()
        .collect(Collectors.groupingBy(
                Student::getSpeciality, Collectors.counting()))
        .forEach((s, count) -> System.out.println(s + ": " + count));
// Psychology: 2
// Physics: 1
// ComputerScience: 3
// Finance: 1
// Biology: 3
// History: 2
```

Сгруппировать студентов по специальностям, сохраняя алфавитный порядок специальности, а затем сгруппировать по курсу.

```java
Map<Speciality, Map<Integer, List<Student>>> result = students.stream()
        .sorted(Comparator
                .comparing(Student::getSpeciality, Comparator.comparing(Enum::name))
                .thenComparing(Student::getYear)
        )
        .collect(Collectors.groupingBy(
                Student::getSpeciality,
                LinkedHashMap::new,
                Collectors.groupingBy(Student::getYear)));
```

Теперь это всё красиво вывести.

```java
result.forEach((s, map) -> {
    System.out.println("-= " + s + " =-");
    map.forEach((year, list) -> System.out.format("%d: %s%n", year, list.stream()
            .map(Student::getName)
            .sorted()
            .collect(Collectors.joining(", ")))
    );
    System.out.println();
});
```
```
-= Biology =-
2: Hinata, Julia
4: Rika
```

```
-= ComputerScience =-
3: Maximilian
4: Sergey
5: Tim
```

```
-= Finance =-
1: Mike
```

```
-= History =-
1: Richard
4: Steve
```

```
-= Physics =-
1: Alex
```

```
-= Psychology =-
1: Ann
2: Kate
```


Проверить, есть ли третьекурсники среди учащихся всех специальностей кроме физики и CS.

```java
students.stream()
        .filter(s -> !EnumSet.of(Speciality.ComputerScience, Speciality.Physics)
                .contains(s.getSpeciality()))
        .anyMatch(s -> s.getYear() == 3);
// false
```

Вычислить число Пи методом **Монте-Карло**.

```java
final Random rnd = new Random();
final double r = 1000.0;
final int max = 10000000;
long count = IntStream.range(0, max)
        .mapToObj(i -> rnd.doubles(2).map(x -> x * r).toArray())
        .parallel()
        .filter(arr -> Math.hypot(arr[0], arr[1]) <= r)
        .count();
System.out.println(4.0 * count / max);
// 3.1415344
```

Вывести таблицу умножения.

```java
IntStream.rangeClosed(2, 9)
        .boxed()
        .flatMap(i -> IntStream.rangeClosed(2, 9)
                .mapToObj(j -> String.format("%d * %d = %d", i, j, i * j))
        )
        .forEach(System.out::println);
// 2 * 2 = 4
// 2 * 3 = 6
// 2 * 4 = 8
// 2 * 5 = 10
// ...
// 9 * 7 = 63
// 9 * 8 = 72
// 9 * 9 = 81
```

Или более экзотический вариант, в 4 столбца, как на школьных тетрадях.

```java
IntFunction<IntFunction<String>> function = i -> j -> String.format("%d x %2d = %2d", i, j, i * j);
IntFunction<IntFunction<IntFunction<String>>> repeaterX = count -> i -> j ->
        IntStream.range(0, count)
                .mapToObj(delta -> function.apply(i + delta).apply(j))
                .collect(Collectors.joining("\t"));
IntFunction<IntFunction<IntFunction<IntFunction<String>>>> repeaterY = countY -> countX -> i -> j ->
        IntStream.range(0, countY)
                .mapToObj(deltaY -> repeaterX.apply(countX).apply(i).apply(j + deltaY))
                .collect(Collectors.joining("\n"));
IntFunction<String> row = i -> repeaterY.apply(10).apply(4).apply(i).apply(1) + "\n";
IntStream.of(2, 6).mapToObj(row).forEach(System.out::println);
```

![multiplicationtable](/multiplicationtable.png)

Но это, конечно же, шутка. Писать такой код вас никто не заставляет.

**12. Задачи**

Там где **XXX** нужно заменить на нужное значение

```java
public class Main {

    public static void main(String[] args) {

        IntStream.concat(
                IntStream.range(2, XXX),
                IntStream.rangeClosed(XXX, XXX))
                .forEach(System.out::println);

        // 2, 3, 4, 5, -1, 0, 1, 2

        System.out.println();


        IntStream.range(5, 30)
                .limit(12)
                .skip(3)
                .limit(6)
                .skip(2)
                .forEach(System.out::println);

        // XXX, XXX, XXX, XXX

        System.out.println();


        IntStream.range(0, 10)
                .skip(2)
                .dropWhile(x -> x < XXX)
                .limit(XXX)
                .forEach(System.out::println);

        // 5, 6, 7

        System.out.println();


        IntStream.range(0, 10)
                .skip(XXX)
                .takeWhile(x -> x < XXX)
                .limit(3)
                .forEach(System.out::println);

        // 3, 4

        System.out.println();


        IntStream.range(1, 5)
                .flatMap(i -> IntStream.generate(() ->  XXX).XXX(XXX))
        .forEach(System.out::println);

        // 1, 2, 2, 3, 3, 3, 4, 4, 4, 4

        System.out.println();


        int x = IntStream.range(-2, 2)
                .map(i -> i * XXX )
                .reduce(10, Integer::sum);

        // x: 0

        System.out.println();


        IntStream.range(0, 10)
                .boxed()
                .collect(Collectors.XXX(i -> XXX ))
        .entrySet().forEach(System.out::println);

        // false=[1, 3, 5, 7, 9]
        // true=[0, 2, 4, 6, 8]

        System.out.println();


        IntStream.range(-5, 0)
                .flatMap(i -> IntStream.of(i,XXX))
                .XXX()
                .forEach(System.out::println);

        // -5, -4, -3, -2, -1, 1, 2, 3, 4, 5

        System.out.println();


        IntStream.range(-5, 0)
                .flatMap(i -> IntStream.of(i,XXX))
                .XXX()
                .sorted(Comparator.comparing(Math::XXX))
                .forEach(System.out::println);

        // -1, 1, -2, 2, -3, 3, -4, 4, -5, 5

        System.out.println();


        IntStream.range(1, 5)
                .flatMap(i -> IntStream.generate(() -> i).limit(i))
                .boxed()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.XXX()))
        .entrySet().forEach(System.out::println);

        // 1=1
        // 2=2
        // 3=3
        // 4=4

    }

}
```

**Ответ**

```java
public class Main {

    public static void main(String[] args) {

        IntStream.concat(
                IntStream.range(2, 6),
                IntStream.rangeClosed(-1, 2))
                .forEach(System.out::println);

        // 2, 3, 4, 5, -1, 0, 1, 2

        System.out.println();


        IntStream.range(5, 30)
                .limit(12)
                .skip(3)
                .limit(6)
                .skip(2)
                .forEach(System.out::println);

        // 10, 11, 12, 13

        System.out.println();


        IntStream.range(0, 10)
                .skip(2)
                .dropWhile(x -> x < 5)
                .limit(3)
                .forEach(System.out::println);

        // 5, 6, 7

        System.out.println();


        IntStream.range(0, 10)
                .skip(3)
                .takeWhile(x -> x < 5)
                .limit(3)
                .forEach(System.out::println);

        // 3, 4

        System.out.println();


        IntStream.range(1, 5)
                .flatMap(i -> IntStream.generate(() ->  i).limit(i))
        .forEach(System.out::println);

        // 1, 2, 2, 3, 3, 3, 4, 4, 4, 4

        System.out.println();


        int x = IntStream.range(-2, 2)
                .map(i -> i * 5 )
                .reduce(10, Integer::sum);

        // x: 0

        System.out.println();


        IntStream.range(0, 10)
                .boxed()
                .collect(Collectors.partitioningBy(i -> i % 2 == 0 ))
        .entrySet().forEach(System.out::println);

        // false=[1, 3, 5, 7, 9]
        // true=[0, 2, 4, 6, 8]

        System.out.println();


        IntStream.range(-5, 0)
                .flatMap(i -> IntStream.of(i,-i))
                .sorted()
                .forEach(System.out::println);

        // -5, -4, -3, -2, -1, 1, 2, 3, 4, 5

        System.out.println();


        IntStream.range(-5, 0)
                .flatMap(i -> IntStream.of(i,-i))
                .boxed()
                .sorted(Comparator.comparing(Math::abs))
                .forEach(System.out::println);

        // -1, 1, -2, 2, -3, 3, -4, 4, -5, 5

        System.out.println();


        IntStream.range(1, 5)
                .flatMap(i -> IntStream.generate(() -> i).limit(i))
                .boxed()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
        .entrySet().forEach(System.out::println);

        // 1=1
        // 2=2
        // 3=3
        // 4=4

    }

}
```

**13. Советы и best practices**

1. Если задачу не получается красиво решить стримами, не решайте её стримами.

2. Если задачу не получается красиво решить стримами, не решайте её стримами!

3. Если задача уже красиво решена не стримами, всё работает и всех всё устраивает, не перерешивайте её стримами!

4. В большинстве случаев нет смысла сохранять стрим в переменную. Используйте цепочку вызовов методов (`method chaining`).

```
// Нечитабельно

Stream<Integer> stream = list.stream();
stream = stream.filter(x -> x > 2);
stream.forEach(System.out::println);

// Так лучше

list.stream()
        .filter(x -> x > 2)
        .forEach(System.out::println);
```

5. Старайтесь сперва отфильтровать стрим от ненужных элементов или ограничить его, а потом выполнять преобразования.

```
// Лишние затраты

list.stream()
        .sorted()
        .filter(x -> x > 0)
        .forEach(System.out::println);

// Так лучше

list.stream()
        .filter(x -> x > 0)
        .sorted()
        .forEach(System.out::println);
```

6. Не используйте параллельные стримы везде, где только можно. Затраты на разбиение элементов, обработку в другом потоке и последующее их слияние порой больше, чем выполнение в одном потоке. Читайте об этом здесь — [When to use parallel streams](http://gee.cs.oswego.edu/dl/html/StreamParallelGuidance.html).

7. При использовании параллельных стримов, убедитесь, что нигде нет блокирующих операций или чего-то, что может помешать обработке элементов.

8. Если где-то в модели вы возвращаете копию списка или другой коллекции, то подумайте о замене на стримы. Например:

```java
// Было
class Model {
 
    private final List<String> data;
 
    public List<String> getData() {
        return new ArrayList<>(data);
    }
}
 
// Стало
class Model {
 
    private final List<String> data;
 
    public Stream<String> dataStream() {
        return data.stream();
    }
}
```

Теперь есть возможность получить не только список `model.dataStream().collect(toList());`, но и множество, любую другую коллекцию, отфильтровать что-то, отсортировать и так далее. Оригинальный `List<String>` data так и останется нетронутым.

----

Если возникнут какие-либо вопросы, смело задавайте их в комментариях.
