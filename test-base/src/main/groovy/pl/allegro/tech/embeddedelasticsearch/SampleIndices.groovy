package pl.allegro.tech.embeddedelasticsearch

import groovy.transform.Immutable

import static java.lang.ClassLoader.getSystemResourceAsStream

class SampleIndices {

    static final BOOK_ALIAS_1 = "book_alias_1"
    static final BOOK_ALIAS_2 = "book_alias_2"
    static final CARS_INDEX_NAME = "cars"
    static final CARS_MAPPING_NAME = "car-mapping.json"
    static final CAR_INDEX_TYPE = "car"
    static final CARS_TEMPLATE_NAME = "cars_template"
    static final CARS_TEMPLATE = resourceToString("cars-template.json")
    static final CARS_TEMPLATE_6x = getSystemResourceAsStream("cars-template-6x.json")
    static final CARS_TEMPLATE_7x = getSystemResourceAsStream("cars-template-7x.json")
    static final BOOKS_INDEX_NAME = "books"
    static final PAPER_BOOK_INDEX_TYPE = "paper_book"
    static final AUDIO_BOOK_INDEX_TYPE = "audio_book"
    static final BOOKS_INDEX_MULTI_TYPE = IndexSettings.builder()
            .withType(PAPER_BOOK_INDEX_TYPE, getSystemResourceAsStream("paper-book-mapping.json"))
            .withType("audio_book", getSystemResourceAsStream("audio-book-mapping.json"))
            .withSettings(getSystemResourceAsStream("elastic-settings.json"))
            .withAliases(getSystemResourceAsStream("elastic-aliases.json"))
            .build()

    static final BOOKS_INDEX = IndexSettings.builder()
            .withType(PAPER_BOOK_INDEX_TYPE, getSystemResourceAsStream("paper-book-mapping.json"))
            .withSettings(getSystemResourceAsStream("elastic-settings.json"))
            .withAliases(getSystemResourceAsStream("elastic-aliases.json"))
            .build()

    static final CARS_INDEX_7x = IndexSettings.builder()
            .withMapping(getSystemResourceAsStream("car-mapping.json"))
            .withSettings(getSystemResourceAsStream("elastic-settings.json"))
            .withAliases(getSystemResourceAsStream("elastic-aliases.json"))
            .build()

    static String toJson(Car car) {
        """
        {
            "manufacturer": "$car.manufacturer",
            "model": "$car.model",
            "description": "$car.description"
        }
        """
    }
    static final Car FIAT_126p = new Car(manufacturer: "Fiat", model: "126p", description: "very good car")

    static String toJson(PaperBook book) {
        """
        {
            "author": "$book.author",
            "title": "$book.title",
            "description": "$book.description"
        }
        """
    }
    static PaperBook SHINING = new PaperBook(author: "Stephen King", title: "Shining", description: "book about overlook hotel")
    static PaperBook CUJO = new PaperBook(author: "Stephen King", title: "Cujo", description: "book about mad dog")
    static PaperBook MISERY = new PaperBook(author: "Stephen King", title: "Misery", description: "book about insane woman that imprisoned favourite writer")

    static String toJson(AudioBook book) {
        """
        {
            "author": "$book.author",
            "title": "$book.title",
            "readBy": "$book.readBy",
            "description": "$book.description"
        }
        """
    }
    static AudioBook AMERICAN_PSYCHO = new AudioBook(author: "Bret Easton Ellis", title: "American Psycho", readBy: "Clint Eastwood", description: "book about psychopath")

    @Immutable
    static class Car {
        String manufacturer
        String model
        String description
    }

    @Immutable
    static class PaperBook {
        String author
        String title
        String description
    }

    @Immutable
    static class AudioBook {
        String author
        String title
        String readBy
        String description
    }

    static String resourceToString(String resourceName) {
        InputStream inputStream = getSystemResourceAsStream(resourceName)
        ByteArrayOutputStream result = new ByteArrayOutputStream()
        byte[] buffer = new byte[1024]
        int length
        while ((length = inputStream.read(buffer)) != -1) {
            result.write(buffer, 0, length)
        }
        return result.toString("UTF-8")
    }
}