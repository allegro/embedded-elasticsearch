package pl.allegro.tech.embeddedelasticsearch;

class Require {
    static void require(boolean condition, String message) {
        if (!condition) {
            throw new InvalidSetupException(message);
        }
    }
}
